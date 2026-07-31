package xd.harm.utils.voronoi;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import xd.harm.utils.render.color.ColorUtils;

import java.util.ArrayList;
import java.util.List;

public class UVoronoiIntegration {
    private VoronoiOfQuad voronoi;
    private net.minecraft.util.math.vector.Matrix4f matrix;

    public static UVoronoiIntegration generateDefault(int pointsCount, boolean createInitThread) {
        return new UVoronoiIntegration(new VoronoiOfQuad(-0.5f, -0.5f, 0.5f, 0.5f, pointsCount, createInitThread));
    }

    public static UVoronoiIntegration generateDefault(int pointsCount, float scale, boolean createInitThread) {
        float ext = 0.5f * scale;
        return new UVoronoiIntegration(new VoronoiOfQuad(-ext, -ext, ext, ext, pointsCount, createInitThread));
    }

    public UVoronoiIntegration(VoronoiOfQuad voronoi) {
        this.voronoi = voronoi;
    }

    public UVoronoiIntegration setVoronoi(VoronoiOfQuad voronoi) {
        this.voronoi = voronoi;
        return this;
    }

    public UVoronoiIntegration setMatrix(net.minecraft.util.math.vector.Matrix4f matrix) {
        this.matrix = matrix;
        return this;
    }

    public List<VoronoiOfQuad.Polygon> getSpreadPolygons(boolean asCopied, float mulTrans, float mulRotate) {
        if (voronoi == null) {
            return new ArrayList<>();
        }
        List<VoronoiOfQuad.Polygon> base = snapshotPolygons();
        return getSpreadPolygons(base, asCopied, mulTrans, mulRotate);
    }

    private List<VoronoiOfQuad.Polygon> snapshotPolygons() {
        List<VoronoiOfQuad.Polygon> base = new ArrayList<>();
        for (VoronoiOfQuad.Polygon polygon : voronoi.getPolygons()) {
            base.add(polygon);
        }
        return base;
    }

    private List<VoronoiOfQuad.Polygon> getSpreadPolygons(List<VoronoiOfQuad.Polygon> base, boolean asCopied, float mulTrans, float mulRotate) {
        List<VoronoiOfQuad.Polygon> polygons;
        if (asCopied) {
            polygons = new ArrayList<>(base.size());
            for (VoronoiOfQuad.Polygon polygon : base) {
                polygons.add(polygon.copy());
            }
        } else {
            polygons = base;
        }

        if (mulTrans == 0.0f && mulRotate == 0.0f) {
            return polygons;
        }

        List<VoronoiOfQuad.Polygon> result = new ArrayList<>(polygons.size());
        for (VoronoiOfQuad.Polygon poly : polygons) {
            VoronoiOfQuad.Polygon updated = poly;
            if (mulTrans != 0.0f) {
                updated = updated.translateAwayPosLoc(voronoi.cx, voronoi.cy,
                        updated.distanceToAtCenter(voronoi.cx, voronoi.cy) * mulTrans);
            }
            if (mulRotate != 0.0f) {
                updated = updated.rotateAtYOfAngleAwayPos(voronoi.cx, voronoi.cy,
                        updated.distanceToAtCenter(voronoi.cx, voronoi.cy) * mulRotate);
            }
            result.add(updated);
        }
        return result;
    }

    public void renderBindTextureSegments(boolean temporalMode, int begin, float trans, float rot, float aPC, int color, int tryDraws) {
        if (voronoi == null || tryDraws <= 0) {
            return;
        }
        color = ColorUtils.swapAlpha(color, ColorUtils.getAlphaFromColor(color) * aPC);
        List<VoronoiOfQuad.Polygon> basePolygons = snapshotPolygons();
        if (basePolygons.isEmpty()) {
            return;
        }
        List<VoronoiOfQuad.Polygon> polygonsToDraw = getSpreadPolygons(basePolygons, temporalMode, trans, rot);
        if (polygonsToDraw.isEmpty()) {
            return;
        }

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        int indexPoly = 0;

        for (VoronoiOfQuad.Polygon renderPolygon : polygonsToDraw) {
            if (indexPoly >= basePolygons.size()) {
                break;
            }
            VoronoiOfQuad.Polygon staticPolygon = basePolygons.get(indexPoly);
            if (staticPolygon == null) {
                indexPoly++;
                continue;
            }

            List<VoronoiOfQuad.Vec2f> staticVecList = staticPolygon.getAllVertices();
            List<VoronoiOfQuad.Vec2f> dynamicVecList = renderPolygon.getAllVertices();
            List<Vec2fUVC> verticesTexCol = new ArrayList<>();

            int vertIndex = 0;
            for (VoronoiOfQuad.Vec2f posVec : dynamicVecList) {
                if (vertIndex >= staticVecList.size()) {
                    break;
                }
                VoronoiOfQuad.Vec2f texVec = staticVecList.get(vertIndex);
                verticesTexCol.add(Vec2fUVC.asVecs2f(texVec, posVec, renderPolygon.uniqueInt,
                        voronoi.x, voronoi.y, voronoi.x2, voronoi.y2));
                vertIndex++;
            }

            if (!verticesTexCol.isEmpty()) {
                buffer.begin(begin, DefaultVertexFormats.POSITION_TEX_COLOR);
                for (Vec2fUVC bufferVec : verticesTexCol) {
                    bufferVec.doVertexTexCol(buffer, color, matrix);
                }
                prepareRenderStart2D();
                tessellator.draw();
                prepareRenderStop2D();
            }

            indexPoly++;
        }
    }

