package net.minecraft.client.renderer.entity.layers;

import xd.harm.Harmony;
import xd.harm.modules.api.ModuleManager;
import xd.harm.utils.figura.CosmeticFeatures;
import xd.harm.modules.impl.render.Theme;
import xd.harm.utils.render.color.ColorUtils;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.block.AbstractSkullBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.IEntityRenderer;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.entity.model.IHasHead;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.SkullTileEntityRenderer;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.monster.ZombieVillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.SkullTileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.opengl.GL11;

import java.util.UUID;

import static com.mojang.blaze3d.systems.RenderSystem.*;
import static net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_COLOR;
import static net.optifine.util.MathUtils.PI2;
import static org.lwjgl.opengl.GL11.*;

public class HeadLayer<T extends LivingEntity, M extends EntityModel<T> & IHasHead> extends LayerRenderer<T, M>
{
    private final float field_239402_a_;
    private final float field_239403_b_;
    private final float field_239404_c_;
    private static final int SEGMENTS = 360;
    private static final float ROTATION_SPEED = 15.0f;
    private static final float WAVE_AMPLITUDE = 0.02f;
    private static final float WAVE_SPEED = 0.5f;
    private static final float PULSE_SPEED = 0.3f;

    public HeadLayer(IEntityRenderer<T, M> p_i50946_1_)
    {
        this(p_i50946_1_, 1.0F, 1.0F, 1.0F);
    }

    public HeadLayer(IEntityRenderer<T, M> p_i232475_1_, float p_i232475_2_, float p_i232475_3_, float p_i232475_4_)
    {
        super(p_i232475_1_);
        this.field_239402_a_ = p_i232475_2_;
        this.field_239403_b_ = p_i232475_3_;
        this.field_239404_c_ = p_i232475_4_;
    }

