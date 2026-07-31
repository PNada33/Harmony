const fs = require("fs");
const path = require("path");

if (process.argv.length < 4) {
    console.error("Usage: node scripts/convert-gltf-pet.js <sourceDir> <outputJson>");
    process.exit(1);
}

const sourceDir = process.argv[2];
const outputJson = process.argv[3];
const preferredGltfPath = fs.existsSync(path.join(sourceDir, "model.gltf"))
    ? path.join(sourceDir, "model.gltf")
    : path.join(sourceDir, "scene.gltf");
const fallbackGltf = fs.readdirSync(sourceDir).find((file) => file.toLowerCase().endsWith(".gltf"));
const gltfPath = fs.existsSync(preferredGltfPath)
    ? preferredGltfPath
    : fallbackGltf
        ? path.join(sourceDir, fallbackGltf)
        : null;
if (!gltfPath) {
    throw new Error(`No .gltf file found in ${sourceDir}`);
}
const gltf = JSON.parse(fs.readFileSync(gltfPath, "utf8"));
const accessors = new Map();
const textureAtlas = buildTextureAtlas();
const TEXTURE_WIDTH = textureAtlas ? textureAtlas.width : 128;
const TEXTURE_HEIGHT = textureAtlas ? textureAtlas.height : 128;
const UV_INSET_U = 0.35 / TEXTURE_WIDTH;
const UV_INSET_V = 0.35 / TEXTURE_HEIGHT;

function readBuffer(uri) {
    if (uri.startsWith("data:")) {
        const commaIndex = uri.indexOf(",");
        return Buffer.from(uri.slice(commaIndex + 1), "base64");
    }
    return fs.readFileSync(path.join(sourceDir, uri));
}

const binary = readBuffer(gltf.buffers[0].uri);

function nextPowerOfTwo(value) {
    let result = 1;
    while (result < value) {
        result <<= 1;
    }
    return result;
}

function getImageBytes(uri) {
    if (!uri) {
        return null;
    }
    if (uri.startsWith("data:")) {
        const commaIndex = uri.indexOf(",");
        return Buffer.from(uri.slice(commaIndex + 1), "base64");
    }
    const imagePath = path.join(sourceDir, uri);
    if (fs.existsSync(imagePath)) {
        return fs.readFileSync(imagePath);
    }
    const texturePath = path.join(path.dirname(sourceDir), "textures", path.basename(uri));
    return fs.existsSync(texturePath) ? fs.readFileSync(texturePath) : null;
}

function readPngSize(bytes) {
    if (!bytes || bytes.length < 24 || bytes.toString("ascii", 1, 4) !== "PNG") {
        return {width: 16, height: 16};
    }
    return {
        width: bytes.readUInt32BE(16),
        height: bytes.readUInt32BE(20)
    };
}

function buildTextureAtlas() {
    if (!gltf.images || gltf.images.length <= 1) {
        return null;
    }

    const images = gltf.images.map((image) => readPngSize(getImageBytes(image.uri)));
    const cell = Math.max(...images.map((image) => Math.max(image.width, image.height)));
    const columns = Math.ceil(Math.sqrt(images.length));
    const rows = Math.ceil(images.length / columns);
    const width = nextPowerOfTwo(columns * cell);
    const height = nextPowerOfTwo(rows * cell);
    const entries = images.map((image, index) => ({
        x: index % columns * cell,
        y: Math.floor(index / columns) * cell,
        width: image.width,
        height: image.height
    }));
    return {cell, columns, rows, width, height, entries};
}

function getPrimitiveTextureIndex(primitive) {
    if (!textureAtlas || primitive.material === undefined) {
        return 0;
    }
    const material = gltf.materials && gltf.materials[primitive.material];
    const texture = material
        && material.pbrMetallicRoughness
        && material.pbrMetallicRoughness.baseColorTexture;
    if (!texture || texture.index === undefined || !gltf.textures || !gltf.textures[texture.index]) {
        return 0;
    }
    return gltf.textures[texture.index].source || 0;
}