    public void renderBindTextureSegmentsRevUV(boolean temporalMode, int begin, float trans, float rot, float aPC, int color, int tryDraws) {
        if (voronoi == null || tryDraws <= 0) {
            return;
        }
        color = ColorUtils.swapAlpha(color, ColorUtils.getAlphaFromColor(color) * aPC);
        List<VoronoiOfQuad.Polygon> basePolygons = snapshotPolygons();
        if (basePolygons.isEmpty()) {
            return;
        }
        List<VoronoiOfQuad.Polygon> polygonsToDraw = getSpreadPolygons(basePolygons, temporalMode, trans, rot);
        if (polygonsToDraw.isEmpty()) {
            return;
        }

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        int indexPoly = 0;

        for (VoronoiOfQuad.Polygon renderPolygon : polygonsToDraw) {
            if (indexPoly >= basePolygons.size()) {
                break;
            }
            VoronoiOfQuad.Polygon staticPolygon = basePolygons.get(indexPoly);
            if (staticPolygon == null) {
                indexPoly++;
                continue;
            }

            List<VoronoiOfQuad.Vec2f> staticVecList = staticPolygon.getAllVertices();
            List<VoronoiOfQuad.Vec2f> dynamicVecList = renderPolygon.getAllVertices();
            List<Vec2fUVC> verticesTexCol = new ArrayList<>();

            int vertIndex = 0;
            for (VoronoiOfQuad.Vec2f posVec : dynamicVecList) {
                if (vertIndex >= staticVecList.size()) {
                    break;
                }
                VoronoiOfQuad.Vec2f texVec = staticVecList.get(vertIndex);
                verticesTexCol.add(Vec2fUVC.asVecs2f(texVec, posVec, renderPolygon.uniqueInt,
                        voronoi.x, voronoi.y, voronoi.x2, voronoi.y2));
                vertIndex++;
            }

            if (!verticesTexCol.isEmpty()) {
                buffer.begin(begin, DefaultVertexFormats.POSITION_TEX_COLOR);
                for (Vec2fUVC bufferVec : verticesTexCol) {
                    bufferVec.doVertexTexColRevUV(buffer, color, matrix);
                }
                prepareRenderStart2D();
                tessellator.draw();
                prepareRenderStop2D();
            }

            indexPoly++;
        }
    }

    public void renderBindTextureSegments(boolean temporalMode, int begin, float trans, float rot, float aPC,
                                          int color1, int color2, int color3, int color4, int tryDraws) {
        if (voronoi == null || tryDraws <= 0) {
            return;
        }
        List<VoronoiOfQuad.Polygon> basePolygons = snapshotPolygons();
        if (basePolygons.isEmpty()) {
            return;
        }
        List<VoronoiOfQuad.Polygon> polygonsToDraw = getSpreadPolygons(basePolygons, temporalMode, trans, rot);
        if (polygonsToDraw.isEmpty()) {
            return;
        }

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        int indexPoly = 0;

        for (VoronoiOfQuad.Polygon renderPolygon : polygonsToDraw) {
            if (indexPoly >= basePolygons.size()) {
                break;
            }
            VoronoiOfQuad.Polygon staticPolygon = basePolygons.get(indexPoly);
            if (staticPolygon == null) {
                indexPoly++;
                continue;
            }

            List<VoronoiOfQuad.Vec2f> staticVecList = staticPolygon.getAllVertices();
            List<VoronoiOfQuad.Vec2f> dynamicVecList = renderPolygon.getAllVertices();
            List<Vec2fUVC> verticesTexCol = new ArrayList<>();

            int vertIndex = 0;
            for (VoronoiOfQuad.Vec2f posVec : dynamicVecList) {
                if (vertIndex >= staticVecList.size()) {
                    break;
                }
                VoronoiOfQuad.Vec2f texVec = staticVecList.get(vertIndex);
                verticesTexCol.add(Vec2fUVC.asVecs2f(texVec, posVec, renderPolygon.uniqueInt,
                        voronoi.x, voronoi.y, voronoi.x2, voronoi.y2));
                vertIndex++;
            }

            if (!verticesTexCol.isEmpty()) {
                buffer.begin(begin, DefaultVertexFormats.POSITION_TEX_COLOR);
                for (Vec2fUVC bufferVec : verticesTexCol) {
                    bufferVec.doVertexTexCol(buffer, color1, color2, color3, color4, aPC, matrix);
                }
                prepareRenderStart2D();
                tessellator.draw();
                prepareRenderStop2D();
            }

            indexPoly++;
        }
    }

