package net.minecraft.client.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import org.lwjgl.opengl.GL11;
import xd.harm.modules.impl.misc.Visuality;
import xd.harm.utils.render.FancyBlockParticleRenderHelper;

public class FancyDiggingParticle extends DiggingParticle {
    private static final IParticleRenderType FANCY_TERRAIN_SHEET = new IParticleRenderType() {
        @Override
        public void beginRender(BufferBuilder bufferBuilder, TextureManager textureManager) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            textureManager.bindTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE);
            bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        }

        @Override
        public void finishRender(Tessellator tessellator) {
            tessellator.draw();
            RenderSystem.disableBlend();
            RenderSystem.disableCull();
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        }

        @Override
        public String toString() {
            return "FANCY_TERRAIN_SHEET";
        }
    };

    private final BlockPos sourcePos;
    private final float baseScale;
    private float renderScale;
    private float prevRenderScale;
    private float prevAlpha;
    private float rotX;
    private float rotY;
    private float rotZ;
    private float prevRotX;
    private float prevRotY;
    private float prevRotZ;
    private float rotSpeedX;
    private float rotSpeedY;
    private float rotSpeedZ;

    public FancyDiggingParticle(ClientWorld world, double x, double y, double z, double motionX, double motionY, double motionZ, BlockState state, BlockPos pos, float scale) {
        super(world, x, y, z, motionX, motionY, motionZ, state);
        this.sourcePos = new BlockPos(pos.getX(), pos.getY(), pos.getZ());
        this.setBlockPos(this.sourcePos);
        this.particleGravity = 1.0F;

        this.baseScale = Math.max(0.035F, this.particleScale * scale);
        this.renderScale = this.prevRenderScale = this.baseScale;
        this.prevAlpha = this.particleAlpha;
        this.setBoundingSize(this.renderScale);
        this.maxAge = Math.max(18, 18 + this.rand.nextInt(16));

        this.rotX = this.prevRotX = this.rand.nextFloat() * 360.0F;
        this.rotY = this.prevRotY = this.rand.nextFloat() * 360.0F;
        this.rotZ = this.prevRotZ = this.rand.nextFloat() * 360.0F;

        float spinBase = 4.0F + this.rand.nextFloat() * 8.0F;
        this.rotSpeedX = (this.rand.nextFloat() - 0.5F) * spinBase;
        this.rotSpeedY = (this.rand.nextFloat() - 0.5F) * spinBase * 1.4F;
        this.rotSpeedZ = (this.rand.nextFloat() - 0.5F) * spinBase;
    }

    @Override
    public Particle multiplyVelocity(float multiplier) {
        super.multiplyVelocity(multiplier);
        return this;
    }

    @Override
    public Particle multiplyParticleScaleBy(float scale) {
        this.renderScale *= scale;
        this.prevRenderScale *= scale;
        this.setBoundingSize(this.renderScale);
        return this;
    }

    @Override
    public void tick() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        this.prevRotX = this.rotX;
        this.prevRotY = this.rotY;
        this.prevRotZ = this.rotZ;
        this.prevRenderScale = this.renderScale;
        this.prevAlpha = this.particleAlpha;

        if (!Visuality.isFancyBlocksEnabled()) {
            this.setExpired();
            return;
        }

        if (this.age++ >= this.maxAge) {
            this.setExpired();
            return;
        }

        float life = (float) this.age / (float) this.maxAge;

        this.motionY -= 0.04D * (double) this.particleGravity;
        this.move(this.motionX, this.motionY, this.motionZ);

        this.motionX *= 0.98D;
        this.motionY *= 0.98D;
        this.motionZ *= 0.98D;

        if (this.onGround) {
            this.motionX *= 0.72D;
            this.motionZ *= 0.72D;
            this.motionY *= 0.68D;
            this.rotSpeedX *= 0.90F;
            this.rotSpeedY *= 0.90F;
            this.rotSpeedZ *= 0.90F;
        } else {
            this.rotSpeedX *= 0.992F;
            this.rotSpeedY *= 0.992F;
            this.rotSpeedZ *= 0.992F;
        }

        double horizontalSpeed = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
        this.rotX += this.rotSpeedX + (float) horizontalSpeed * 32.0F;
        this.rotY += this.rotSpeedY + (float) horizontalSpeed * 24.0F;
        this.rotZ += this.rotSpeedZ + (float) horizontalSpeed * 32.0F;

        if (life > 0.72F) {
            float fade = MathHelper.clamp((1.0F - life) / 0.28F, 0.0F, 1.0F);
            this.particleAlpha = fade;
            this.renderScale = Math.max(0.012F, this.baseScale * (0.45F + fade * 0.55F));
        } else {
            this.particleAlpha = 1.0F;
            this.renderScale = this.baseScale;
        }

        this.setBoundingSize(this.renderScale);
    }

    @Override
    public IParticleRenderType getRenderType() {
        return FANCY_TERRAIN_SHEET;
    }

    @Override
    public int getBrightnessForRender(float partialTick) {
        return super.getBrightnessForRender(partialTick);
    }

    @Override
    public void renderParticle(IVertexBuilder buffer, ActiveRenderInfo renderInfo, float partialTicks) {
        Vector3d camera = renderInfo.getProjectedView();
        double x = MathHelper.lerp((double) partialTicks, this.prevPosX, this.posX) - camera.x;
        double y = MathHelper.lerp((double) partialTicks, this.prevPosY, this.posY) - camera.y;
        double z = MathHelper.lerp((double) partialTicks, this.prevPosZ, this.posZ) - camera.z;

        float scale = MathHelper.lerp(partialTicks, this.prevRenderScale, this.renderScale);
        float alpha = MathHelper.lerp(partialTicks, this.prevAlpha, this.particleAlpha);
        float smoothRotX = MathHelper.lerp(partialTicks, this.prevRotX, this.rotX);
        float smoothRotY = MathHelper.lerp(partialTicks, this.prevRotY, this.rotY);
        float smoothRotZ = MathHelper.lerp(partialTicks, this.prevRotZ, this.rotZ);

        FancyBlockParticleRenderHelper.renderCube(buffer, x, y + scale, z, scale, smoothRotX, smoothRotY, smoothRotZ, this.getMinU(), this.getMaxU(), this.getMinV(), this.getMaxV(), this.getBrightnessForRender(partialTicks), this.particleRed, this.particleGreen, this.particleBlue, alpha);
    }

    private void setBoundingSize(float scale) {
        float size = Math.max(0.02F, scale * 2.0F);
        this.setSize(size, size);
    }
}
