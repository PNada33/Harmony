const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

function stableStringify(value) {
  if (Array.isArray(value)) {
    return `[${value.map((item) => stableStringify(item)).join(',')}]`;
  }
  if (value && typeof value === 'object') {
    const keys = Object.keys(value).sort();
    return `{${keys.map((key) => `${JSON.stringify(key)}:${stableStringify(value[key])}`).join(',')}}`;
  }
  return JSON.stringify(value);
}

function randomUuid() {
  if (crypto.randomUUID) {
    return crypto.randomUUID();
  }
  const bytes = crypto.randomBytes(16);
  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  const hex = bytes.toString('hex');
  return [
    hex.slice(0, 8),
    hex.slice(8, 12),
    hex.slice(12, 16),
    hex.slice(16, 20),
    hex.slice(20),
  ].join('-');
}

function clone(value) {
  return JSON.parse(JSON.stringify(value));
}

function buildBaseKey(element) {
  const base = {};
  for (const [key, value] of Object.entries(element)) {
    if (key === 'uuid' || key === 'vertices' || key === 'faces') {
      continue;
    }
    base[key] = value;
  }
  return stableStringify(base);
}

function mergeElements(elements, index, label) {
  const first = clone(elements[0]);
  first.uuid = randomUuid();
  first.vertices = {};
  first.faces = {};
  first.name = elements.length > 1 ? `${label}_${index + 1}` : first.name;

  let vertexCounter = 0;
  let faceCounter = 0;

  for (const element of elements) {
    const vertexMap = new Map();

    for (const [oldVertexKey, coords] of Object.entries(element.vertices || {})) {
      const newVertexKey = `v${String(vertexCounter++).padStart(6, '0')}`;
      first.vertices[newVertexKey] = coords;
      vertexMap.set(oldVertexKey, newVertexKey);
    }

    for (const face of Object.values(element.faces || {})) {
      const newFaceKey = `f${String(faceCounter++).padStart(6, '0')}`;
      const nextFace = clone(face);
      nextFace.vertices = (face.vertices || []).map((vertexKey) => vertexMap.get(vertexKey));

      if (nextFace.uv && typeof nextFace.uv === 'object' && !Array.isArray(nextFace.uv)) {
        const nextUv = {};
        for (const [oldVertexKey, uvValue] of Object.entries(nextFace.uv)) {
          const mappedVertexKey = vertexMap.get(oldVertexKey);
          nextUv[mappedVertexKey] = uvValue;
        }
        nextFace.uv = nextUv;
      }

      first.faces[newFaceKey] = nextFace;
    }
  }

  return first;
}

function optimizeRootGroup(group, uuidToElement) {
  const groupedChildren = new Map();
  const passthroughChildren = [];

  for (const child of group.children || []) {
    if (typeof child !== 'string') {
      passthroughChildren.push(clone(child));
      continue;
    }

    const element = uuidToElement.get(child);
    if (!element) {
      passthroughChildren.push(child);
      continue;
    }

    const key = buildBaseKey(element);
    if (!groupedChildren.has(key)) {
      groupedChildren.set(key, []);
    }
    groupedChildren.get(key).push(element);
  }

  const mergedElements = [];
  const nextChildren = [];
  let mergedIndex = 0;

  for (const elements of groupedChildren.values()) {
    const merged = mergeElements(elements, mergedIndex, group.name || 'merged_part');
    mergedIndex += 1;
    mergedElements.push(merged);
    nextChildren.push(merged.uuid);
  }

  for (const child of passthroughChildren) {
    nextChildren.push(child);
  }

  const nextGroup = clone(group);
  nextGroup.children = nextChildren;
  if (!nextGroup.name) {
    nextGroup.name = 'optimized_group';
  }

  return { group: nextGroup, elements: mergedElements };
}

function main() {
  const inputPath = process.argv[2];
  if (!inputPath) {
    console.error('Usage: node optimize-bbmodel.js <path-to-model.bbmodel>');
    process.exit(1);
  }

  const source = JSON.parse(fs.readFileSync(inputPath, 'utf8'));
  const uuidToElement = new Map((source.elements || []).map((element) => [element.uuid, element]));

  const optimizedElements = [];
  const optimizedOutliner = [];

  for (const item of source.outliner || []) {
    if (typeof item === 'string') {
      const element = uuidToElement.get(item);
      if (element) {
        const merged = mergeElements([element], optimizedElements.length, 'root_part');
        optimizedElements.push(merged);
        optimizedOutliner.push(merged.uuid);
      } else {
        optimizedOutliner.push(item);
      }
      continue;
    }

    const optimizedGroup = optimizeRootGroup(item, uuidToElement);
    optimizedElements.push(...optimizedGroup.elements);
    optimizedOutliner.push(optimizedGroup.group);
  }

  const result = {
    ...source,
    elements: optimizedElements,
    outliner: optimizedOutliner,
  };

  const inputDir = path.dirname(inputPath);
  const inputBaseName = path.basename(inputPath, path.extname(inputPath));
  const jsonOutputPath = path.join(inputDir, `${inputBaseName}.optimized.json`);
  const bbmodelOutputPath = path.join(inputDir, `${inputBaseName}.optimized.bbmodel`);

  const output = JSON.stringify(result);
  fs.writeFileSync(jsonOutputPath, output, 'utf8');
  fs.writeFileSync(bbmodelOutputPath, output, 'utf8');

  const originalCount = (source.elements || []).length;
  const optimizedCount = optimizedElements.length;
  const rootSummary = (optimizedOutliner || []).map((item) => {
    if (typeof item === 'string') {
      return 1;
    }
    return (item.children || []).length;
  });

  console.log(JSON.stringify({
    input: inputPath,
    originalCount,
    optimizedCount,
    reducedBy: originalCount - optimizedCount,
    outputs: {
      json: jsonOutputPath,
      bbmodel: bbmodelOutputPath,
    },
    rootSummary,
  }, null, 2));
}

main();