    public void renderBindTextureSegmentsRevUV(boolean temporalMode, int begin, float trans, float rot, float aPC,
                                               int color1, int color2, int color3, int color4, int tryDraws) {
        if (voronoi == null || tryDraws <= 0) {
            return;
        }
        List<VoronoiOfQuad.Polygon> basePolygons = snapshotPolygons();
        if (basePolygons.isEmpty()) {
            return;
        }
        List<VoronoiOfQuad.Polygon> polygonsToDraw = getSpreadPolygons(basePolygons, temporalMode, trans, rot);
        if (polygonsToDraw.isEmpty()) {
            return;
        }

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        int indexPoly = 0;

        for (VoronoiOfQuad.Polygon renderPolygon : polygonsToDraw) {
            if (indexPoly >= basePolygons.size()) {
                break;
            }
            VoronoiOfQuad.Polygon staticPolygon = basePolygons.get(indexPoly);
            if (staticPolygon == null) {
                indexPoly++;
                continue;
            }

            List<VoronoiOfQuad.Vec2f> staticVecList = staticPolygon.getAllVertices();
            List<VoronoiOfQuad.Vec2f> dynamicVecList = renderPolygon.getAllVertices();
            List<Vec2fUVC> verticesTexCol = new ArrayList<>();

            int vertIndex = 0;
            for (VoronoiOfQuad.Vec2f posVec : dynamicVecList) {
                if (vertIndex >= staticVecList.size()) {
                    break;
                }
                VoronoiOfQuad.Vec2f texVec = staticVecList.get(vertIndex);
                verticesTexCol.add(Vec2fUVC.asVecs2f(texVec, posVec, renderPolygon.uniqueInt,
                        voronoi.x, voronoi.y, voronoi.x2, voronoi.y2));
                vertIndex++;
            }

            if (!verticesTexCol.isEmpty()) {
                buffer.begin(begin, DefaultVertexFormats.POSITION_TEX_COLOR);
                for (Vec2fUVC bufferVec : verticesTexCol) {
                    bufferVec.doVertexTexColRevUV(buffer, color1, color2, color3, color4, aPC, matrix);
                }
                prepareRenderStart2D();
                tessellator.draw();
                prepareRenderStop2D();
            }

            indexPoly++;
        }
    }

    private void prepareRenderStart2D() {
    }

    private void prepareRenderStop2D() {
    }

    public static class Vec2fUVC {
        private final float x;
        private final float y;
        private final float u;
        private final float v;

        private Vec2fUVC(float x, float y, float u, float v) {
            this.x = x;
            this.y = y;
            this.u = u;
            this.v = v;
        }

        public static Vec2fUVC asVecs2f(VoronoiOfQuad.Vec2f texVec, VoronoiOfQuad.Vec2f posVec,
                                        int uniqueInt, float x, float y, float x2, float y2) {
            float width = Math.max(0.0001f, x2 - x);
            float height = Math.max(0.0001f, y2 - y);
            float u = (texVec.x - x) / width;
            float v = (texVec.y - y) / height;
            u = clamp01(u);
            v = clamp01(v);
            return new Vec2fUVC(posVec.x, posVec.y, u, v);
        }

        public void doVertexTexCol(BufferBuilder buffer, int color) {
            buffer.pos(x, y, 0.0f)
                    .tex(u, v)
                    .color(ColorUtils.getRed(color), ColorUtils.getGreen(color), ColorUtils.getBlue(color), ColorUtils.getAlpha(color))
                    .endVertex();
        }

        public void doVertexTexColRevUV(BufferBuilder buffer, int color) {
            buffer.pos(x, y, 0.0f)
                    .tex(u, 1.0f - v)
                    .color(ColorUtils.getRed(color), ColorUtils.getGreen(color), ColorUtils.getBlue(color), ColorUtils.getAlpha(color))
                    .endVertex();
        }

