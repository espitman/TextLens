const { app, BrowserWindow, ipcMain, dialog } = require('electron');
const path = require('path');
const fs = require('fs');
const { exec, spawn } = require('child_process');
const { getSubtitles, getVideoDetails } = require('youtube-caption-extractor');

// Helper function to extract YouTube video ID from URL
function extractVideoId(input) {
  if (!input) return '';
  if (/^[a-zA-Z0-9_-]{11}$/.test(input)) return input;
  try {
    const url = new URL(input);
    if (url.hostname.includes('youtu.be')) {
      return url.pathname.replace('/', '').slice(0, 11);
    }
    return url.searchParams.get('v') || '';
  } catch {
    return '';
  }
}

// Helper to format seconds to SRT timestamp format
function formatTimestamp(seconds) {
  const safeSeconds = Math.max(0, Number.isFinite(seconds) ? seconds : 0);
  const totalMillis = Math.round(safeSeconds * 1000);
  const millis = totalMillis % 1000;
  const totalSeconds = Math.floor(totalMillis / 1000);
  const secs = totalSeconds % 60;
  const totalMinutes = Math.floor(totalSeconds / 60);
  const minutes = totalMinutes % 60;
  const hours = Math.floor(totalMinutes / 60);

  return [
    String(hours).padStart(2, '0'),
    String(minutes).padStart(2, '0'),
    String(secs).padStart(2, '0')
  ].join(':') + `,${String(millis).padStart(3, '0')}`;
}

// Helper to convert subtitles JSON structure to SRT string
function subtitlesToSrt(subtitles) {
  return subtitles
    .map((subtitle, index) => {
      const start = Number(subtitle.start);
      const duration = Number(subtitle.dur);
      const end = start + (Number.isFinite(duration) && duration > 0 ? duration : 1.5);
      const text = String(subtitle.text || '')
        .replace(/\r\n/g, '\n')
        .replace(/\r/g, '\n')
        .trim();

      return [
        String(index + 1),
        `${formatTimestamp(start)} --> ${formatTimestamp(end)}`,
        text
      ].join('\n');
    })
    .join('\n\n') + '\n';
}

// Promisified command runner
function runCommand(command, args) {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, { stdio: ['ignore', 'pipe', 'pipe'] });
    let stdout = '';
    let stderr = '';
    child.stdout.on('data', (chunk) => { stdout += chunk.toString(); });
    child.stderr.on('data', (chunk) => { stderr += chunk.toString(); });
    child.on('error', reject);
    child.on('close', (code) => {
      if (code === 0) {
        resolve({ stdout, stderr });
      } else {
        const err = new Error(stderr.trim() || `${command} exited with ${code}`);
        err.code = code;
        reject(err);
      }
    });
  });
}

// Check if file path exists
async function pathExists(filePath) {
  try {
    await fs.promises.access(filePath);
    return true;
  } catch {
    return false;
  }
}

// Main function to extract subtitles using yt-dlp
async function extractWithYtDlp(url, lang = 'en', useCookies = false) {
  const videoID = extractVideoId(url);
  const tempDir = app.getPath('temp');
  const outputBase = path.join(tempDir, `yt-sub-${Date.now()}-${videoID}`);
  const expectedLanguageFile = `${outputBase}.${lang}.srt`;
  const fallbackFile = `${outputBase}.srt`;
  
  const ytDlpArgs = [
    '--no-update',
    '--ignore-no-formats',
    '--skip-download',
    '--write-subs',
    '--write-auto-subs',
    '--sub-langs',
    lang,
    '--sub-format',
    'srt/best',
    '--convert-subs',
    'srt',
    '-o',
    outputBase,
    url
  ];
  
  if (useCookies) {
    ytDlpArgs.push('--cookies-from-browser', 'chrome');
  }
  
  // Use Brew-installed path, or fallback to system path
  const ytDlpPath = '/opt/homebrew/bin/yt-dlp';
  
  await runCommand(ytDlpPath, ytDlpArgs);
  
  const generatedPath = await pathExists(expectedLanguageFile)
    ? expectedLanguageFile
    : await pathExists(fallbackFile)
      ? fallbackFile
      : '';
      
  if (!generatedPath) {
    throw new Error('yt-dlp did not produce an SRT file.');
  }
  
  const content = fs.readFileSync(generatedPath, 'utf-8');
  
  // Clean up
  try {
    fs.unlinkSync(generatedPath);
  } catch (e) {}
  
  return content;
}

