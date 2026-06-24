const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('electronAPI', {
  selectSrtFile: () => ipcRenderer.invoke('select-srt-file'),
  saveSrtFile: (data) => ipcRenderer.invoke('save-srt-file', data),
  getSystemKeys: () => ipcRenderer.invoke('get-system-keys'),
  extractYoutubeSubtitles: (data) => ipcRenderer.invoke('extract-youtube-subtitles', data)
});