function mapTextureUv(uv, imageIndex) {
    const u = uv[0];
    const v = 1 - uv[1];
    if (!textureAtlas) {
        return [round(u), round(v)];
    }

    const entry = textureAtlas.entries[imageIndex] || textureAtlas.entries[0];
    return [
        round((entry.x + u * entry.width) / textureAtlas.width),
        round((entry.y + v * entry.height) / textureAtlas.height)
    ];
}

function componentCount(type) {
    switch (type) {
        case "SCALAR":
            return 1;
        case "VEC2":
            return 2;
        case "VEC3":
            return 3;
        case "VEC4":
            return 4;
        case "MAT4":
            return 16;
        default:
            throw new Error(`Unsupported accessor type: ${type}`);
    }
}

function componentInfo(componentType) {
    switch (componentType) {
        case 5121:
            return {size: 1, read: (buffer, offset) => buffer.readUInt8(offset)};
        case 5123:
            return {size: 2, read: (buffer, offset) => buffer.readUInt16LE(offset)};
        case 5125:
            return {size: 4, read: (buffer, offset) => buffer.readUInt32LE(offset)};
        case 5126:
            return {size: 4, read: (buffer, offset) => buffer.readFloatLE(offset)};
        default:
            throw new Error(`Unsupported component type: ${componentType}`);
    }
}

function readAccessor(index) {
    if (accessors.has(index)) {
        return accessors.get(index);
    }

    const accessor = gltf.accessors[index];
    const view = gltf.bufferViews[accessor.bufferView];
    const info = componentInfo(accessor.componentType);
    const count = componentCount(accessor.type);
    const stride = view.byteStride || info.size * count;
    const baseOffset = (view.byteOffset || 0) + (accessor.byteOffset || 0);
    const values = [];

    for (let i = 0; i < accessor.count; i++) {
        const itemOffset = baseOffset + i * stride;
        if (count === 1) {
            values.push(info.read(binary, itemOffset));
            continue;
        }

        const entry = [];
        for (let c = 0; c < count; c++) {
            entry.push(info.read(binary, itemOffset + c * info.size));
        }
        values.push(entry);
    }

    accessors.set(index, values);
    return values;
}

function round(value) {
    return Number(value.toFixed(6));
}

function identityMatrix() {
    return [
        1, 0, 0, 0,
        0, 1, 0, 0,
        0, 0, 1, 0,
        0, 0, 0, 1
    ];
}

function multiplyMatrices(a, b) {
    const out = new Array(16).fill(0);
    for (let column = 0; column < 4; column++) {
        for (let row = 0; row < 4; row++) {
            out[column * 4 + row] =
                a[row] * b[column * 4] +
                a[4 + row] * b[column * 4 + 1] +
                a[8 + row] * b[column * 4 + 2] +
                a[12 + row] * b[column * 4 + 3];
        }
    }
    return out;
}

function transformPoint(matrix, point) {
    const x = point[0];
    const y = point[1];
    const z = point[2];
    return [
        matrix[0] * x + matrix[4] * y + matrix[8] * z + matrix[12],
        matrix[1] * x + matrix[5] * y + matrix[9] * z + matrix[13],
        matrix[2] * x + matrix[6] * y + matrix[10] * z + matrix[14]
    ];
}

function addWeightedPoint(result, point, weight) {
    result[0] += point[0] * weight;
    result[1] += point[1] * weight;
    result[2] += point[2] * weight;
}

