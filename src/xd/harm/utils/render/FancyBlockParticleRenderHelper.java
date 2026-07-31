package xd.harm.utils.render;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;

public final class FancyBlockParticleRenderHelper {
    private static final Vector3d[] CUBE = new Vector3d[]{
            new Vector3d(1.0D, 1.0D, -1.0D), new Vector3d(1.0D, 1.0D, 1.0D), new Vector3d(-1.0D, 1.0D, 1.0D), new Vector3d(-1.0D, 1.0D, -1.0D),
            new Vector3d(-1.0D, -1.0D, -1.0D), new Vector3d(-1.0D, -1.0D, 1.0D), new Vector3d(1.0D, -1.0D, 1.0D), new Vector3d(1.0D, -1.0D, -1.0D),
            new Vector3d(-1.0D, -1.0D, 1.0D), new Vector3d(-1.0D, 1.0D, 1.0D), new Vector3d(1.0D, 1.0D, 1.0D), new Vector3d(1.0D, -1.0D, 1.0D),
            new Vector3d(1.0D, -1.0D, -1.0D), new Vector3d(1.0D, 1.0D, -1.0D), new Vector3d(-1.0D, 1.0D, -1.0D), new Vector3d(-1.0D, -1.0D, -1.0D),
            new Vector3d(-1.0D, -1.0D, -1.0D), new Vector3d(-1.0D, 1.0D, -1.0D), new Vector3d(-1.0D, 1.0D, 1.0D), new Vector3d(-1.0D, -1.0D, 1.0D),
            new Vector3d(1.0D, -1.0D, 1.0D), new Vector3d(1.0D, 1.0D, 1.0D), new Vector3d(1.0D, 1.0D, -1.0D), new Vector3d(1.0D, -1.0D, -1.0D)
    };

    private static final Vector3d[] CUBE_NORMALS = new Vector3d[]{
            new Vector3d(0.0D, 1.0D, 0.0D), new Vector3d(0.0D, -1.0D, 0.0D),
            new Vector3d(0.0D, 0.0D, 1.0D), new Vector3d(0.0D, 0.0D, -1.0D),
            new Vector3d(-1.0D, 0.0D, 0.0D), new Vector3d(1.0D, 0.0D, 0.0D)
    };

    private FancyBlockParticleRenderHelper() {
    }

    public static void renderCube(IVertexBuilder builder, double x, double y, double z, float scale, float rotX, float rotY, float rotZ, float minU, float maxU, float minV, float maxV, int light, float red, float green, float blue, float alpha) {
        float radX = (float) Math.toRadians(rotX);
        float radY = (float) Math.toRadians(rotY);
        float radZ = (float) Math.toRadians(rotZ);
        float[][] uv = new float[][]{
                {maxU, maxV},
                {maxU, minV},
                {minU, minV},
                {minU, maxV}
        };

        for (int i = 0; i < CUBE.length; i += 4) {
            Vector3d v1 = rotate(CUBE[i], radX, radY, radZ).scale(scale).add(x, y, z);
            Vector3d v2 = rotate(CUBE[i + 1], radX, radY, radZ).scale(scale).add(x, y, z);
            Vector3d v3 = rotate(CUBE[i + 2], radX, radY, radZ).scale(scale).add(x, y, z);
            Vector3d v4 = rotate(CUBE[i + 3], radX, radY, radZ).scale(scale).add(x, y, z);

            Vector3d normal = rotate(CUBE_NORMALS[i / 4], radX, radY, radZ);
            float shade = MathHelper.clamp(0.56F + (float) Math.max(0.0D, normal.y) * 0.24F + (float) Math.abs(normal.x) * 0.10F + (float) Math.abs(normal.z) * 0.10F, 0.18F, 1.0F);

            addVertex(builder, v1, uv[0][0], uv[0][1], light, red * shade, green * shade, blue * shade, alpha, normal);
            addVertex(builder, v2, uv[1][0], uv[1][1], light, red * shade, green * shade, blue * shade, alpha, normal);
            addVertex(builder, v3, uv[2][0], uv[2][1], light, red * shade, green * shade, blue * shade, alpha, normal);
            addVertex(builder, v4, uv[3][0], uv[3][1], light, red * shade, green * shade, blue * shade, alpha, normal);
        }
    }

    private static void addVertex(IVertexBuilder builder, Vector3d position, float u, float v, int light, float red, float green, float blue, float alpha, Vector3d normal) {
        builder.pos(position.x, position.y, position.z)
                .color(red, green, blue, alpha)
                .tex(u, v)
                .overlay(OverlayTexture.NO_OVERLAY)
                .lightmap(light)
                .normal((float) normal.x, (float) normal.y, (float) normal.z)
                .endVertex();
    }

    private static Vector3d rotate(Vector3d vector, float angleX, float angleY, float angleZ) {
        Vector3d sin = new Vector3d(MathHelper.sin(angleX), MathHelper.sin(angleY), MathHelper.sin(angleZ));
        Vector3d cos = new Vector3d(MathHelper.cos(angleX), MathHelper.cos(angleY), MathHelper.cos(angleZ));

        Vector3d rotated = new Vector3d(vector.x, vector.y * cos.x - vector.z * sin.x, vector.y * sin.x + vector.z * cos.x);
        rotated = new Vector3d(rotated.x * cos.z - rotated.y * sin.z, rotated.x * sin.z + rotated.y * cos.z, rotated.z);
        return new Vector3d(rotated.x * cos.y + rotated.z * sin.y, rotated.y, rotated.x * sin.y - rotated.z * cos.y);
    }
}
