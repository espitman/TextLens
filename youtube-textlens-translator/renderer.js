// Renderer Process

// UI Elements
const apiProviderSelect = document.getElementById('api-provider');
const apiKeyInput = document.getElementById('api-key');
const toggleKeyVisibility = document.getElementById('toggle-key-visibility');
const keyLabel = document.getElementById('key-label');
const modelSelect = document.getElementById('model-select');
const concurrencySlider = document.getElementById('concurrency-slider');
const concurrencyVal = document.getElementById('concurrency-val');

const tabYt = document.getElementById('tab-yt');
const tabFile = document.getElementById('tab-file');
const viewYt = document.getElementById('view-yt');
const viewUpload = document.getElementById('view-upload');
const viewTranslate = document.getElementById('view-translate');
const dropZone = document.getElementById('drop-zone');
const browseBtn = document.getElementById('browse-btn');

const ytUrlInput = document.getElementById('yt-url');
const ytFetchBtn = document.getElementById('yt-fetch-btn');
const ytLangSelect = document.getElementById('yt-lang');

const loadedFileName = document.getElementById('loaded-file-name');
const loadedFileInfo = document.getElementById('loaded-file-info');
const startBtn = document.getElementById('start-btn');
const resetBtn = document.getElementById('reset-btn');

const progressContainer = document.getElementById('progress-container');
const statusBadge = document.getElementById('status-badge');
const progressPct = document.getElementById('progress-pct');
const progressBarFill = document.getElementById('progress-bar-fill');
const logOutput = document.getElementById('log-output');
const previewOutput = document.getElementById('preview-output');

const successBox = document.getElementById('success-box');
const saveBtn = document.getElementById('save-btn');
const doneBtn = document.getElementById('done-btn');

// State
let loadedFile = null; // { name, path, content }
let parsedBlocks = [];
let translationProgress = {}; // index -> translated text
let isTranslating = false;
let shouldCancel = false;

// API Models configuration with pricing (per 1M tokens in USD)
const modelsMap = {
  openrouter: [
    { value: 'google/gemma-4-31b-it:free', name: 'Gemma 4 31B (Free)', inputPrice: 0, outputPrice: 0 },
    { value: 'openai/gpt-4.1-mini', name: 'GPT 4.1 Mini', inputPrice: 0.15, outputPrice: 0.60 },
    { value: 'google/gemini-2.0-flash-lite-001', name: 'Gemini 2.0 Flash Lite', inputPrice: 0.075, outputPrice: 0.30 },
    { value: 'anthropic/claude-3.5-sonnet', name: 'Claude 3.5 Sonnet', inputPrice: 3.00, outputPrice: 15.00 }
  ],
  liara: [
    { value: 'openai/gpt-5-nano', name: 'GPT 5 Nano (Default)', inputPrice: 0.075, outputPrice: 0.30 },
    { value: 'openai/gpt-4.1-mini', name: 'GPT 4.1 Mini', inputPrice: 0.15, outputPrice: 0.60 },
    { value: 'google/gemma-3-27b-it', name: 'Gemma 3 27B', inputPrice: 0.10, outputPrice: 0.40 },
    { value: 'google/gemini-2.0-flash-lite-001', name: 'Gemini 2.0 Flash Lite', inputPrice: 0.075, outputPrice: 0.30 }
  ]
};

// Initialize Settings
async function initSettings() {
  // Load saved API Provider
  const savedProvider = localStorage.getItem('api_provider') || 'openrouter';
  apiProviderSelect.value = savedProvider;
  
  // Try to load saved API Key, or fetch from system defaults if not set in local storage
  let savedKey = localStorage.getItem(`api_key_${savedProvider}`);
  
  if (savedKey === null) {
    try {
      const systemKeys = await window.electronAPI.getSystemKeys();
      if (systemKeys.openRouter) {
        localStorage.setItem('api_key_openrouter', systemKeys.openRouter);
      }
      if (systemKeys.liara) {
        localStorage.setItem('api_key_liara', systemKeys.liara);
      }
      savedKey = savedProvider === 'liara' ? systemKeys.liara : systemKeys.openRouter;
    } catch (e) {
      savedKey = '';
    }
  }
  
  apiKeyInput.value = savedKey || '';
  
  // Set correct label
  updateKeyLabel(savedProvider);
  
  // Populate models
  populateModels(savedProvider);
  const savedModel = localStorage.getItem(`model_${savedProvider}`);
  if (savedModel && Array.from(modelSelect.options).some(o => o.value === savedModel)) {
    modelSelect.value = savedModel;
  }

  // Load concurrency
  const savedConcurrency = localStorage.getItem('concurrency') || '4';
  concurrencySlider.value = savedConcurrency;
  concurrencyVal.textContent = savedConcurrency;
}