function inverseMatrix(m) {
    const out = [];
    out[0] = m[5] * m[10] * m[15] - m[5] * m[11] * m[14] - m[9] * m[6] * m[15] + m[9] * m[7] * m[14] + m[13] * m[6] * m[11] - m[13] * m[7] * m[10];
    out[4] = -m[4] * m[10] * m[15] + m[4] * m[11] * m[14] + m[8] * m[6] * m[15] - m[8] * m[7] * m[14] - m[12] * m[6] * m[11] + m[12] * m[7] * m[10];
    out[8] = m[4] * m[9] * m[15] - m[4] * m[11] * m[13] - m[8] * m[5] * m[15] + m[8] * m[7] * m[13] + m[12] * m[5] * m[11] - m[12] * m[7] * m[9];
    out[12] = -m[4] * m[9] * m[14] + m[4] * m[10] * m[13] + m[8] * m[5] * m[14] - m[8] * m[6] * m[13] - m[12] * m[5] * m[10] + m[12] * m[6] * m[9];
    out[1] = -m[1] * m[10] * m[15] + m[1] * m[11] * m[14] + m[9] * m[2] * m[15] - m[9] * m[3] * m[14] - m[13] * m[2] * m[11] + m[13] * m[3] * m[10];
    out[5] = m[0] * m[10] * m[15] - m[0] * m[11] * m[14] - m[8] * m[2] * m[15] + m[8] * m[3] * m[14] + m[12] * m[2] * m[11] - m[12] * m[3] * m[10];
    out[9] = -m[0] * m[9] * m[15] + m[0] * m[11] * m[13] + m[8] * m[1] * m[15] - m[8] * m[3] * m[13] - m[12] * m[1] * m[11] + m[12] * m[3] * m[9];
    out[13] = m[0] * m[9] * m[14] - m[0] * m[10] * m[13] - m[8] * m[1] * m[14] + m[8] * m[2] * m[13] + m[12] * m[1] * m[10] - m[12] * m[2] * m[9];
    out[2] = m[1] * m[6] * m[15] - m[1] * m[7] * m[14] - m[5] * m[2] * m[15] + m[5] * m[3] * m[14] + m[13] * m[2] * m[7] - m[13] * m[3] * m[6];
    out[6] = -m[0] * m[6] * m[15] + m[0] * m[7] * m[14] + m[4] * m[2] * m[15] - m[4] * m[3] * m[14] - m[12] * m[2] * m[7] + m[12] * m[3] * m[6];
    out[10] = m[0] * m[5] * m[15] - m[0] * m[7] * m[13] - m[4] * m[1] * m[15] + m[4] * m[3] * m[13] + m[12] * m[1] * m[7] - m[12] * m[3] * m[5];
    out[14] = -m[0] * m[5] * m[14] + m[0] * m[6] * m[13] + m[4] * m[1] * m[14] - m[4] * m[2] * m[13] - m[12] * m[1] * m[6] + m[12] * m[2] * m[5];
    out[3] = -m[1] * m[6] * m[11] + m[1] * m[7] * m[10] + m[5] * m[2] * m[11] - m[5] * m[3] * m[10] - m[9] * m[2] * m[7] + m[9] * m[3] * m[6];
    out[7] = m[0] * m[6] * m[11] - m[0] * m[7] * m[10] - m[4] * m[2] * m[11] + m[4] * m[3] * m[10] + m[8] * m[2] * m[7] - m[8] * m[3] * m[6];
    out[11] = -m[0] * m[5] * m[11] + m[0] * m[7] * m[9] + m[4] * m[1] * m[11] - m[4] * m[3] * m[9] - m[8] * m[1] * m[7] + m[8] * m[3] * m[5];
    out[15] = m[0] * m[5] * m[10] - m[0] * m[6] * m[9] - m[4] * m[1] * m[10] + m[4] * m[2] * m[9] + m[8] * m[1] * m[6] - m[8] * m[2] * m[5];

    const determinant = m[0] * out[0] + m[1] * out[4] + m[2] * out[8] + m[3] * out[12];
    if (Math.abs(determinant) < 1e-8) {
        return identityMatrix();
    }

    const invDet = 1 / determinant;
    for (let i = 0; i < 16; i++) {
        out[i] *= invDet;
    }
    return out;
}

