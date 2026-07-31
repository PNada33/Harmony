package xd.harm.utils.text.font;

import xd.harm.utils.render.font.Font;
import lombok.SneakyThrows;
import xd.harm.utils.text.font.common.Lang;
import xd.harm.utils.text.font.styled.StyledFont;


public class ClientFonts {
    public static final String FONT_DIR = "/assets/minecraft/harmony/fonts/normal/";

    public static volatile StyledFont[] msBold = new StyledFont[50];
    public static volatile StyledFont[] msMedium = new StyledFont[50];
    public static volatile StyledFont[] msLight = new StyledFont[50];
    public static volatile StyledFont[] msRegular = new StyledFont[50];
    public static volatile StyledFont[] msSemiBold = new StyledFont[50];
    public static volatile StyledFont[] roadRage = new StyledFont[50];
    public static volatile StyledFont[] sf_medium = new StyledFont[24];
    public static volatile StyledFont[] font = new StyledFont[52];
    public static volatile StyledFont[] small_pixel = new StyledFont[50];
    public static volatile StyledFont[] tech = new StyledFont[50];
    public static volatile StyledFont[] icon = new StyledFont[50];
    public static volatile StyledFont[] icons = new StyledFont[50];
    public static volatile StyledFont[] icons_wex = new StyledFont[50];
    public static volatile StyledFont[] icons_nur = new StyledFont[50];
    public static volatile StyledFont[] icons_client = new StyledFont[50];
    public static volatile StyledFont[] comfortaa = new StyledFont[50];
    public static volatile StyledFont[] interBold = new StyledFont[80];
    public static volatile StyledFont[] interMedium = new StyledFont[80];
    public static volatile StyledFont[] tenacity = new StyledFont[80];
    public static volatile StyledFont[] interRegular = new StyledFont[80];
    public static volatile StyledFont[] interSemiBold = new StyledFont[80];
    public static volatile StyledFont[] AltManager = new StyledFont[80];
    public static volatile StyledFont[] watermark = new StyledFont[80];
    public static volatile StyledFont[] elusiveText = new StyledFont[80];
    public static volatile StyledFont[] elusiveLogo = new StyledFont[80];
    public static volatile StyledFont[] elusiveIcons = new StyledFont[80];
    public static volatile StyledFont[] elusiveNotify = new StyledFont[80];
    public static volatile StyledFont[] iconsnew = new StyledFont[80];
    public static volatile StyledFont[] minecraftia = new StyledFont[50];
    public static volatile StyledFont[] keybind = new StyledFont[50];
    public static volatile StyledFont[] upico = new StyledFont[50];
    public static volatile StyledFont[] nunitoRegular = new StyledFont[50];
    public static volatile StyledFont[] nunitoBold = new StyledFont[50];
    public static volatile StyledFont minecraftia_16;