function updateKeyLabel(provider) {
  if (provider === 'liara') {
    keyLabel.textContent = 'Liara API Key';
    apiKeyInput.placeholder = 'eyJhbGciOi...';
  } else {
    keyLabel.textContent = 'OpenRouter API Key';
    apiKeyInput.placeholder = 'sk-or-...';
  }
}

function populateModels(provider) {
  modelSelect.innerHTML = '';
  const models = modelsMap[provider] || [];
  models.forEach(m => {
    const opt = document.createElement('option');
    opt.value = m.value;
    opt.textContent = m.name;
    modelSelect.appendChild(opt);
  });
}

// Event Listeners for Settings
apiProviderSelect.addEventListener('change', (e) => {
  const provider = e.target.value;
  localStorage.setItem('api_provider', provider);
  
  // Load key for this provider
  const savedKey = localStorage.getItem(`api_key_${provider}`) || '';
  apiKeyInput.value = savedKey;
  
  updateKeyLabel(provider);
  populateModels(provider);
  
  // Save default selected model
  localStorage.setItem(`model_${provider}`, modelSelect.value);
  updateFileInfo();
});

apiKeyInput.addEventListener('input', (e) => {
  const provider = apiProviderSelect.value;
  localStorage.setItem(`api_key_${provider}`, e.target.value.trim());
});

modelSelect.addEventListener('change', (e) => {
  const provider = apiProviderSelect.value;
  localStorage.setItem(`model_${provider}`, e.target.value);
  updateFileInfo();
});

concurrencySlider.addEventListener('input', (e) => {
  concurrencyVal.textContent = e.target.value;
  localStorage.setItem('concurrency', e.target.value);
  updateFileInfo();
});

// Toggle password visibility
toggleKeyVisibility.addEventListener('click', () => {
  if (apiKeyInput.type === 'password') {
    apiKeyInput.type = 'text';
    toggleKeyVisibility.textContent = '🙈';
  } else {
    apiKeyInput.type = 'password';
    toggleKeyVisibility.textContent = '👁️';
  }
});

// Dynamic File Details and Cost Estimator
function updateFileInfo() {
  if (!loadedFile || !parsedBlocks.length) return;
  
  const concurrency = parseInt(concurrencySlider.value, 10);
  const totalChunks = Math.ceil(parsedBlocks.length / 30);
  const estimatedSeconds = Math.ceil((totalChunks / concurrency) * 3.5);
  
  // Cost Calculation
  const provider = apiProviderSelect.value;
  const modelValue = modelSelect.value;
  const modelInfo = (modelsMap[provider] || []).find(m => m.value === modelValue);
  
  let costText = '';
  if (modelInfo) {
    if (modelInfo.inputPrice === 0 && modelInfo.outputPrice === 0) {
      costText = 'Free';
    } else {
      // Prompt template has ~150 tokens overhead per chunk
      const estInputTokens = (parsedBlocks.length * 18) + (totalChunks * 150);
      const estOutputTokens = parsedBlocks.length * 22;
      
      const inputCost = (estInputTokens / 1000000) * modelInfo.inputPrice;
      const outputCost = (estOutputTokens / 1000000) * modelInfo.outputPrice;
      const totalCost = inputCost + outputCost;
      
      if (totalCost < 0.0001) {
        costText = '< $0.0001';
      } else {
        costText = `$${totalCost.toFixed(4)}`;
      }
    }
  } else {
    costText = 'N/A';
  }
  
  loadedFileInfo.textContent = `${parsedBlocks.length} blocks • Est. time: ~${estimatedSeconds}s • Est. Cost: ${costText}`;
}

