(function () {
  "use strict";

  if (!window.CloudStore) {
    console.error("CloudStore not initialized");
    return;
  }

  const refs = {
    configList: document.getElementById("configList"),
    searchInput: document.getElementById("searchInput"),
    refreshBtn: document.getElementById("refreshBtn"),
    modeChip: document.getElementById("modeChip"),
    editorTitle: document.getElementById("editorTitle"),
    nameInput: document.getElementById("nameInput"),
    newBtn: document.getElementById("newBtn"),
    saveBtn: document.getElementById("saveBtn"),
    deleteBtn: document.getElementById("deleteBtn"),
    exportBtn: document.getElementById("exportBtn"),
    importBtn: document.getElementById("importBtn"),
    statusLine: document.getElementById("statusLine")
  };

  const state = {
    configs: [],
    selectedId: null,
    filter: ""
  };

  function setStatus(text, type) {
    refs.statusLine.textContent = text || "";
    refs.statusLine.classList.remove("ok", "error");
    if (type === "ok") {
      refs.statusLine.classList.add("ok");
    }
    if (type === "error") {
      refs.statusLine.classList.add("error");
    }
  }

  function isPlainObject(value) {
    return value && typeof value === "object" && !Array.isArray(value);
  }

  function pickName(source, fallbackName) {
    if (!isPlainObject(source)) {
      return fallbackName;
    }

    const candidates = [source.name, source.configName, source.title];
    for (const candidate of candidates) {
      if (typeof candidate === "string" && candidate.trim()) {
        return candidate.trim();
      }
    }

    return fallbackName;
  }

  function extractPayload(source) {
    if (!isPlainObject(source)) {
      throw new Error("Конфиг должен быть JSON-объектом.");
    }

    if (window.CloudCrypto && typeof source[window.CloudCrypto.payloadKey] === "string") {
      return window.CloudCrypto.decodePayloadObject(source);
    }

    if (isPlainObject(source.payload)) {
      return source.payload;
    }

    if (isPlainObject(source.config)) {
      return source.config;
    }

    if (isPlainObject(source.data)) {
      if (isPlainObject(source.data.payload)) {
        return source.data.payload;
      }
      if (isPlainObject(source.data.config)) {
        return source.data.config;
      }
      return source.data;
    }

    return source;
  }

  function normalizeImportedEntry(entry, fallbackName) {
    if (!isPlainObject(entry)) {
      throw new Error("Элемент импорта должен быть JSON-объектом.");
    }

    const payload = extractPayload(entry);
    if (!isPlainObject(payload)) {
      throw new Error("Payload должен быть JSON-объектом.");
    }

    const normalized = {
      name: pickName(entry, fallbackName),
      payload
    };

    if (typeof entry.id === "string" && entry.id.trim()) {
      normalized.id = entry.id.trim();
    }

    return normalized;
  }

  function formatDate(iso) {
    const date = new Date(iso);
    if (Number.isNaN(date.getTime())) {
      return "дата неизвестна";
    }
    return date.toLocaleString("ru-RU");
  }

  function filteredConfigs() {
    const query = state.filter.trim().toLowerCase();
    if (!query) {
      return state.configs;
    }
    return state.configs.filter((item) => item.name.toLowerCase().includes(query));
  }

  function createConfigItem(config) {
    const item = document.createElement("button");
    item.type = "button";
    item.className = "config-item";
    item.dataset.id = config.id;
    if (config.id === state.selectedId) {
      item.classList.add("active");
    }

    const name = document.createElement("div");
    name.className = "config-name";
    name.textContent = config.name;

    const meta = document.createElement("div");
    meta.className = "config-meta";
    meta.textContent = `updated: ${formatDate(config.updatedAt)}`;

    item.append(name, meta);
    item.addEventListener("click", () => selectConfig(config.id));

    return item;
  }

  function renderList() {
    refs.configList.innerHTML = "";
    const items = filteredConfigs();

    if (!items.length) {
      const empty = document.createElement("div");
      empty.className = "empty";
      empty.textContent = "Конфиги не найдены.";
      refs.configList.appendChild(empty);
      return;
    }

    items.forEach((config) => {
      refs.configList.appendChild(createConfigItem(config));
    });
  }

  function clearEditor() {
    state.selectedId = null;
    refs.editorTitle.textContent = "Новый конфиг";
    refs.nameInput.value = "";
    renderList();
  }

  function selectConfig(id) {
    const config = state.configs.find((item) => item.id === id);
    if (!config) {
      return;
    }

    state.selectedId = id;
    refs.editorTitle.textContent = `Редактирование: ${config.name}`;
    refs.nameInput.value = config.name;
    renderList();
  }

  async function loadConfigs() {
    try {
      setStatus("Загружаю конфиги...");
      state.configs = await window.CloudStore.listConfigs();
      renderList();
      setStatus("");
    } catch (error) {
      console.error(error);
      setStatus(`Ошибка загрузки: ${error.message}`, "error");
    }
  }

  function collectEditorData() {
    if (!state.selectedId) {
      throw new Error("Сначала импортируй конфиг через кнопку import.");
    }

    const selected = state.configs.find((item) => item.id === state.selectedId);
    if (!selected) {
      throw new Error("Выбранный конфиг не найден.");
    }

    const explicitName = refs.nameInput.value.trim();
    const finalName = explicitName || selected.name;
    if (!finalName) {
      throw new Error("Укажи название конфига.");
    }

    return {
      id: selected.id,
      name: finalName,
      payload: selected.payload
    };
  }

  async function saveCurrent() {
    try {
      const data = collectEditorData();
      const saved = await window.CloudStore.upsertConfig(data);
      await loadConfigs();
      selectConfig(saved.id);
      setStatus(`Сохранено: ${saved.name}`, "ok");
    } catch (error) {
      setStatus(`Ошибка сохранения: ${error.message}`, "error");
    }
  }

  async function deleteCurrent() {
    if (!state.selectedId) {
      setStatus("Сначала выбери конфиг для удаления.", "error");
      return;
    }

    const config = state.configs.find((item) => item.id === state.selectedId);
    const title = config ? config.name : "этот конфиг";
    const accepted = window.confirm(`Удалить ${title}?`);
    if (!accepted) {
      return;
    }

    try {
      await window.CloudStore.deleteConfig(state.selectedId);
      clearEditor();
      await loadConfigs();
      setStatus("Конфиг удален.", "ok");
    } catch (error) {
      setStatus(`Ошибка удаления: ${error.message}`, "error");
    }
  }

  async function copyTextToClipboard(text) {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      try {
        await navigator.clipboard.writeText(text);
        return true;
      } catch {
      }
    }

    try {
      const textarea = document.createElement("textarea");
      textarea.value = text;
      textarea.setAttribute("readonly", "readonly");
      textarea.style.position = "fixed";
      textarea.style.opacity = "0";
      textarea.style.pointerEvents = "none";
      document.body.appendChild(textarea);
      textarea.focus();
      textarea.select();
      const copied = document.execCommand("copy");
      textarea.remove();
      return copied;
    } catch {
      return false;
    }
  }

  async function exportCurrent() {
    if (!state.selectedId) {
      setStatus("Выбери конфиг для копирования кода.", "error");
      return;
    }

    const config = state.configs.find((item) => item.id === state.selectedId);
    if (!config) {
      setStatus("Конфиг не найден.", "error");
      return;
    }

    const shareCode = window.CloudCrypto ? window.CloudCrypto.encodeShare(config.name, config.payload) : null;
    if (!shareCode) {
      setStatus("Не удалось сгенерировать код.", "error");
      return;
    }

    const copied = await copyTextToClipboard(shareCode);
    if (!copied) {
      window.prompt("Скопируй код конфига (HCFG1):", shareCode);
      setStatus("Автокопирование заблокировано. Код показан в окне.", "error");
      return;
    }

    setStatus(`Код конфига скопирован: ${config.name}`, "ok");
  }

  function parseImportPayload(rawCode, fallbackName) {
    const raw = String(rawCode || "").trim();
    const shared = window.CloudCrypto ? window.CloudCrypto.decodeShare(raw) : null;
    if (shared) {
      return [normalizeImportedEntry(shared, fallbackName)];
    }

    let parsed;
    try {
      parsed = JSON.parse(raw);
    } catch {
      throw new Error("Вставь валидный HCFG1 код или JSON.");
    }

    if (Array.isArray(parsed)) {
      return parsed.map((entry, index) => normalizeImportedEntry(entry, `${fallbackName}-${index + 1}`));
    }

    if (isPlainObject(parsed) && Array.isArray(parsed.configs)) {
      return parsed.configs.map((entry, index) => normalizeImportedEntry(entry, `${fallbackName}-${index + 1}`));
    }

    if (isPlainObject(parsed) && Array.isArray(parsed.items)) {
      return parsed.items.map((entry, index) => normalizeImportedEntry(entry, `${fallbackName}-${index + 1}`));
    }

    if (isPlainObject(parsed)) {
      return [normalizeImportedEntry(parsed, fallbackName)];
    }

    throw new Error("JSON должен быть объектом или массивом объектов.");
  }

  async function importFromCode() {
    let clipboardText = "";
    if (navigator.clipboard && navigator.clipboard.readText) {
      try {
        clipboardText = await navigator.clipboard.readText();
      } catch {
      }
    }

    const pasted = window.prompt("Вставь HCFG1 код конфига:", clipboardText || "");
    if (pasted === null) {
      return;
    }

    try {
      const importItems = parseImportPayload(pasted, "imported-config");
      if (!importItems.length) {
        throw new Error("Нет данных для импорта.");
      }

      await window.CloudStore.bulkImport(importItems);
      await loadConfigs();
      const importedName = importItems[0]?.name;
      const imported = state.configs.find((item) => item.name === importedName);
      if (imported) {
        selectConfig(imported.id);
      } else {
        clearEditor();
      }
      setStatus(`Импортировано конфигов: ${importItems.length}`, "ok");
    } catch (error) {
      setStatus(`Ошибка импорта: ${error.message}`, "error");
    }
  }

  function bindEvents() {
    refs.searchInput.addEventListener("input", () => {
      state.filter = refs.searchInput.value;
      renderList();
    });

    refs.refreshBtn.addEventListener("click", loadConfigs);
    refs.newBtn.addEventListener("click", clearEditor);
    refs.saveBtn.addEventListener("click", saveCurrent);
    refs.deleteBtn.addEventListener("click", deleteCurrent);
    refs.exportBtn.addEventListener("click", exportCurrent);
    refs.importBtn.addEventListener("click", importFromCode);
  }

  async function init() {
    refs.modeChip.textContent = `Mode: ${window.CloudStore.mode}`;
    bindEvents();
    await loadConfigs();
  }

  init();
})();
