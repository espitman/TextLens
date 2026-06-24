// Core Subtitle Translation Logic

/**
 * Parses SRT content into an array of subtitle blocks.
 * Each block: { index: number, time: string, text: string }
 */
function parseSRT(content) {
  const normalized = content.replace(/\r\n/g, '\n').replace(/\r/g, '\n');
  const rawBlocks = normalized.trim().split(/\n\n+/);
  
  const blocks = [];
  for (const raw of rawBlocks) {
    const lines = raw.split('\n');
    if (lines.length >= 3) {
      const indexStr = lines[0].trim();
      const timeLine = lines[1].trim();
      const textLine = lines.slice(2).join(' ').trim();
      
      const index = parseInt(indexStr, 10);
      if (!isNaN(index) && timeLine.includes('-->')) {
        blocks.append = blocks.push({
          index,
          time: timeLine,
          text: textLine
        });
      }
    }
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
function parseTranslationResponse(responseText, progressMap) {
  const lines = responseText.split('\n');
  let count = 0;
  
  for (const line of lines) {
    const match = line.trim().match(/^(\d+)\s*:\s*(.*)$/);
    if (match) {
      const index = parseInt(match[1], 10);
      const text = match[2].trim();
      progressMap[index] = text;
      count++;
    }
  }
  return count;
}

/**
 * Reconstructs the translated blocks back into a valid SRT string.
 */
function compileSRT(blocks, progressMap) {
  const lines = [];
  for (const b of blocks) {
    const translation = progressMap[b.index] || b.text; // Fallback to English if not translated
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
  compileSRT
};