// File Handling Setup
function setLoadedFile(file) {
  if (!file) return;
  loadedFile = file;
  parsedBlocks = window.SubtitleTranslator.parseSRT(file.content);
  
  loadedFileName.textContent = file.name;
  updateFileInfo();
  
  // Transition to Translation view
  viewUpload.classList.remove('active');
  viewYt.classList.remove('active');
  viewTranslate.classList.add('active');
  
  // Reset logs and success states
  progressContainer.style.display = 'none';
  successBox.style.display = 'none';
  startBtn.style.display = 'inline-flex';
  resetBtn.style.display = 'none';
  
  logOutput.textContent = 'Ready to translate...';
  previewOutput.textContent = 'ترجمه زیرنویس در این بخش نمایش داده می‌شود.';
  progressBarFill.style.width = '0%';
  progressPct.textContent = '0%';
}

// Drag and drop handlers
dropZone.addEventListener('dragover', (e) => {
  e.preventDefault();
  dropZone.classList.add('dragover');
});

dropZone.addEventListener('dragleave', () => {
  dropZone.classList.remove('dragover');
});

dropZone.addEventListener('drop', (e) => {
  e.preventDefault();
  dropZone.classList.remove('dragover');
  
  if (e.dataTransfer.files.length > 0) {
    const file = e.dataTransfer.files[0];
    if (file.name.endsWith('.srt')) {
      const reader = new FileReader();
      reader.onload = (event) => {
        setLoadedFile({
          name: file.name,
          path: file.path || file.name,
          content: event.target.result
        });
      };
      reader.readAsText(file);
    } else {
      alert('Please drop a valid .srt file.');
    }
  }
});

// Browse click
browseBtn.addEventListener('click', async () => {
  const file = await window.electronAPI.selectSrtFile();
  if (file) {
    setLoadedFile(file);
  }
});

// Translation Execution Queue
async function startTranslation() {
  const provider = apiProviderSelect.value;
  const apiKey = apiKeyInput.value.trim();
  const model = modelSelect.value;
  const concurrency = parseInt(concurrencySlider.value, 10);
  
  if (!apiKey) {
    alert('Please enter your API Key in the sidebar settings first.');
    return;
  }
  
  isTranslating = true;
  shouldCancel = false;
  translationProgress = {};
  
  // UI setup
  startBtn.style.display = 'none';
  resetBtn.style.display = 'inline-flex';
  resetBtn.textContent = 'Cancel';
  progressContainer.style.display = 'flex';
  successBox.style.display = 'none';
  statusBadge.className = 'status-badge working';
  statusBadge.textContent = 'Translating';
  
  logOutput.textContent = '';
  previewOutput.textContent = '';
  
  const chunks = window.SubtitleTranslator.chunkBlocks(parsedBlocks, 30);
  const totalChunks = chunks.length;
  let completedChunks = 0;
  
  appendLog(`Split file into ${totalChunks} chunks of blocks.\nStarting translation using ${model}...`);
  
  // Set progress fill
  updateProgress(0);

  // Queue state
  let chunkIdx = 0;
  
  // Worker function
  async function worker(workerId) {
    while (chunkIdx < totalChunks && !shouldCancel) {
      const currentIdx = chunkIdx++;
      const chunk = chunks[currentIdx];
      const startB = chunk[0].index;
      const endB = chunk[chunk.length - 1].index;
      
      appendLog(`[Worker ${workerId}] Translating blocks ${startB}–${endB}...`);
      
      const prompt = window.SubtitleTranslator.formatPrompt(chunk);
      let success = false;
      let retries = 3;
      
      while (retries > 0 && !success && !shouldCancel) {
        try {
          const baseURL = provider === 'liara'
            ? 'https://ai.liara.ir/api/6a0ccd2d298429714a4b3e25/v1'
            : 'https://openrouter.ai/api/v1';
            
          const responseText = await window.SubtitleTranslator.callOpenAICompatibleAPI(baseURL, apiKey, model, prompt);
          
          const parsedCount = window.SubtitleTranslator.parseTranslationResponse(responseText, translationProgress);
          success = true;
          completedChunks++;
          
          appendLog(`[Worker ${workerId}] Completed blocks ${startB}–${endB} (${parsedCount}/${chunk.length} blocks success).`);
          
          // Show some translation in preview box
          updatePreview();
          
          // Update progress percentage
          const pct = Math.round((completedChunks / totalChunks) * 100);
          updateProgress(pct);
          
        } catch (err) {
          retries--;
          appendLog(`[Worker ${workerId}] Error on blocks ${startB}–${endB}: ${err.message}. Retries left: ${retries}`);
          if (retries > 0 && !shouldCancel) {
            // Backoff before retry
            await new Promise(r => setTimeout(r, 2000));
          }
        }
      }
      
      if (!success && !shouldCancel) {
        appendLog(`[Worker ${workerId}] Failed chunk ${startB}–${endB} after all retries.`);
      }
    }
  }

  // Spawn parallel workers
  const workers = [];
  const activeWorkersCount = Math.min(concurrency, totalChunks);
  for (let i = 1; i <= activeWorkersCount; i++) {
    workers.push(worker(i));
  }
  
  await Promise.all(workers);
  
  isTranslating = false;
  resetBtn.style.display = 'none';
  
  if (shouldCancel) {
    statusBadge.className = 'status-badge';
    statusBadge.textContent = 'Cancelled';
    appendLog('\nTranslation cancelled by user.');
    startBtn.style.display = 'inline-flex';
    return;
  }
  
  statusBadge.className = 'status-badge';
  statusBadge.textContent = 'Done';
  appendLog('\nTranslation process completed!');
  
  // Show Success Card
  successBox.style.display = 'flex';
}