        public void doVertexTexCol(BufferBuilder buffer, int c1, int c2, int c3, int c4, float aPC) {
            int color = bilerp(c1, c2, c3, c4, u, v);
            int alpha = (int) (ColorUtils.getAlpha(color) * aPC);
            color = ColorUtils.setAlpha(color, alpha);
            buffer.pos(x, y, 0.0f)
                    .tex(u, v)
                    .color(ColorUtils.getRed(color), ColorUtils.getGreen(color), ColorUtils.getBlue(color), ColorUtils.getAlpha(color))
                    .endVertex();
        }

        public void doVertexTexColRevUV(BufferBuilder buffer, int c1, int c2, int c3, int c4, float aPC) {
            float vv = 1.0f - v;
            int color = bilerp(c1, c2, c3, c4, u, vv);
            int alpha = (int) (ColorUtils.getAlpha(color) * aPC);
            color = ColorUtils.setAlpha(color, alpha);
            buffer.pos(x, y, 0.0f)
                    .tex(u, vv)
                    .color(ColorUtils.getRed(color), ColorUtils.getGreen(color), ColorUtils.getBlue(color), ColorUtils.getAlpha(color))
                    .endVertex();
        }

        public void doVertexTexCol(BufferBuilder buffer, int color, net.minecraft.util.math.vector.Matrix4f matrix) {
            if (matrix != null) {
                buffer.pos(matrix, x, y, 0.0f)
                        .tex(u, v)
                        .color(ColorUtils.getRed(color), ColorUtils.getGreen(color), ColorUtils.getBlue(color), ColorUtils.getAlpha(color))
                        .endVertex();
            } else {
                doVertexTexCol(buffer, color);
            }
        }

        public void doVertexTexColRevUV(BufferBuilder buffer, int color, net.minecraft.util.math.vector.Matrix4f matrix) {
            if (matrix != null) {
                buffer.pos(matrix, x, y, 0.0f)
                        .tex(u, 1.0f - v)
                        .color(ColorUtils.getRed(color), ColorUtils.getGreen(color), ColorUtils.getBlue(color), ColorUtils.getAlpha(color))
                        .endVertex();
            } else {
                doVertexTexColRevUV(buffer, color);
            }
        }

        public void doVertexTexCol(BufferBuilder buffer, int c1, int c2, int c3, int c4, float aPC,
                                   net.minecraft.util.math.vector.Matrix4f matrix) {
            if (matrix != null) {
                int color = bilerp(c1, c2, c3, c4, u, v);
                int alpha = (int) (ColorUtils.getAlpha(color) * aPC);
                color = ColorUtils.setAlpha(color, alpha);
                buffer.pos(matrix, x, y, 0.0f)
                        .tex(u, v)
                        .color(ColorUtils.getRed(color), ColorUtils.getGreen(color), ColorUtils.getBlue(color), ColorUtils.getAlpha(color))
                        .endVertex();
            } else {
                doVertexTexCol(buffer, c1, c2, c3, c4, aPC);
            }
        }

        public void doVertexTexColRevUV(BufferBuilder buffer, int c1, int c2, int c3, int c4, float aPC,
                                        net.minecraft.util.math.vector.Matrix4f matrix) {
            if (matrix != null) {
                float vv = 1.0f - v;
                int color = bilerp(c1, c2, c3, c4, u, vv);
                int alpha = (int) (ColorUtils.getAlpha(color) * aPC);
                color = ColorUtils.setAlpha(color, alpha);
                buffer.pos(matrix, x, y, 0.0f)
                        .tex(u, vv)
                        .color(ColorUtils.getRed(color), ColorUtils.getGreen(color), ColorUtils.getBlue(color), ColorUtils.getAlpha(color))
                        .endVertex();
            } else {
                doVertexTexColRevUV(buffer, c1, c2, c3, c4, aPC);
            }
        }

        private static int bilerp(int c1, int c2, int c3, int c4, float u, float v) {
            // c1: top-left, c2: top-right, c3: bottom-right, c4: bottom-left
            int top = ColorUtils.interpolateColor(c1, c2, u);
            int bottom = ColorUtils.interpolateColor(c4, c3, u);
            return ColorUtils.interpolateColor(top, bottom, v);
        }

        private static float clamp01(float v) {
            if (v < 0.0f) {
                return 0.0f;
            }
            if (v > 1.0f) {
                return 1.0f;
            }
            return v;
        }
    }
}