function insetUv(uv, center, epsilon) {
    if (Math.abs(uv - center) < 1e-6) {
        return uv;
    }
    return uv < center ? Math.min(uv + epsilon, center) : Math.max(uv - epsilon, center);
}

function matrixToTrs(matrix) {
    const translation = [matrix[12], matrix[13], matrix[14]];
    const scaleX = Math.hypot(matrix[0], matrix[1], matrix[2]);
    const scaleY = Math.hypot(matrix[4], matrix[5], matrix[6]);
    const scaleZ = Math.hypot(matrix[8], matrix[9], matrix[10]);
    const sx = scaleX || 1;
    const sy = scaleY || 1;
    const sz = scaleZ || 1;

    const m00 = matrix[0] / sx;
    const m01 = matrix[4] / sy;
    const m02 = matrix[8] / sz;
    const m10 = matrix[1] / sx;
    const m11 = matrix[5] / sy;
    const m12 = matrix[9] / sz;
    const m20 = matrix[2] / sx;
    const m21 = matrix[6] / sy;
    const m22 = matrix[10] / sz;

    const trace = m00 + m11 + m22;
    let x;
    let y;
    let z;
    let w;

    if (trace > 0) {
        const s = Math.sqrt(trace + 1.0) * 2;
        w = 0.25 * s;
        x = (m21 - m12) / s;
        y = (m02 - m20) / s;
        z = (m10 - m01) / s;
    } else if (m00 > m11 && m00 > m22) {
        const s = Math.sqrt(1.0 + m00 - m11 - m22) * 2;
        w = (m21 - m12) / s;
        x = 0.25 * s;
        y = (m01 + m10) / s;
        z = (m02 + m20) / s;
    } else if (m11 > m22) {
        const s = Math.sqrt(1.0 + m11 - m00 - m22) * 2;
        w = (m02 - m20) / s;
        x = (m01 + m10) / s;
        y = 0.25 * s;
        z = (m12 + m21) / s;
    } else {
        const s = Math.sqrt(1.0 + m22 - m00 - m11) * 2;
        w = (m10 - m01) / s;
        x = (m02 + m20) / s;
        y = (m12 + m21) / s;
        z = 0.25 * s;
    }

    return {
        translation: translation.map(round),
        rotation: [x, y, z, w].map(round),
        scale: [sx, sy, sz].map(round)
    };
}

function readNodeTrs(node) {
    if (Array.isArray(node.matrix) && node.matrix.length === 16) {
        return matrixToTrs(node.matrix);
    }
    return {
        translation: (node.translation || [0, 0, 0]).map(round),
        rotation: (node.rotation || [0, 0, 0, 1]).map(round),
        scale: (node.scale || [1, 1, 1]).map(round)
    };
}

function composeNodeMatrix(node) {
    if (Array.isArray(node.matrix) && node.matrix.length === 16) {
        return node.matrix.slice();
    }

    const translation = node.translation || [0, 0, 0];
    const rotation = node.rotation || [0, 0, 0, 1];
    const scale = node.scale || [1, 1, 1];
    const [x, y, z, w] = rotation;
    const x2 = x + x;
    const y2 = y + y;
    const z2 = z + z;
    const xx = x * x2;
    const xy = x * y2;
    const xz = x * z2;
    const yy = y * y2;
    const yz = y * z2;
    const zz = z * z2;
    const wx = w * x2;
    const wy = w * y2;
    const wz = w * z2;

    return [
        (1 - (yy + zz)) * scale[0],
        (xy + wz) * scale[0],
        (xz - wy) * scale[0],
        0,
        (xy - wz) * scale[1],
        (1 - (xx + zz)) * scale[1],
        (yz + wx) * scale[1],
        0,
        (xz + wy) * scale[2],
        (yz - wx) * scale[2],
        (1 - (xx + yy)) * scale[2],
        0,
        translation[0],
        translation[1],
        translation[2],
        1
    ];
}

const worldMatrixCache = new Map();