function appendLog(message) {
  logOutput.textContent += message + '\n';
  logOutput.scrollTop = logOutput.scrollHeight;
}

function updateProgress(pct) {
  progressPct.textContent = `${pct}%`;
  progressBarFill.style.width = `${pct}%`;
}

function updatePreview() {
  // Show last 5 translated blocks in the preview container
  const keys = Object.keys(translationProgress).map(Number).sort((a, b) => b - a); // descending order
  const lastKeys = keys.slice(0, 8).reverse();
  
  let html = '';
  for (const k of lastKeys) {
    html += `<div><strong>${k}:</strong> ${translationProgress[k]}</div>`;
  }
  previewOutput.innerHTML = html || 'Translating...';
}

// Button Events
startBtn.addEventListener('click', () => {
  if (!isTranslating && loadedFile) {
    startTranslation();
  }
});

resetBtn.addEventListener('click', () => {
  if (isTranslating) {
    shouldCancel = true;
    resetBtn.disabled = true;
    resetBtn.textContent = 'Cancelling...';
  }
});

saveBtn.addEventListener('click', async () => {
  if (!loadedFile) return;
  
  // Compile SRT
  const finalSrtContent = window.SubtitleTranslator.compileSRT(parsedBlocks, translationProgress);
  const defaultSaveName = loadedFile.name.replace('.srt', '_fa.srt');
  
  const result = await window.electronAPI.saveSrtFile({
    defaultName: defaultSaveName,
    content: finalSrtContent
  });
  
  if (result.success) {
    alert(`Subtitle saved successfully to:\n${result.path}`);
  }
});

// Tab Switching Events
tabYt.addEventListener('click', () => {
  tabYt.classList.add('active');
  tabFile.classList.remove('active');
  viewYt.classList.add('active');
  viewUpload.classList.remove('active');
});

tabFile.addEventListener('click', () => {
  tabFile.classList.add('active');
  tabYt.classList.remove('active');
  viewUpload.classList.add('active');
  viewYt.classList.remove('active');
});

// YouTube Subtitles Fetch Event
ytFetchBtn.addEventListener('click', async () => {
  const url = ytUrlInput.value.trim();
  const lang = ytLangSelect.value;
  
  if (!url) {
    alert('Please enter a YouTube video URL.');
    return;
  }
  
  ytFetchBtn.disabled = true;
  ytFetchBtn.textContent = 'Fetching...';
  
  try {
    const result = await window.electronAPI.extractYoutubeSubtitles({ url, lang });
    
    setLoadedFile({
      name: `${result.title}.srt`,
      path: `${result.title}.srt`,
      content: result.content
    });
    
    // Clear input
    ytUrlInput.value = '';
  } catch (err) {
    alert(`Failed to fetch subtitles: ${err.message}`);
  } finally {
    ytFetchBtn.disabled = false;
    ytFetchBtn.textContent = 'Fetch Subtitles';
  }
});

doneBtn.addEventListener('click', () => {
  // Reset back to active upload tab
  loadedFile = null;
  parsedBlocks = [];
  translationProgress = {};
  
  viewTranslate.classList.remove('active');
  
  if (tabYt.classList.contains('active')) {
    viewYt.classList.add('active');
  } else {
    viewUpload.classList.add('active');
  }
  
  resetBtn.disabled = false;
});

// Run Init
initSettings();
