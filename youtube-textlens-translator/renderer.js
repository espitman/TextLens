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
const tabSubtitleTo = document.getElementById('tab-subtitleto');
const tabFile = document.getElementById('tab-file');
const viewYt = document.getElementById('view-yt');
const viewSubtitleTo = document.getElementById('view-subtitleto');
const viewUpload = document.getElementById('view-upload');
const viewTranslate = document.getElementById('view-translate');
const dropZone = document.getElementById('drop-zone');
const browseBtn = document.getElementById('browse-btn');

const ytUrlInput = document.getElementById('yt-url');
const ytFetchBtn = document.getElementById('yt-fetch-btn');
const ytLangSelect = document.getElementById('yt-lang');
const subtitleToUrlInput = document.getElementById('subtitleto-url');
const subtitleToOpenBtn = document.getElementById('subtitleto-open-btn');
const subtitleToStatus = document.getElementById('subtitleto-status');
const subtitleToWebview = document.getElementById('subtitleto-webview');
const subtitleToEmpty = document.getElementById('subtitleto-empty');

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
let subtitleToRunId = 0;
let subtitleToAutomationRunning = false;

// API Models configuration with pricing (per 1M tokens in USD)
const modelsMap = {
  openrouter: [
    { value: 'google/gemma-4-31b-it:free', name: 'Gemma 4 31B (Free)', inputPrice: 0, outputPrice: 0 },
    { value: 'openai/gpt-4.1-mini', name: 'GPT 4.1 Mini', inputPrice: 0.15, outputPrice: 0.60 },
    { value: 'google/gemini-2.0-flash-lite-001', name: 'Gemini 2.0 Flash Lite', inputPrice: 0.075, outputPrice: 0.30 },
    { value: 'anthropic/claude-3.5-sonnet', name: 'Claude 3.5 Sonnet', inputPrice: 3.00, outputPrice: 15.00 }
  ],
  liara: [
    { value: 'openai/gpt-5-nano', name: 'GPT 5 Nano (Default)', inputPrice: 5000, outputPrice: 20000 },
    { value: 'openai/gpt-4.1-mini', name: 'GPT 4.1 Mini', inputPrice: 10000, outputPrice: 40000 },
    { value: 'google/gemma-3-27b-it', name: 'Gemma 3 27B', inputPrice: 6500, outputPrice: 26000 },
    { value: 'google/gemini-2.0-flash-lite-001', name: 'Gemini 2.0 Flash Lite', inputPrice: 5000, outputPrice: 20000 }
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
      
      if (provider === 'liara') {
        // Liara prices are in Tomans (تومان)
        if (totalCost < 1) {
          costText = '< ۱ تومان';
        } else {
          costText = `${Math.ceil(totalCost).toLocaleString('fa-IR')} تومان`;
        }
      } else {
        // OpenRouter is in USD ($)
        if (totalCost < 0.0001) {
          costText = '< $0.0001';
        } else {
          costText = `$${totalCost.toFixed(4)}`;
        }
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
  viewSubtitleTo.classList.remove('active');
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
function activateInputView(activeTab, activeView) {
  [tabYt, tabSubtitleTo, tabFile].forEach(tab => tab.classList.remove('active'));
  [viewYt, viewSubtitleTo, viewUpload, viewTranslate].forEach(view => view.classList.remove('active'));
  activeTab.classList.add('active');
  activeView.classList.add('active');
}

tabYt.addEventListener('click', () => {
  activateInputView(tabYt, viewYt);
});

tabSubtitleTo.addEventListener('click', () => {
  activateInputView(tabSubtitleTo, viewSubtitleTo);
});

tabFile.addEventListener('click', () => {
  activateInputView(tabFile, viewUpload);
});

function buildSubtitleToUrl(rawUrl) {
  const trimmed = String(rawUrl || '').trim();
  if (!trimmed) return '';
  const normalized = /^[a-z][a-z0-9+.-]*:\/\//i.test(trimmed)
    ? trimmed
    : `https://${trimmed}`;
  return `https://subtitle.to/${normalized}`;
}

function openSubtitleTo() {
  const targetUrl = buildSubtitleToUrl(subtitleToUrlInput.value);
  if (!targetUrl) {
    alert('Please enter a YouTube video URL.');
    return;
  }

  subtitleToRunId++;
  subtitleToAutomationRunning = false;
  subtitleToOpenBtn.disabled = true;
  subtitleToOpenBtn.textContent = 'Fetching...';
  subtitleToWebview.setAttribute('src', targetUrl);
  subtitleToWebview.classList.add('active');
  subtitleToEmpty.classList.remove('hidden');
  subtitleToStatus.textContent = 'Loading subtitle.to in background...';
}

subtitleToOpenBtn.addEventListener('click', openSubtitleTo);
subtitleToUrlInput.addEventListener('keydown', (event) => {
  if (event.key === 'Enter') {
    openSubtitleTo();
  }
});

async function clickFirstSubtitleToSrt() {
  if (!subtitleToWebview.getAttribute('src')) {
    return { ok: false, reason: 'Open a subtitle.to page first.' };
  }

  return subtitleToWebview.executeJavaScript(`
      (() => {
        const visible = (el) => {
          const rect = el.getBoundingClientRect();
          const style = window.getComputedStyle(el);
          return rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden' && style.display !== 'none';
        };

        const textOf = (el) => (el.innerText || el.textContent || '').replace(/\\s+/g, ' ').trim();
        const candidates = Array.from(document.querySelectorAll('a, button, [role="button"]'))
          .filter(visible)
          .map((el) => {
            const text = textOf(el);
            const label = [
              text,
              el.getAttribute('aria-label') || '',
              el.getAttribute('title') || '',
              el.getAttribute('href') || ''
            ].join(' ');
            return { el, text, label, rowText: textOf(el.closest('tr, li, .row, .list-group-item, div') || el) };
          })
          .filter((item) => {
            const shortButtonText = item.text.length <= 24 && /(^|\\b)SRT(\\b|$)/i.test(item.text);
            const explicitSrtLabel = /(^|[\\s/_?&=-])SRT($|[\\s/_?&=-])/i.test(item.label);
            const hugePageText = item.text.length > 80;
            return !hugePageText && (shortButtonText || explicitSrtLabel);
          });

        const englishCandidate = candidates.find((item) => {
          return /English/i.test(item.rowText);
        });

        const target = (englishCandidate || candidates[0])?.el;
        if (!target) {
          return { ok: false, reason: 'No visible SRT button found.' };
        }

        target.scrollIntoView({ block: 'center', inline: 'center' });
        target.click();
        return { ok: true, text: textOf(target) };
      })();
    `);
}

async function runSubtitleToAutomation(runId) {
  if (subtitleToAutomationRunning) return;
  subtitleToAutomationRunning = true;

  for (let attempt = 1; attempt <= 30; attempt++) {
    if (runId !== subtitleToRunId) return;

    subtitleToStatus.textContent = `Searching for SRT... ${attempt}/30`;
    await new Promise(resolve => setTimeout(resolve, attempt <= 3 ? 900 : 1500));

    try {
      const result = await clickFirstSubtitleToSrt();
      if (result && result.ok) {
        subtitleToStatus.textContent = `Clicked ${result.text || 'SRT'}, waiting for download...`;
        return;
      }
    } catch (error) {
      subtitleToStatus.textContent = error.message || 'Could not inspect subtitle.to.';
    }
  }

  if (runId === subtitleToRunId) {
    subtitleToStatus.textContent = 'SRT button not found. Try again or use the normal YouTube fetch.';
    subtitleToOpenBtn.disabled = false;
    subtitleToOpenBtn.textContent = 'Fetch SRT';
  }
}

subtitleToWebview.addEventListener('did-finish-load', () => {
  subtitleToStatus.textContent = 'Page loaded. Waiting for SRT list...';
  runSubtitleToAutomation(subtitleToRunId);
});
subtitleToWebview.addEventListener('did-fail-load', (event) => {
  if (event.errorCode !== -3) {
    subtitleToStatus.textContent = `Load failed: ${event.errorDescription || event.errorCode}`;
    subtitleToOpenBtn.disabled = false;
    subtitleToOpenBtn.textContent = 'Fetch SRT';
  }
});

window.electronAPI.onSubtitleToDownload((payload) => {
  subtitleToOpenBtn.disabled = false;
  subtitleToOpenBtn.textContent = 'Fetch SRT';

  if (!payload || !payload.ok) {
    subtitleToStatus.textContent = payload?.error || 'Download failed.';
    return;
  }

  subtitleToStatus.textContent = `Downloaded ${payload.name}.`;
  setLoadedFile({
    name: payload.name,
    path: payload.path,
    content: payload.content
  });
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
  } else if (tabSubtitleTo.classList.contains('active')) {
    viewSubtitleTo.classList.add('active');
  } else {
    viewUpload.classList.add('active');
  }
  
  resetBtn.disabled = false;
});

// Run Init
initSettings();
