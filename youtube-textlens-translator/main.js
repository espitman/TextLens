const { app, BrowserWindow, ipcMain, dialog, session } = require('electron');
const path = require('path');
const fs = require('fs');
const { exec } = require('child_process');

const SUBTITLE_TO_PARTITION = 'persist:textlens-subtitleto';

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
      sandbox: false,
      webviewTag: true
    }
  });

  win.removeMenu();
  win.loadFile('index.html');
}

function setupSubtitleToDownloadBridge() {
  const subtitleToSession = session.fromPartition(SUBTITLE_TO_PARTITION);

  subtitleToSession.on('will-download', (event, item) => {
    const fallbackName = `subtitleto-${Date.now()}.srt`;
    const fileName = path.basename(item.getFilename() || fallbackName) || fallbackName;
    const savePath = path.join(app.getPath('temp'), fileName);

    item.setSavePath(savePath);

    item.once('done', (_event, state) => {
      const windows = BrowserWindow.getAllWindows();
      if (state !== 'completed') {
        windows.forEach((win) => {
          win.webContents.send('subtitleto-download', {
            ok: false,
            error: `Download ${state}.`
          });
        });
        return;
      }

      try {
        const content = fs.readFileSync(savePath, 'utf-8');
        windows.forEach((win) => {
          win.webContents.send('subtitleto-download', {
            ok: true,
            name: fileName,
            path: savePath,
            content
          });
        });
      } catch (error) {
        windows.forEach((win) => {
          win.webContents.send('subtitleto-download', {
            ok: false,
            error: error.message || 'Could not read downloaded SRT.'
          });
        });
      }
    });
  });
}

app.whenReady().then(() => {
  setupSubtitleToDownloadBridge();

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

  // 2. Open File Dialog
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

  // 3. Save File Dialog
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
