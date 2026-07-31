package net.minecraft.client.entity.player;

import xd.harm.utils.waveycapes.CapeHolder;
import xd.harm.utils.waveycapes.sim.StickSimulation;
import com.google.common.eventbus.Subscribe;
import com.google.common.hash.Hashing;
import com.mojang.authlib.GameProfile;
import java.io.File;
import java.io.InputStream;
import java.awt.image.BufferedImage;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import xd.harm.Harmony;
import xd.harm.modules.api.ModuleManager;
import xd.harm.utils.figura.CosmeticFeatures;
import xd.harm.utils.figura.CosmeticRenderer;
import xd.harm.utils.render.GifUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.client.renderer.texture.DownloadingTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.texture.Texture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.passive.ShoulderRidingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameType;
import net.optifine.Config;
import net.optifine.player.CapeUtils;
import net.optifine.player.PlayerConfigurations;
import net.optifine.reflect.Reflector;

public class AbstractClientPlayerEntity extends PlayerEntity implements CapeHolder
{
    private final StickSimulation stickSimulation;
    private NetworkPlayerInfo playerInfo;
    public float rotateElytraX;
    public float rotateElytraY;
    public float rotateElytraZ;
    public final ClientWorld worldClient;
    private ResourceLocation locationOfCape = null;
    private long reloadCapeTimeMs = 0L;
    private boolean elytraOfCape = false;
    private String nameClear = null;
    public ShoulderRidingEntity entityShoulderLeft;
    public ShoulderRidingEntity entityShoulderRight;
    public float capeRotateX;
    public float capeRotateY;
    public float capeRotateZ;
    private static final ResourceLocation TEXTURE_ELYTRA = new ResourceLocation("textures/entity/elytra.png");

    private static int lastCapeColor = -1;
    private static ResourceLocation coloredCapeLocation = null;
    private static BufferedImage originalCapeImage = null;
    private static long lastCapeColorRefreshMs = 0L;
    private static final long CAPE_COLOR_REFRESH_INTERVAL_MS = 1000L;

    public AbstractClientPlayerEntity(ClientWorld world, GameProfile profile)
    {
        super(world, world.func_239140_u_(), world.func_243489_v(), profile);
        this.worldClient = world;
        this.nameClear = profile.getName();

        if (this.nameClear != null && !this.nameClear.isEmpty())
        {
            this.nameClear = StringUtils.stripControlCodes(this.nameClear);
        }

        CapeUtils.downloadCape(this);
        PlayerConfigurations.getPlayerConfiguration(this);
        this.stickSimulation = new StickSimulation();
    }

    public boolean isSpectator()
    {
        NetworkPlayerInfo networkplayerinfo = Minecraft.getInstance().getConnection().getPlayerInfo(this.getGameProfile().getId());
        return networkplayerinfo != null && networkplayerinfo.getGameType() == GameType.SPECTATOR;
    }

    public boolean isCreative()
    {
        NetworkPlayerInfo networkplayerinfo = Minecraft.getInstance().getConnection().getPlayerInfo(this.getGameProfile().getId());
        return networkplayerinfo != null && networkplayerinfo.getGameType() == GameType.CREATIVE;
    }

    public boolean hasPlayerInfo()
    {
        return this.getPlayerInfo() != null;
    }

    @Nullable
    protected NetworkPlayerInfo getPlayerInfo()
    {
        if (this.playerInfo == null)
        {
            this.playerInfo = Minecraft.getInstance().getConnection().getPlayerInfo(this.getUniqueID());
        }

        return this.playerInfo;
    }

    public boolean hasSkin()
    {
        NetworkPlayerInfo networkplayerinfo = this.getPlayerInfo();
        return networkplayerinfo != null && networkplayerinfo.hasLocationSkin();
    }

    public ResourceLocation getLocationSkin() {
        NetworkPlayerInfo networkplayerinfo = this.getPlayerInfo();
        return networkplayerinfo == null ? DefaultPlayerSkin.getDefaultSkin(this.getUniqueID()) : networkplayerinfo.getLocationSkin();
    }