function getNodeWorldMatrix(nodeIndex) {
    if (worldMatrixCache.has(nodeIndex)) {
        return worldMatrixCache.get(nodeIndex);
    }

    const parentIndex = findParentNode(nodeIndex);
    const parentMatrix = parentIndex === -1 ? identityMatrix() : getNodeWorldMatrix(parentIndex);
    const matrix = multiplyMatrices(parentMatrix, composeNodeMatrix(gltf.nodes[nodeIndex]));
    worldMatrixCache.set(nodeIndex, matrix);
    return matrix;
}

function findParentNode(childIndex) {
    for (let i = 0; i < gltf.nodes.length; i++) {
        const children = gltf.nodes[i].children;
        if (Array.isArray(children) && children.includes(childIndex)) {
            return i;
        }
    }
    return -1;
}

function skinVertex(position, primitive, vertexIndex, nodeIndex) {
    const node = gltf.nodes[nodeIndex];
    if (!node || node.skin === undefined || !gltf.skins || !gltf.skins[node.skin]) {
        return position;
    }
    if (primitive.attributes.JOINTS_0 === undefined || primitive.attributes.WEIGHTS_0 === undefined) {
        return position;
    }

    const skin = gltf.skins[node.skin];
    const joints = readAccessor(primitive.attributes.JOINTS_0)[vertexIndex];
    const weights = readAccessor(primitive.attributes.WEIGHTS_0)[vertexIndex];
    const inverseBindMatrices = skin.inverseBindMatrices !== undefined
        ? readAccessor(skin.inverseBindMatrices)
        : [];
    const meshInverseWorld = inverseMatrix(getNodeWorldMatrix(nodeIndex));
    const result = [0, 0, 0];
    let weightSum = 0;

    for (let i = 0; i < 4; i++) {
        const weight = weights[i] || 0;
        if (weight <= 0) {
            continue;
        }

        const jointNodeIndex = skin.joints[joints[i]];
        if (jointNodeIndex === undefined) {
            continue;
        }

        const inverseBind = inverseBindMatrices[joints[i]] || identityMatrix();
        const skinMatrix = multiplyMatrices(
            meshInverseWorld,
            multiplyMatrices(getNodeWorldMatrix(jointNodeIndex), inverseBind)
        );
        addWeightedPoint(result, transformPoint(skinMatrix, position), weight);
        weightSum += weight;
    }

    return weightSum > 0 ? result.map((value) => value / weightSum) : position;
}

function buildMeshTriangles(meshIndex, nodeIndex = -1) {
    const mesh = gltf.meshes[meshIndex];
    const triangles = [];
    if (!mesh || !Array.isArray(mesh.primitives)) {
        return triangles;
    }

    for (const primitive of mesh.primitives) {
        const textureIndex = getPrimitiveTextureIndex(primitive);
        const positions = readAccessor(primitive.attributes.POSITION);
        const texCoords = primitive.attributes.TEXCOORD_0 !== undefined
            ? readAccessor(primitive.attributes.TEXCOORD_0)
            : null;
        const indices = primitive.indices !== undefined
            ? readAccessor(primitive.indices)
            : positions.map((_, index) => index);

        for (let i = 0; i + 2 < indices.length; i += 3) {
            const triangle = [];
            for (let j = 0; j < 3; j++) {
                const vertexIndex = indices[i + j];
                const position = nodeIndex >= 0
                    ? skinVertex(positions[vertexIndex], primitive, vertexIndex, nodeIndex)
                    : positions[vertexIndex];
                const uv = texCoords ? texCoords[vertexIndex] : [0, 0];
                const mappedUv = mapTextureUv(uv, textureIndex);
                triangle.push([
                    round(position[0]),
                    round(position[1]),
                    round(position[2]),
                    mappedUv[0],
                    mappedUv[1]
                ]);
            }

            const centerU = (triangle[0][3] + triangle[1][3] + triangle[2][3]) / 3;
            const centerV = (triangle[0][4] + triangle[1][4] + triangle[2][4]) / 3;
            for (const vertex of triangle) {
                vertex[3] = round(insetUv(vertex[3], centerU, UV_INSET_U));
                vertex[4] = round(insetUv(vertex[4], centerV, UV_INSET_V));
            }

            triangles.push(triangle);
        }
    }

    return triangles;
}

