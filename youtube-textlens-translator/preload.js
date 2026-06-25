const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('electronAPI', {
  selectSrtFile: () => ipcRenderer.invoke('select-srt-file'),
  saveSrtFile: (data) => ipcRenderer.invoke('save-srt-file', data),
  getSystemKeys: () => ipcRenderer.invoke('get-system-keys'),
  onSubtitleToDownload: (callback) => {
    const listener = (_event, payload) => callback(payload);
    ipcRenderer.on('subtitleto-download', listener);
    return () => ipcRenderer.removeListener('subtitleto-download', listener);
  }
});