    @Nullable
    public ResourceLocation getLocationCape() {
        ModuleManager moduleManager = Harmony.getInstance().getModuleManager();

        if (CosmeticFeatures.isEnabled(CosmeticFeatures.RAINCOAT) && this instanceof ClientPlayerEntity &&
                !(moduleManager.getNoRender().isState() &&
                        moduleManager.getNoRender().element.getValueByName("Плащ").get())) {

            int themeColor = CosmeticRenderer.capeColor();

            long now = System.currentTimeMillis();
            if (coloredCapeLocation == null || (themeColor != lastCapeColor && now - lastCapeColorRefreshMs >= CAPE_COLOR_REFRESH_INTERVAL_MS)) {
                ResourceLocation previousCape = coloredCapeLocation;
                ResourceLocation newCape = createColoredCape(themeColor);
                if (newCape != null) {
                    if (previousCape != null) {
                        Minecraft.getInstance().getTextureManager().deleteTexture(previousCape);
                    }
                    lastCapeColor = themeColor;
                    lastCapeColorRefreshMs = now;
                    coloredCapeLocation = newCape;
                }
            }

            if (coloredCapeLocation != null) {
                return coloredCapeLocation;
            }

            return new ResourceLocation("harmony/images/gui/cape.png");
        }

        if (!Config.isShowCapes()) {
            return null;
        } else {
            if (this.reloadCapeTimeMs != 0L && System.currentTimeMillis() > this.reloadCapeTimeMs) {
                CapeUtils.reloadCape(this);
                this.reloadCapeTimeMs = 0L;
            }

            if (this.locationOfCape != null) {
                return this.locationOfCape;
            } else {
                NetworkPlayerInfo networkplayerinfo = this.getPlayerInfo();
                return networkplayerinfo == null ? null : networkplayerinfo.getLocationCape();
            }
        }
    }

