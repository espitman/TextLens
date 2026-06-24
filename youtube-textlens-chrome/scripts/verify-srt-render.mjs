import fs from "node:fs";
import path from "node:path";
import vm from "node:vm";

const root = path.resolve(path.dirname(new URL(import.meta.url).pathname), "..");

function createNoopElement(overrides = {}) {
  const element = {
    addEventListener() {},
    appendChild() {},
    classList: { add() {}, remove() {}, toggle() {}, contains() { return false; } },
    dataset: {},
    querySelector() { return createNoopElement(); },
    querySelectorAll() { return []; },
    releasePointerCapture() {},
    remove() {},
    setAttribute() {},
    setPointerCapture() {},
    style: {},
    textContent: ""
  };
  return Object.assign(element, overrides);
}

function loadContentSandbox() {
  const code = fs.readFileSync(path.join(root, "content.js"), "utf8");
  const store = {};
  const createdElements = [];
  const documentElements = new Map();
  const player = createNoopElement({
    appendChild(element) {
      if (element.id) documentElements.set(element.id, element);
      createdElements.push(element);
    }
  });
  const video = {
    currentTime: 0,
    duration: 12,
    paused: false,
    playbackRate: 1,
    parentElement: player
  };
  const documentElement = createNoopElement({
    appendChild(element) {
      if (element.id) documentElements.set(element.id, element);
      createdElements.push(element);
    }
  });
  const sandbox = {
    chrome: {
      storage: {
        local: {
          get: async (key) => {
            if (typeof key === "string") return { [key]: store[key] };
            if (Array.isArray(key)) {
              return Object.fromEntries(key.map((item) => [item, store[item]]));
            }
            if (key && typeof key === "object") {
              return Object.fromEntries(Object.keys(key).map((item) => [item, store[item] ?? key[item]]));
            }
            return { ...store };
          },
          set: async (value) => {
            Object.assign(store, value);
          }
        }
      }
    },
    console,
    URL,
    document: {
      createElement() {
        const element = createNoopElement();
        createdElements.push(element);
        return element;
      },
      documentElement,
      getElementById(id) { return documentElements.get(id) || null; },
      querySelector(selector) {
        if (selector === "video") return video;
        if (selector === "#movie_player") return player;
        return null;
      },
      title: "TextLens test - YouTube"
    },
    getComputedStyle() { return { position: "relative" }; },
    history: { pushState() {}, replaceState() {} },
    setInterval() {},
    setTimeout() {},
    window: {
      addEventListener() {},
      location: { href: "https://www.youtube.com/watch?v=abcdefghijk" },
      setTimeout() {}
    }
  };
  vm.createContext(sandbox);
  vm.runInContext(code, sandbox);
  sandbox.__store = store;
  sandbox.__video = video;
  return sandbox;
}

async function verifySrtLoadAndRender() {
  const sandbox = loadContentSandbox();
  const sampleSrt = [
    "1",
    "00:00:00,440 --> 00:00:02,800",
    "سلام",
    "",
    "2",
    "00:00:04,080 --> 00:00:05,640",
    "خداحافظ",
    ""
  ].join("\n");

  const cues = sandbox.parseSrt(sampleSrt);
  if (cues.length !== 2) throw new Error(`Expected 2 cues, got ${cues.length}`);

  await sandbox.loadManualSrtFromInput({
    name: "sample.srt",
    text: async () => sampleSrt
  });

  const saved = sandbox.__store.textLensManualSubtitles?.abcdefghijk;
  if (!saved) throw new Error("SRT was not saved for the active video.");
  if (saved.cueCount !== 2) throw new Error(`Expected 2 saved cues, got ${saved.cueCount}`);

  const subtitle = sandbox.document.getElementById("textlens-youtube-subtitle");
  if (!subtitle) throw new Error("Subtitle overlay was not created.");

  sandbox.renderSubtitleAt(1);
  if (subtitle.textContent !== "سلام") {
    throw new Error(`Expected first cue text, got: ${subtitle.textContent}`);
  }

  sandbox.renderSubtitleAt(5);
  if (subtitle.textContent !== "خداحافظ") {
    throw new Error(`Expected second cue text, got: ${subtitle.textContent}`);
  }
}

await verifySrtLoadAndRender();
console.log("ok srt load + render");
