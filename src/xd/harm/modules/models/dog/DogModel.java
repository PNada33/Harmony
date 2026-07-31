package xd.harm.modules.models.dog;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.model.IHasArm;
import net.minecraft.client.renderer.entity.model.IHasHead;
import net.minecraft.client.renderer.model.Model;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.HandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3f;
import net.optifine.DynamicLights;
import xd.harm.events.render.EventRender3D;
import xd.harm.utils.client.IMinecraft;

import java.util.function.Function;

public class DogModel extends Model implements IHasArm, IHasHead, IMinecraft
{
    public ModelRenderer head;
    public ModelRenderer body;
    public ModelRenderer neck;
    public ModelRenderer chest;
    public ModelRenderer back;
    public ModelRenderer frontLeftLeg;
    public ModelRenderer frontRightLeg;
    public ModelRenderer leftBackLeg;
    public ModelRenderer rightBackLeg;
    public ModelRenderer tail;
    public ModelRenderer leftEar;
    public ModelRenderer rightEar;

    public DogModel(Function<ResourceLocation, RenderType> renderTypeIn)
    {
        super(renderTypeIn);
        this.textureWidth = 60;
        this.textureHeight = 36;


        this.head = new ModelRenderer(this);
        this.head.setRotationPoint(0.0F, 10.5F, -6.8F);
        this.head.setTextureOffset(0, 0).addBox(-3.0F, -3.0F, -4.0F, 6.0F, 6.0F, 4.0F, 0.0F);
        this.head.setTextureOffset(21, 0).addBox(-1.5F, 0.0F, -7.0F, 3.0F, 3.0F, 3.0F, 0.0F);


        this.leftEar = new ModelRenderer(this);
        this.leftEar.setRotationPoint(3.0F, 3.0F, -2.0F);
        this.leftEar.setTextureOffset(32, 4).addBox(0.0F, -5.0F, -1.5F, 1.0F, 3.0F, 3.0F, 0.0F);
        this.leftEar.setTextureOffset(34, 1).addBox(0.0F, -5.5F, -0.75F, 1.0F, 1.0F, 1.5F, 0.0F);
        this.head.addChild(this.leftEar);


        this.rightEar = new ModelRenderer(this);
        this.rightEar.setRotationPoint(-3.0F, 3.0F, -2.0F);
        this.rightEar.setTextureOffset(32, 4).addBox(-1.0F, -5.0F, -1.5F, 1.0F, 3.0F, 3.0F, 0.0F);
        this.rightEar.setTextureOffset(34, 1).addBox(-1.0F, -5.5F, -0.75F, 1.0F, 1.0F, 1.5F, 0.0F);
        this.head.addChild(this.rightEar);

        this.neck = new ModelRenderer(this);
        this.neck.setRotationPoint(0.0F, 10.5F, -5.0F);
        this.neck.rotateAngleX = -25.0F * (float)(Math.PI / 180.0);
        this.neck.setTextureOffset(15, 7).addBox(-2.95F, -1.0F, -4.0F, 5.9F, 5.0F, 6.0F, 0.0F);


        this.body = new ModelRenderer(this);
        this.body.setRotationPoint(0.0F, 13.5F, -5.0F);

        this.chest = new ModelRenderer(this);
        this.chest.setRotationPoint(0.0F, 0.0F, 3.0F);
        this.chest.setTextureOffset(32, 13).addBox(-4.0F, -3.5F, -3.0F, 8.0F, 7.0F, 6.0F, 0.0F);
        this.body.addChild(this.chest);

        this.back = new ModelRenderer(this);
        this.back.setRotationPoint(0.0F, -0.5F, 5.5F);
        this.back.setTextureOffset(3, 19).addBox(-3.0F, -3.0F, -0.5F, 6.0F, 6.0F, 11.0F, 0.0F);
        this.body.addChild(this.back);


        this.frontLeftLeg = new ModelRenderer(this);
        this.frontLeftLeg.setRotationPoint(1.5F, 16.0F, -3.0F);
        this.frontLeftLeg.setTextureOffset(42, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F, 0.0F);

        this.frontRightLeg = new ModelRenderer(this);
        this.frontRightLeg.setRotationPoint(-1.5F, 16.0F, -3.0F);
        this.frontRightLeg.setTextureOffset(42, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F, 0.0F);
        this.frontRightLeg.mirror = true;

        this.leftBackLeg = new ModelRenderer(this);
        this.leftBackLeg.setRotationPoint(1.5F, 16.0F, 9.0F);
        this.leftBackLeg.setTextureOffset(52, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F, 0.0F);

        this.rightBackLeg = new ModelRenderer(this);
        this.rightBackLeg.setRotationPoint(-1.5F, 16.0F, 9.0F);
        this.rightBackLeg.setTextureOffset(52, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F, 0.0F);
        this.rightBackLeg.mirror = true;

        this.tail = new ModelRenderer(this);
        this.tail.setRotationPoint(0.0F, 9.0F, 10.0F);
        this.tail.rotateAngleX = 22.5F * (float)(Math.PI / 180.0);
        this.tail.setTextureOffset(2, 12).addBox(-1.0F, 2.0F, -1.0F, 2.0F, 8.0F, 2.0F, 0.0F);
    }