    private ResourceLocation createColoredCape(int color) {
        try {
            if (originalCapeImage == null) {
                InputStream inputStream = Minecraft.getInstance().getResourceManager()
                        .getResource(new ResourceLocation("harmony/images/gui/cape.png"))
                        .getInputStream();
                originalCapeImage = ImageIO.read(inputStream);
                inputStream.close();
            }

            BufferedImage colored = new BufferedImage(
                    originalCapeImage.getWidth(),
                    originalCapeImage.getHeight(),
                    BufferedImage.TYPE_INT_ARGB
            );

            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;

            for (int x = 0; x < originalCapeImage.getWidth(); x++) {
                for (int y = 0; y < originalCapeImage.getHeight(); y++) {
                    int pixel = originalCapeImage.getRGB(x, y);
                    int alpha = (pixel >> 24) & 0xFF;

                    if (alpha > 0) {
                        int origR = (pixel >> 16) & 0xFF;
                        int origG = (pixel >> 8) & 0xFF;
                        int origB = pixel & 0xFF;

                        float brightness = (origR + origG + origB) / (3f * 255f);

                        int newR = Math.min(255, Math.max(0, (int)(r * brightness)));
                        int newG = Math.min(255, Math.max(0, (int)(g * brightness)));
                        int newB = Math.min(255, Math.max(0, (int)(b * brightness)));

                        colored.setRGB(x, y, (alpha << 24) | (newR << 16) | (newG << 8) | newB);
                    } else {
                        colored.setRGB(x, y, pixel);
                    }
                }
            }

            NativeImage nativeImage = new NativeImage(colored.getWidth(), colored.getHeight(), true);
            for (int x = 0; x < colored.getWidth(); x++) {
                for (int y = 0; y < colored.getHeight(); y++) {
                    int argb = colored.getRGB(x, y);
                    int a = (argb >> 24) & 0xFF;
                    int red = (argb >> 16) & 0xFF;
                    int green = (argb >> 8) & 0xFF;
                    int blue = argb & 0xFF;
                    int abgr = (a << 24) | (blue << 16) | (green << 8) | red;
                    nativeImage.setPixelRGBA(x, y, abgr);
                }
            }

            DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);
            ResourceLocation location = Minecraft.getInstance().getTextureManager()
                    .getDynamicTextureLocation("harmony_cape_" + Integer.toHexString(color), dynamicTexture);

            return location;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Subscribe
    public void updateCape() {
        if (this.hasCustomCape()) {
            this.simulate(this);
        }
        super.updateCape();
    }

    public boolean hasCustomCape() {
        return this instanceof ClientPlayerEntity;
    }

    public boolean isPlayerInfoSet()
    {
        return this.getPlayerInfo() != null;
    }

    @Nullable
    public ResourceLocation getLocationElytra() {
        NetworkPlayerInfo networkplayerinfo = this.getPlayerInfo();
        ResourceLocation elytraLocation = networkplayerinfo == null ? null : networkplayerinfo.getLocationElytra();

        if (elytraLocation != null) {
            return elytraLocation;
        }

        return TEXTURE_ELYTRA;
    }

    public static DownloadingTexture getDownloadImageSkin(ResourceLocation resourceLocationIn, String username)
    {
        TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
        Texture texture = texturemanager.getTexture(resourceLocationIn);

        if (texture == null)
        {
            texture = new DownloadingTexture((File)null, String.format("http://skins.minecraft.net/MinecraftSkins/%s.png", StringUtils.stripControlCodes(username)), DefaultPlayerSkin.getDefaultSkin(getOfflineUUID(username)), true, (Runnable)null);
            texturemanager.loadTexture(resourceLocationIn, texture);
        }

        return (DownloadingTexture)texture;
    }

    public static ResourceLocation getLocationSkin(String username)
    {
        return new ResourceLocation("skins/" + Hashing.sha1().hashUnencodedChars(StringUtils.stripControlCodes(username)));
    }

    public String getSkinType()
    {
        NetworkPlayerInfo networkplayerinfo = this.getPlayerInfo();
        return networkplayerinfo == null ? DefaultPlayerSkin.getSkinType(this.getUniqueID()) : networkplayerinfo.getSkinType();
    }

    public float getFovModifier()
    {
        float f = 1.0F;

        if (this.abilities.isFlying)
        {
            f *= 1.1F;
        }

        f = (float)((double)f * ((this.getAttributeValue(Attributes.MOVEMENT_SPEED) / (double)this.abilities.getWalkSpeed() + 1.0D) / 2.0D));

        if (this.abilities.getWalkSpeed() == 0.0F || Float.isNaN(f) || Float.isInfinite(f))
        {
            f = 1.0F;
        }

        if (this.isHandActive() && this.getActiveItemStack().getItem() instanceof BowItem)
        {
            int i = this.getItemInUseMaxCount();
            float f1 = (float)i / 20.0F;

            if (f1 > 1.0F)
            {
                f1 = 1.0F;
            }
            else
            {
                f1 = f1 * f1;
            }

            f *= 1.0F - f1 * 0.15F;
        }

        return Reflector.ForgeHooksClient_getOffsetFOV.exists() ? Reflector.callFloat(Reflector.ForgeHooksClient_getOffsetFOV, this, f) : MathHelper.lerp(Minecraft.getInstance().gameSettings.fovScaleEffect, 1.0F, f);
    }

    public String getNameClear()
    {
        return this.nameClear;
    }

    public ResourceLocation getLocationOfCape()
    {
        return this.locationOfCape;
    }

    public void setLocationOfCape(ResourceLocation p_setLocationOfCape_1_)
    {
        this.locationOfCape = p_setLocationOfCape_1_;
    }

    public boolean hasElytraCape() {
        ResourceLocation capeLocation = this.getLocationCape();

        if (capeLocation == null) {
            return false;
        }

        return false;
    }

    public void setElytraOfCape(boolean p_setElytraOfCape_1_) {
        this.elytraOfCape = false;
    }

    public boolean isElytraOfCape() {
        return false;
    }

    public long getReloadCapeTimeMs()
    {
        return this.reloadCapeTimeMs;
    }

    public void setReloadCapeTimeMs(long p_setReloadCapeTimeMs_1_)
    {
        this.reloadCapeTimeMs = p_setReloadCapeTimeMs_1_;
    }

    @Override
    public StickSimulation getSimulation() {
        return this.stickSimulation;
    }
}
