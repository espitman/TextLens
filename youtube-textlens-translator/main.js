const { app, BrowserWindow, ipcMain, dialog } = require('electron');
const path = require('path');
const fs = require('fs');
const { exec } = require('child_process');
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

  // 2. Extract YouTube Subtitles
  ipcMain.handle('extract-youtube-subtitles', async (event, { url, lang = 'en' }) => {
    const videoID = extractVideoId(url);
    if (!videoID) {
      throw new Error('Invalid YouTube URL or Video ID');
    }
    
    try {
      const [subtitles, details] = await Promise.all([
        getSubtitles({ videoID, lang }),
        getVideoDetails({ videoID, lang }).catch(() => ({ title: videoID }))
      ]);
      
      if (!subtitles || !subtitles.length) {
        throw new Error(`No English subtitles found for YouTube video ${videoID}.`);
      }
      
      const srtContent = subtitlesToSrt(subtitles);
      return {
        title: details.title || videoID,
        content: srtContent
      };
    } catch (error) {
      throw new Error(error.message || 'Failed to extract YouTube captions.');
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
