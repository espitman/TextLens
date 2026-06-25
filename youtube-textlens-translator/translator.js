// Core Subtitle Translation Logic

/**
 * Parses SRT content into an array of subtitle blocks.
 * Each block: { index: number, time: string, text: string }
 */
function parseSRT(content) {
  // Normalize line endings
  const lines = content.replace(/\r\n/g, '\n').replace(/\r/g, '\n').split('\n');
  const blocks = [];
  let currentBlock = null;
  
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].trim();
    
    // Check if line is a number (starts a new block)
    if (/^\d+$/.test(line)) {
      if (currentBlock && currentBlock.time) {
        blocks.push({
          index: currentBlock.index,
          time: currentBlock.time,
          text: currentBlock.textLines.join(' ').trim()
        });
      }
      currentBlock = {
        index: parseInt(line, 10),
        time: '',
        textLines: []
      };
    } else if (currentBlock && line.includes('-->')) {
      currentBlock.time = line;
    } else if (currentBlock && currentBlock.time) {
      if (line !== '') {
        currentBlock.textLines.push(line);
      }
    }
  }
  
  // Push the final block
  if (currentBlock && currentBlock.time) {
    blocks.push({
      index: currentBlock.index,
      time: currentBlock.time,
      text: currentBlock.textLines.join(' ').trim()
    });
  }
  
  return blocks;
}

/**
 * Creates chunks from the parsed blocks.
 */
function chunkBlocks(blocks, chunkSize = 40) {
  const chunks = [];
  for (let i = 0; i < blocks.length; i += chunkSize) {
    chunks.push(blocks.slice(i, i + chunkSize));
  }
  return chunks;
}

/**
 * Formats a block chunk into a prompt for the model.
 */
function formatPrompt(chunk) {
  const lines = chunk.map(b => `${b.index}: ${b.text}`).join('\n');
  
  return `You are an expert subtitle translator. Translate the following English subtitle blocks into fluent, natural, and idiomatic Persian (Farsi).
Maintain the exact index prefix and structure. Do NOT add notes, explanations, or change the indexes.
Return exactly ${chunk.length} translated lines. Every requested index must appear exactly once.
Translate every line to Persian. Do not leave any English sentence untranslated.
Keep special tags like [Music] or [Applause] in their appropriate Persian equivalents (e.g. [موسیقی] or [تشویق]).
Correct any clear speech-to-text spelling/names errors (e.g. edit names of players or teams if they are obviously misspelled in the English transcript).

Format the output strictly as:
[index]: [Persian Translation]

Here are the lines to translate:
${lines}`;
}

/**
 * Calls OpenAI-compatible completion API (like OpenRouter or Liara)
 */
async function callOpenAICompatibleAPI(baseURL, apiKey, modelName, prompt) {
  const url = `${baseURL.replace(/\/$/, '')}/chat/completions`;
  
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${apiKey}`
  };
  
  // Add OpenRouter specific headers if calling OpenRouter
  if (baseURL.includes('openrouter.ai')) {
    headers['HTTP-Referer'] = 'https://github.com/espitman/TextLens';
    headers['X-Title'] = 'TextLens Subtitle Translator';
  }
  
  const response = await fetch(url, {
    method: 'POST',
    headers: headers,
    body: JSON.stringify({
      model: modelName,
      messages: [
        { role: 'user', content: prompt }
      ],
      temperature: 0.2
    })
  });
  
  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`API Error (${response.status}): ${errText}`);
  }
  
  const data = await response.json();
  try {
    return data.choices[0].message.content;
  } catch (e) {
    throw new Error("Invalid response format from API");
  }
}

/**
 * Parses the translated response and maps indexes to their Persian translations.
 */
function parseTranslationResponse(responseText, progressMap, allowedIndexes = null) {
  const lines = responseText.split('\n');
  const allowed = allowedIndexes ? new Set(allowedIndexes) : null;
  const parsed = new Map();
  let currentIndex = null;
  let currentText = [];
  let count = 0;

  const normalizeDigits = (value) => String(value)
    .replace(/[۰-۹]/g, (digit) => '۰۱۲۳۴۵۶۷۸۹'.indexOf(digit))
    .replace(/[٠-٩]/g, (digit) => '٠١٢٣٤٥٦٧٨٩'.indexOf(digit));

  const flush = () => {
    if (currentIndex === null) return;
    if (allowed && !allowed.has(currentIndex)) {
      currentIndex = null;
      currentText = [];
      return;
    }

    const text = currentText.join(' ').replace(/\s+/g, ' ').trim();
    if (text) {
      parsed.set(currentIndex, text);
    }
    currentIndex = null;
    currentText = [];
  };
  
  for (const line of lines) {
    const trimmed = line.trim();
    const match = trimmed.match(/^(?:[-*]\s*)?\[?([0-9۰-۹٠-٩]+)\]?\s*(?::|\.|\)|-|–|—)\s*(.*)$/);
    if (match) {
      flush();
      currentIndex = parseInt(normalizeDigits(match[1]), 10);
      currentText = [match[2].trim()].filter(Boolean);
    } else if (currentIndex !== null && trimmed) {
      currentText.push(trimmed);
    }
  }
  flush();

  for (const [index, text] of parsed.entries()) {
    progressMap[index] = text;
    count++;
  }

  return count;
}

function missingTranslations(blocks, progressMap) {
  return blocks.filter((block) => !String(progressMap[block.index] || '').trim());
}

/**
 * Reconstructs the translated blocks back into a valid SRT string.
 */
function compileSRT(blocks, progressMap) {
  const lines = [];
  for (const b of blocks) {
    const translation = progressMap[b.index] || '';
    lines.push(`${b.index}`);
    lines.push(`${b.time}`);
    lines.push(`${translation}`);
    lines.push(''); // Empty line separator
  }
  return lines.join('\n');
}

// Export for renderer UI
window.SubtitleTranslator = {
  parseSRT,
  chunkBlocks,
  formatPrompt,
  callOpenAICompatibleAPI,
  parseTranslationResponse,
  missingTranslations,
  compileSRT
};