    public void render(MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn, T entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch)
    {
        ItemStack itemstack = entitylivingbaseIn.getItemStackFromSlot(EquipmentSlotType.HEAD);

        if (!itemstack.isEmpty())
        {
            Item item = itemstack.getItem();
            matrixStackIn.push();
            matrixStackIn.scale(this.field_239402_a_, this.field_239403_b_, this.field_239404_c_);
            boolean flag = entitylivingbaseIn instanceof VillagerEntity || entitylivingbaseIn instanceof ZombieVillagerEntity;

            if (entitylivingbaseIn.isChild() && !(entitylivingbaseIn instanceof VillagerEntity))
            {
                float f = 2.0F;
                float f1 = 1.4F;
                matrixStackIn.translate(0.0D, 0.03125D, 0.0D);
                matrixStackIn.scale(0.7F, 0.7F, 0.7F);
                matrixStackIn.translate(0.0D, 1.0D, 0.0D);
            }

            this.getEntityModel().getModelHead().translateRotate(matrixStackIn);

            if (item instanceof BlockItem && ((BlockItem)item).getBlock() instanceof AbstractSkullBlock)
            {
                float f3 = 1.1875F;
                matrixStackIn.scale(1.1875F, -1.1875F, -1.1875F);

                if (flag)
                {
                    matrixStackIn.translate(0.0D, 0.0625D, 0.0D);
                }

                GameProfile gameprofile = null;

                if (itemstack.hasTag())
                {
                    CompoundNBT compoundnbt = itemstack.getTag();

                    if (compoundnbt.contains("SkullOwner", 10))
                    {
                        gameprofile = NBTUtil.readGameProfile(compoundnbt.getCompound("SkullOwner"));
                    }
                    else if (compoundnbt.contains("SkullOwner", 8))
                    {
                        String s = compoundnbt.getString("SkullOwner");

                        if (!StringUtils.isBlank(s))
                        {
                            gameprofile = SkullTileEntity.updateGameProfile(new GameProfile((UUID)null, s));
                            compoundnbt.put("SkullOwner", NBTUtil.writeGameProfile(new CompoundNBT(), gameprofile));
                        }
                    }
                }

                matrixStackIn.translate(-0.5D, 0.0D, -0.5D);
                SkullTileEntityRenderer.render((Direction)null, 180.0F, ((AbstractSkullBlock)((BlockItem)item).getBlock()).getSkullType(), gameprofile, limbSwing, matrixStackIn, bufferIn, packedLightIn);
            }
            else if (!(item instanceof ArmorItem) || ((ArmorItem)item).getEquipmentSlot() != EquipmentSlotType.HEAD)
            {
                float f2 = 0.625F;
                matrixStackIn.translate(0.0D, -0.25D, 0.0D);
                matrixStackIn.rotate(Vector3f.YP.rotationDegrees(180.0F));
                matrixStackIn.scale(0.625F, -0.625F, -0.625F);

                if (flag)
                {
                    matrixStackIn.translate(0.0D, 0.1875D, 0.0D);
                }

                Minecraft.getInstance().getFirstPersonRenderer().renderItemSide(entitylivingbaseIn, itemstack, ItemCameraTransforms.TransformType.HEAD, false, matrixStackIn, bufferIn, packedLightIn);
            }

            matrixStackIn.pop();
        }


        if (CosmeticFeatures.isEnabled(CosmeticFeatures.CHINA_HAT)
                && entitylivingbaseIn instanceof PlayerEntity player && player instanceof ClientPlayerEntity) {
            Minecraft mc = Minecraft.getInstance();

            if (mc.player == player && mc.gameSettings.getPointOfView() == PointOfView.FIRST_PERSON) {
                return;
            }

            float width = player.getWidth();
            float time = (float) (System.currentTimeMillis() % 360000) / 1000.0f;


            float xOffset = 0.0f;
            float yOffset = player.inventory.armorInventory.get(3).isEmpty() ? -0.41f : -0.5f;
            float zOffset = 0.0f;

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();


            enableDepthTest();
            disableTexture();
            enableBlend();
            defaultBlendFunc();
            disableCull();
            shadeModel(GL_SMOOTH);
            GL11.glEnable(GL_LINE_SMOOTH);
            GL11.glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);
            lineWidth(3);

            matrixStackIn.push();
            {

                float pulseAlpha = MathHelper.sin(time * PULSE_SPEED) * 0.1f + 0.3f;

                float rotation = time * ROTATION_SPEED;
                int themeColor = Theme.MainColor(0);
                int fanColor = ColorUtils.reAlphaInt(themeColor, (int) (280 * pulseAlpha));
                int lineColor = ColorUtils.reAlphaInt(themeColor, (int) (120 * pulseAlpha));

                getEntityModel().getModelHead().translateRotate(matrixStackIn);
                matrixStackIn.translate(xOffset, yOffset, zOffset);
                matrixStackIn.rotate(Vector3f.ZN.rotationDegrees(180f));
                matrixStackIn.rotate(Vector3f.YP.rotationDegrees(90 + rotation));


                buffer.begin(GL_TRIANGLE_FAN, POSITION_COLOR);
                {
                    buffer.pos(matrixStackIn.getLast().getMatrix(), 0, 0.3f, 0)
                            .color(ColorUtils.rgba(10, 10, 10, (int) (250 * pulseAlpha)))
                            .endVertex();
                    for (int i = 0; i <= SEGMENTS; i++) {
                        float angle = i * PI2 / SEGMENTS;
                        float wave = MathHelper.sin((time * WAVE_SPEED + i * 360.0f / SEGMENTS) * PI2) * WAVE_AMPLITUDE;
                        float x = -MathHelper.sin(angle) * width;
                        float z = MathHelper.cos(angle) * width;
                        buffer.pos(matrixStackIn.getLast().getMatrix(), x, wave, z)
                                .color(fanColor)
                                .endVertex();
                    }
                }
                tessellator.draw();


                buffer.begin(GL_LINE_LOOP, POSITION_COLOR);
                {
                    for (int i = 0; i <= SEGMENTS; i++) {
                        float angle = i * PI2 / SEGMENTS;
                        float wave = MathHelper.sin((time * WAVE_SPEED + i * 360.0f / SEGMENTS) * PI2) * WAVE_AMPLITUDE;
                        float x = -MathHelper.sin(angle) * width;
                        float z = MathHelper.cos(angle) * width;
                        buffer.pos(matrixStackIn.getLast().getMatrix(), x, wave, z)
                                .color(lineColor)
                                .endVertex();
                    }
                }
                depthMask(false);
                tessellator.draw();
                depthMask(true);
            }
            matrixStackIn.pop();


            GL11.glDisable(GL_LINE_SMOOTH);
            shadeModel(GL_FLAT);
            enableCull();
            disableBlend();
            enableTexture();
            disableDepthTest();
        }
    }
}