const nodes = gltf.nodes.map((node, index) => {
    const trs = readNodeTrs(node);
    return {
        name: node.name || "",
        translation: trs.translation,
        rotation: trs.rotation,
        scale: trs.scale,
        children: Array.isArray(node.children) ? node.children : [],
        triangles: node.mesh !== undefined ? buildMeshTriangles(node.mesh, index) : []
    };
});

const bounds = {
    min: [Infinity, Infinity, Infinity],
    max: [-Infinity, -Infinity, -Infinity]
};

function includePoint(point) {
    bounds.min[0] = Math.min(bounds.min[0], point[0]);
    bounds.min[1] = Math.min(bounds.min[1], point[1]);
    bounds.min[2] = Math.min(bounds.min[2], point[2]);
    bounds.max[0] = Math.max(bounds.max[0], point[0]);
    bounds.max[1] = Math.max(bounds.max[1], point[1]);
    bounds.max[2] = Math.max(bounds.max[2], point[2]);
}

function traverseWorld(nodeIndex, parentMatrix) {
    const node = gltf.nodes[nodeIndex];
    if (!node) {
        return;
    }

    const worldMatrix = multiplyMatrices(parentMatrix, composeNodeMatrix(node));
    if (node.mesh !== undefined) {
        for (const triangle of buildMeshTriangles(node.mesh, nodeIndex)) {
            includePoint(transformPoint(worldMatrix, triangle[0]));
            includePoint(transformPoint(worldMatrix, triangle[1]));
            includePoint(transformPoint(worldMatrix, triangle[2]));
        }
    }

    if (Array.isArray(node.children)) {
        for (const child of node.children) {
            traverseWorld(child, worldMatrix);
        }
    }
}

const scene = gltf.scenes[gltf.scene || 0];
for (const rootNode of scene.nodes || []) {
    traverseWorld(rootNode, identityMatrix());
}

function buildAnimations() {
    if (!Array.isArray(gltf.animations)) {
        return [];
    }

    return gltf.animations.map((animation) => {
        const channels = [];
        let duration = 0;

        for (const channel of animation.channels || []) {
            const sampler = animation.samplers && animation.samplers[channel.sampler];
            const target = channel.target || {};
            const node = gltf.nodes && gltf.nodes[target.node];
            if (!sampler || !node || (target.path !== "translation" && target.path !== "rotation")) {
                continue;
            }

            const times = readAccessor(sampler.input).map(round);
            const values = readAccessor(sampler.output).map((value) => {
                const array = Array.isArray(value) ? value : [value];
                return array.map(round);
            });
            if (!times.length || !values.length) {
                continue;
            }

            duration = Math.max(duration, times[times.length - 1]);
            channels.push({
                node: node.name || "",
                path: target.path,
                interpolation: sampler.interpolation || "LINEAR",
                times,
                values
            });
        }

        return {
            name: animation.name || "",
            duration: round(duration),
            channels
        };
    }).filter((animation) => animation.channels.length > 0);
}

const output = {
    texture: textureAtlas ? "atlas" : (gltf.images && gltf.images[0] && gltf.images[0].uri) || "",
    bounds: {
        min: bounds.min.map(round),
        max: bounds.max.map(round)
    },
    rootNodes: scene.nodes || [],
    nodes,
    animations: buildAnimations()
};

fs.mkdirSync(path.dirname(outputJson), {recursive: true});
fs.writeFileSync(outputJson, JSON.stringify(output));
console.log(`Wrote ${nodes.length} nodes to ${outputJson}`);
