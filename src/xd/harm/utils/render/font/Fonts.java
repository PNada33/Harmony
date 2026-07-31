package xd.harm.utils.render.font;

public class Fonts {

    public static Font montserrat, consolas, icons,icons2, damage, sfui,sfuy, sfbold, sfMedium,tfMedium,sfsemibolt,xyinakakita,ico, guiicons;
    public static xd.harm.utils.text.font.styled.StyledFont minecraftia_16;

    public static void register() {
        montserrat = new Font("Montserrat-Regular.ttf.png", "Montserrat-Regular.ttf.json");
        icons = new Font("icons.ttf.png", "icons.ttf.json");
        consolas = new Font("consolas.ttf.png", "consolas.ttf.json");
        damage = new Font("damage.ttf.png", "damage.ttf.json");
        sfui = new Font("sf_semibold.png", "sf_semibold.json");
        sfuy = new Font("sfsemi.png", "sfsemi.json");
        sfbold = new Font("sf_bold.ttf.png", "sf_bold.ttf.json");
        icons2 = new Font("icons2.png", "icons2.json");
        sfMedium = new Font("sf_medium.ttf.png", "sf_medium.ttf.json");
        sfsemibolt = new Font("sf_light.png", "sf_light.json");
        tfMedium = new Font("sf_bold.png", "sf_bold.json");
        xyinakakita = new Font("sf_medium.png", "sf_medium.json");
        ico = new Font("icos.png", "icos.json");
        guiicons = new Font("guiicons.png", "guiicons.json");
    }

}