    /**
     * Sets this entity's model rotation angles
     */
    public void setRotationAngles(float ageInTicks, DogBrain brain)
    {

        head.rotateAngleY = brain.getYaw() * ((float)Math.PI / 180F);
        head.rotateAngleX = brain.getPitch() * ((float)Math.PI / 180F);


        frontLeftLeg.rotateAngleX = (float)Math.cos(brain.limbSwing * 0.6662F) * 1.4F * brain.limbSwingAmount;
        frontRightLeg.rotateAngleX = (float)Math.cos(brain.limbSwing * 0.6662F + (float)Math.PI) * 1.4F * brain.limbSwingAmount;
        leftBackLeg.rotateAngleX = (float)Math.cos(brain.limbSwing * 0.6662F + (float)Math.PI) * 1.4F * brain.limbSwingAmount;
        rightBackLeg.rotateAngleX = (float)Math.cos(brain.limbSwing * 0.6662F) * 1.4F * brain.limbSwingAmount;

        if (brain.isLay()) {
            frontLeftLeg.rotateAngleX = (float) Math.toRadians(-90);
            frontRightLeg.rotateAngleX = (float) Math.toRadians(-90);
            leftBackLeg.rotateAngleX = (float) Math.toRadians(90);
            rightBackLeg.rotateAngleX = (float) Math.toRadians(90);

            frontLeftLeg.rotateAngleY = (float) Math.toRadians(-22);
            frontRightLeg.rotateAngleY = (float) Math.toRadians(22);
            leftBackLeg.rotateAngleY = (float) Math.toRadians(22);
            rightBackLeg.rotateAngleY = (float) Math.toRadians(-22);
        } else {
            frontLeftLeg.rotateAngleY = (float) Math.toRadians(0);
            frontRightLeg.rotateAngleY = (float) Math.toRadians(0);
            leftBackLeg.rotateAngleY = (float) Math.toRadians(0);
            rightBackLeg.rotateAngleY = (float) Math.toRadians(0);
        }

        tail.rotateAngleX = (float) Math.toRadians(brain.isLay() ? 45 : 22);


        tail.rotateAngleZ = (float) (Math.toRadians(-22.5F) + 22.5F * (float)(Math.PI / 180.0) + (float)Math.cos(ageInTicks * 0.15F) * 0.3F);
    }

    @Override
    public ModelRenderer getModelHead() {
        return head;
    }

    @Override
    public void translateHand(HandSide sideIn, MatrixStack matrixStackIn) {

    }

    public void render(MatrixStack matrixStackIn, EventRender3D e, DogBrain brain) {
        IVertexBuilder bufferIn = e.getVertex().getBuffer(RenderType.getEntityTranslucent(new ResourceLocation("minecraft", "harmony/images/pet/dog.png")));
        int packedLightIn = DynamicLights.getCombinedLight(new BlockPos(brain.getPos()), 999);
        int packedOverlayIn = OverlayTexture.NO_OVERLAY;

        matrixStackIn.push();
        matrixStackIn.translate(0.0, 1.2f - (brain.isLay() ? 0.3f : 0), 0.0);
        matrixStackIn.rotate(Vector3f.XP.rotationDegrees(180.0F));
        matrixStackIn.rotate(Vector3f.YP.rotationDegrees(brain.getBody()));
        this.head.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn);
        this.neck.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn);
        this.body.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn);
        this.frontLeftLeg.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn);
        this.frontRightLeg.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn);
        this.leftBackLeg.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn);
        this.rightBackLeg.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn);
        this.tail.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn);
        matrixStackIn.pop();
    }

    @Override
    public void render(MatrixStack matrixStackIn, IVertexBuilder bufferIn, int packedLightIn, int packedOverlayIn, float red, float green, float blue, float alpha) {

    }


}