    @SneakyThrows
    public static void init() {

        for (int i = 8; i < 50;i++) {
            font[i] = new StyledFont("font.ttf", i, 0.0f, 0.0f, 0.0f, true, Lang.ENG_RU);
        }
        for (int i = 8; i < 50;i++) {
            msBold[i] = new StyledFont("Montserrat-Bold.ttf", i, 0.0f, 0.0f, 0.0f, true, Lang.ENG_RU);
        }
        for (int i = 8; i < 50;i++) {
            msLight[i] = new StyledFont("Montserrat-Light.ttf", i, 0.0f, 0.0f, 0.0f, true, Lang.ENG_RU);
        }
        for (int i = 8; i < 50;i++) {
            msMedium[i] = new StyledFont("Montserrat-Medium.ttf", i, 0.0f, 0.0f, 0.0f, true, Lang.ENG_RU);
        }
        for (int i = 8; i < 50;i++) {
            msRegular[i] = new StyledFont("Montserrat-Regular.ttf", i, 0.0f, 0.0f, 0.0f, true, Lang.ENG_RU);
        }
        for (int i = 8; i < 50;i++) {
            msSemiBold[i] = new StyledFont("Montserrat-SemiBold.ttf", i, 0.0f, 0.0f, 0.0f, true, Lang.ENG_RU);
        }
        for (int i = 8; i < 23;i++) {
            sf_medium[i] = new StyledFont("sf_medium.ttf", i, 0.0f, 0.0f, 0.0f, true, Lang.ENG_RU);
        }
        for (int i = 8; i < 50;i++) {
            roadRage[i] = new StyledFont("roadrage.ttf", i, 0.0f, 0.0f, 0.0f, true, Lang.ENG_RU);
        }
        for (int i = 8; i < 50;i++) {
            small_pixel[i] = new StyledFont("small_pixel.ttf", i, 0.0f, 0.0f, 0.0f, true, Lang.ENG_RU);
        }
        for (int i = 8; i < 50;i++) {
            tech[i] = new StyledFont("tech.ttf", i, 0.0f, 0.0f, 0.0f, true, Lang.ENG_RU);
        }
        for (int i = 8; i < 50;i++) {
            icon[i] = new StyledFont("icon.ttf", i, 0.0f, 0.0f, 0.0f, true, Lang.ENG_RU);
        }
        for (int i = 8; i < 50;i++) {
            icons[i] = new StyledFont("penus.ttf", i, 0.0f, 0.0f, 0.0f, true, Lang.ENG_RU);
        }
        for (int i = 8; i < 50;i++) {
            icons_client[i] = new StyledFont("iconclient.ttf", i, 0.0f, 0.0f, 0.0f, true, Lang.ENG_RU);
        }
        for (int i = 8; i < 50;i++) {
            icons_wex[i] = new StyledFont("iconswex.ttf", i, 0.0f, 0.0f, 0.0f, true, Lang.ENG_RU);
        }
        for (int i = 8; i < 50;i++) {
            icons_nur[i] = new StyledFont("iconsnur.ttf", i, 0.0f, 0.0f, 0.0f, true, Lang.ENG_RU);
        }
        for (int i = 8; i < 50;i++) {
            comfortaa[i] = new StyledFont("comfortaa-regular.ttf", i, 0.0f, 0.0f, 0.0f, true, Lang.ENG_RU);
        }
        for (int i = 8; i < 80;i++) {
            interRegular[i] = new StyledFont("inter_regular.ttf", i, 0.0f, 0.0f, 0.0f, true, Lang.ENG_RU);
        }
        for (int i = 8; i < 80;i++) {
            interMedium[i] = new StyledFont("inter_medium.ttf", i, 0.0f, 0.0f, 0.0f, true, Lang.ENG_RU);
        }
        for (int i = 8; i < 80;i++) {
            interSemiBold[i] = new StyledFont("inter_semibold.ttf", i, 0.0f, 0.0f, 0.0f, true, Lang.ENG_RU);
        }
        for (int i = 8; i < 80;i++) {
            tenacity[i] = new StyledFont("tenacity.ttf", i, 0.0f, 0.0f, 0.0f, true, Lang.ENG_RU);
        }
        for (int i = 8; i < 80;i++) {
            interBold[i] = new StyledFont("inter_bold.ttf", i, 0.0f, 0.0f, 0.0f, true, Lang.ENG_RU);
        }
        for (int i = 8; i < 80;i++) {
            watermark[i] = new StyledFont("elusive-logo.ttf", i, 0.0f, 0.0f, 0.0f, true, Lang.ENG_RU);
        }
        for (int i = 8; i < 80;i++) {
            elusiveText[i] = new StyledFont("elusive-tenacity-bold.ttf", i, 0.0f, 0.0f, 0.0f, true, Lang.ENG_RU);
            elusiveLogo[i] = new StyledFont("elusive-logo.ttf", i, 0.0f, 0.0f, 0.0f, true, Lang.ENG_RU);
            elusiveIcons[i] = new StyledFont("elusive-icons.ttf", i, 0.0f, 0.0f, 0.0f, true, Lang.ENG_RU);
            elusiveNotify[i] = new StyledFont("elusive-notify.ttf", i, 0.0f, 0.0f, 0.0f, true, Lang.ENG_RU);
        }
        for (int i = 8; i < 80;i++) {
            iconsnew[i] = new StyledFont("fps.ttf", i, 0.0f, 0.0f, 0.0f, true, Lang.ENG_RU);
        }
        for (int i = 8; i < 80;i++) {
            AltManager[i] = new StyledFont("AltManager.ttf", i, 0.0f, 0.0f, 0.0f, true, Lang.ENG_RU);
        }
        for (int i = 8; i < 50;i++) {
            minecraftia[i] = new StyledFont("minecraftia.ttf", i, 0.0f, 0.0f, 0.0f, true, Lang.ENG_RU);
        }
        for (int i = 8; i < 50;i++) {
            keybind[i] = new StyledFont("keybind.ttf", i, 0.0f, 0.0f, 0.0f, true, Lang.ENG_RU);
        }
        for (int i = 8; i < 50;i++) {
            upico[i] = new StyledFont("upico.ttf", i, 0.0f, 0.0f, 0.0f, true, Lang.ENG_RU);
        }
        for (int i = 8; i < 50;i++) {
            nunitoRegular[i] = new StyledFont("Nunito-Regular.ttf", i, 0.0f, 0.0f, 0.0f, true, Lang.ENG_RU);
        }
        for (int i = 8; i < 50;i++) {
            nunitoBold[i] = new StyledFont("Nunito-Bold.ttf", i, 0.0f, 0.0f, 0.0f, true, Lang.ENG_RU);
        }
        minecraftia_16 = minecraftia[16];
        xd.harm.utils.render.font.Fonts.minecraftia_16 = minecraftia_16;
    }
}