function createWindow() {
  const win = new BrowserWindow({
    width: 950,
    height: 720,
    minWidth: 750,
    minHeight: 550,
    backgroundColor: '#070707',
    title: 'TextLens Subtitle Translator',
    titleBarStyle: 'hidden',
    trafficLightPosition: { x: 18, y: 22 },
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true
    }
  });

  win.removeMenu();
  win.loadFile('index.html');
}

app.whenReady().then(() => {
  // IPC Handlers
  
  // 1. Fetch system API keys from macOS Defaults com.textlens.app
  ipcMain.handle('get-system-keys', async () => {
    return new Promise((resolve) => {
      exec('defaults read com.textlens.app', (error, stdout, stderr) => {
        if (error) {
          resolve({ openRouter: '', liara: '' });
          return;
        }
        
        const openRouterMatch = stdout.match(/"translation\.openRouter\.apiKey"\s*=\s*"([^"]+)"/);
        const liaraMatch = stdout.match(/"translation\.liara\.apiKey"\s*=\s*"([^"]+)"/);
        
        resolve({
          openRouter: openRouterMatch ? openRouterMatch[1] : '',
          liara: liaraMatch ? liaraMatch[1] : ''
        });
      });
    });
  });

  // 2. Extract YouTube Subtitles (with fallback mechanisms)
  ipcMain.handle('extract-youtube-subtitles', async (event, { url, lang = 'en' }) => {
    const videoID = extractVideoId(url);
    if (!videoID) {
      throw new Error('Invalid YouTube URL or Video ID');
    }
    
    // Retrieve title if possible
    let title = videoID;
    try {
      const details = await getVideoDetails({ videoID, lang }).catch(() => ({ title: videoID }));
      if (details && details.title) {
        title = details.title;
      }
    } catch (e) {}

    // Method 1: Try yt-dlp without cookies
    try {
      const srtContent = await extractWithYtDlp(url, lang, false);
      return { title, content: srtContent };
    } catch (e1) {
      console.warn('yt-dlp without cookies failed, trying with Chrome cookies...', e1.message);
      
      // Method 2: Try yt-dlp with Chrome cookies to bypass bot check
      try {
        const srtContent = await extractWithYtDlp(url, lang, true);
        return { title, content: srtContent };
      } catch (e2) {
        console.warn('yt-dlp with Chrome cookies failed, falling back to youtube-caption-extractor...', e2.message);
        
        // Method 3: Fallback to fetch-based youtube-caption-extractor
        try {
          const subtitles = await getSubtitles({ videoID, lang });
          if (!subtitles || !subtitles.length) {
            throw new Error(`No subtitles found with lang=${lang}.`);
          }
          const srtContent = subtitlesToSrt(subtitles);
          return { title, content: srtContent };
        } catch (e3) {
          throw new Error(`Failed to extract subtitles.\nMethod 1 (yt-dlp): ${e1.message}\nMethod 2 (yt-dlp with cookies): ${e2.message}\nMethod 3 (API scraper): ${e3.message}`);
        }
      }
    }
  });

  // 3. Open File Dialog
  ipcMain.handle('select-srt-file', async () => {
    const result = await dialog.showOpenDialog({
      properties: ['openFile'],
      filters: [{ name: 'Subtitle Files', extensions: ['srt'] }]
    });
    if (result.canceled || result.filePaths.length === 0) {
      return null;
    }
    const filePath = result.filePaths[0];
    const content = fs.readFileSync(filePath, 'utf-8');
    return {
      path: filePath,
      name: path.basename(filePath),
      content: content
    };
  });

  // 4. Save File Dialog
  ipcMain.handle('save-srt-file', async (event, { defaultName, content }) => {
    const result = await dialog.showSaveDialog({
      defaultPath: defaultName,
      filters: [{ name: 'Subtitle Files', extensions: ['srt'] }]
    });
    if (result.canceled || !result.filePath) {
      return { success: false };
    }
    fs.writeFileSync(result.filePath, content, 'utf-8');
    return { success: true, path: result.filePath };
  });

  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});
