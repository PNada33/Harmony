package xd.harm.utils.player;

import xd.harm.utils.render.color.ColorUtils;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DonateItems {

    public static ArrayList<ItemStack> donitem = new ArrayList<>();

    public static HashMap<String, Integer> itemColors = new HashMap<>();

    public static void add() {

        itemColors.put("Меч Крушителя", ColorUtils.rgb(255, 0, 0));
        itemColors.put("Дезориентация", ColorUtils.rgb(255, 0, 255));
        itemColors.put("Пласт", ColorUtils.rgb(0, 128, 0));
        itemColors.put("Явная пыль", ColorUtils.rgb(255, 255, 255));
        itemColors.put("Трезубец Крушителя", ColorUtils.rgb(0, 191, 255));
        itemColors.put("Лук Крушителя", ColorUtils.rgb(165, 42, 42));
        itemColors.put("Арбалет Крушителя", ColorUtils.rgb(165, 42, 42));
        itemColors.put("Трапка", ColorUtils.rgb(255, 215, 0));
        itemColors.put("Снежок заморозка", ColorUtils.rgb(135, 206, 250));
        itemColors.put("Шлем Крушителя", ColorUtils.rgb(255, 0, 0));
        itemColors.put("Нагрудник Крушителя", ColorUtils.rgb(255, 0, 0));
        itemColors.put("Поножи Крушителя", ColorUtils.rgb(255, 0, 0));
        itemColors.put("Ботинки Крушителя", ColorUtils.rgb(255, 0, 0));
        itemColors.put("Зелье силы", ColorUtils.rgb(255, 0, 0));
        itemColors.put("Зелье невидимости", ColorUtils.rgb(169, 169, 169));
        itemColors.put("Зелье скорости", ColorUtils.rgb(0, 0, 255));
        itemColors.put("Зелье регенерации", ColorUtils.rgb(255, 105, 180));
        itemColors.put("Вспышка", ColorUtils.rgb(255, 255, 255));
        itemColors.put("Огненный смерч", ColorUtils.rgb(255, 100, 0));
        itemColors.put("Божья аура", ColorUtils.rgb(255, 255, 200));
        itemColors.put("Хлопушка", ColorUtils.rgb(255, 0, 100));
        itemColors.put("Святая вода", ColorUtils.rgb(100, 200, 255));
        itemColors.put("Зелье гнева", ColorUtils.rgb(200, 0, 0));
        itemColors.put("Зелье палладина", ColorUtils.rgb(255, 215, 0));
        itemColors.put("Зелье ассасина", ColorUtils.rgb(50, 50, 50));
        itemColors.put("Зелье радиации", ColorUtils.rgb(0, 255, 0));
        itemColors.put("Снотворное", ColorUtils.rgb(100, 100, 200));
        itemColors.put("Элитры Крушителя", ColorUtils.rgb(255, 0, 0));
        itemColors.put("Талисман Крушителя", ColorUtils.rgb(255, 0, 0));
        itemColors.put("Талисман Карателя", ColorUtils.rgb(255, 80, 0));
        itemColors.put("Талисман Раздора", ColorUtils.rgb(255, 60, 60));
        itemColors.put("Талисман Ярости", ColorUtils.rgb(255, 40, 40));
        itemColors.put("Талисман Тирана", ColorUtils.rgb(200, 0, 0));
        itemColors.put("Талисман Вихря", ColorUtils.rgb(70, 180, 255));
        itemColors.put("Талисман Мрака", ColorUtils.rgb(90, 0, 120));
        itemColors.put("Талисман Демона", ColorUtils.rgb(220, 40, 40));
        itemColors.put("Сфера Хаоса", ColorUtils.rgb(170, 60, 255));
        itemColors.put("Сфера Сатира", ColorUtils.rgb(255, 160, 0));
        itemColors.put("Сфера Бестии", ColorUtils.rgb(60, 200, 80));
        itemColors.put("Сфера Ареса", ColorUtils.rgb(255, 60, 40));
        itemColors.put("Сфера Гидры", ColorUtils.rgb(40, 200, 180));
        itemColors.put("Сфера Икара", ColorUtils.rgb(255, 215, 0));
        itemColors.put("Сфера Титана", ColorUtils.rgb(170, 170, 170));
        itemColors.put("Сфера Эрида", ColorUtils.rgb(120, 180, 255));

        itemColors.put("\u041a\u043d\u0438\u0433\u0430 \u041f\u043e\u0447\u0438\u043d\u043a\u0430", ColorUtils.rgb(255, 215, 0));

        ItemStack desorientationItem = new ItemStack(Items.ENDER_EYE);
        desorientationItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Дезориентация"));

        ItemStack crusherSwordItem = new ItemStack(Items.NETHERITE_SWORD);
        crusherSwordItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Меч Крушителя"));

        ItemStack plastItem = new ItemStack(Items.DRIED_KELP);
        plastItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Пласт"));

        ItemStack obviousDustItem = new ItemStack(Items.SUGAR);
        obviousDustItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Явная пыль"));

        ItemStack tridentCrusherItem = new ItemStack(Items.TRIDENT);
        tridentCrusherItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Трезубец Крушителя"));

        ItemStack crusherBowItem = new ItemStack(Items.BOW);
        crusherBowItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Лук Крушителя"));

        ItemStack crossbowCrusherItem = new ItemStack(Items.CROSSBOW);
        crossbowCrusherItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Арбалет Крушителя"));

        ItemStack trapItem = new ItemStack(Items.NETHERITE_SCRAP);
        trapItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Трапка"));


        ItemStack freezingSnowballItem = new ItemStack(Items.SNOWBALL);
        freezingSnowballItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Снежок заморозка"));

        ItemStack crusherHelmetItem = new ItemStack(Items.NETHERITE_HELMET);
        crusherHelmetItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Шлем Крушителя"));


        ItemStack crusherChestplateItem = new ItemStack(Items.NETHERITE_CHESTPLATE);
        crusherChestplateItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Нагрудник Крушителя"));


        ItemStack crusherLeggingsItem = new ItemStack(Items.NETHERITE_LEGGINGS);
        crusherLeggingsItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Поножи Крушителя"));


        ItemStack crusherBootsItem = new ItemStack(Items.NETHERITE_BOOTS);
        crusherBootsItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Ботинки Крушителя"));

        ItemStack potionOfStrengthItem = new ItemStack(Items.POTION);
        potionOfStrengthItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Зелье силы"));

        ItemStack potionOfInvisibilityItem = new ItemStack(Items.POTION);
        potionOfInvisibilityItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Зелье невидимости"));

        ItemStack potionOfSwiftnessItem = new ItemStack(Items.POTION);
        potionOfSwiftnessItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Зелье скорости"));

        ItemStack potionOfRegenerationItem = new ItemStack(Items.POTION);
        potionOfRegenerationItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Зелье регенерации"));

        ItemStack fieryTornadoItem = new ItemStack(Items.FIRE_CHARGE);
        fieryTornadoItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Огненный смерч"));

        ItemStack godsAuraItem = new ItemStack(Items.PHANTOM_MEMBRANE);
        godsAuraItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Божья аура"));

        ItemStack popperItem = new ItemStack(Items.SPLASH_POTION);
        popperItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Хлопушка"));

        ItemStack holyWaterItem = new ItemStack(Items.SPLASH_POTION);
        holyWaterItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Святая вода"));

        ItemStack wrathPotionItem = new ItemStack(Items.SPLASH_POTION);
        wrathPotionItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Зелье гнева"));

        ItemStack paladinPotionItem = new ItemStack(Items.SPLASH_POTION);
        paladinPotionItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Зелье палладина"));

        ItemStack assassinPotionItem = new ItemStack(Items.SPLASH_POTION);
        assassinPotionItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Зелье ассасина"));

        ItemStack radiationPotionItem = new ItemStack(Items.SPLASH_POTION);
        radiationPotionItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Зелье радиации"));

        ItemStack sleepingPotionItem = new ItemStack(Items.SPLASH_POTION);
        sleepingPotionItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Снотворное"));

        ItemStack crusherElytraItem = new ItemStack(Items.ELYTRA);
        crusherElytraItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Элитры Крушителя"));

        ItemStack talismanCrusherItem = new ItemStack(Items.TOTEM_OF_UNDYING);
        talismanCrusherItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Талисман Крушителя"));

        ItemStack talismanExecutionerItem = new ItemStack(Items.TOTEM_OF_UNDYING);
        talismanExecutionerItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Талисман Карателя"));

        ItemStack talismanDiscordItem = new ItemStack(Items.TOTEM_OF_UNDYING);
        talismanDiscordItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Талисман Раздора"));

        ItemStack talismanRageItem = new ItemStack(Items.TOTEM_OF_UNDYING);
        talismanRageItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Талисман Ярости"));

        ItemStack talismanTyrantItem = new ItemStack(Items.TOTEM_OF_UNDYING);
        talismanTyrantItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Талисман Тирана"));

        ItemStack talismanWhirlItem = new ItemStack(Items.TOTEM_OF_UNDYING);
        talismanWhirlItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Талисман Вихря"));

        ItemStack talismanDarknessItem = new ItemStack(Items.TOTEM_OF_UNDYING);
        talismanDarknessItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Талисман Мрака"));

        ItemStack talismanDemonItem = new ItemStack(Items.TOTEM_OF_UNDYING);
        talismanDemonItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Талисман Демона"));

        ItemStack sphereChaosItem = new ItemStack(Items.PLAYER_HEAD);
        sphereChaosItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Сфера Хаоса"));

        ItemStack sphereSatyrItem = new ItemStack(Items.PLAYER_HEAD);
        sphereSatyrItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Сфера Сатира"));

        ItemStack sphereBeastItem = new ItemStack(Items.PLAYER_HEAD);
        sphereBeastItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Сфера Бестии"));

        ItemStack sphereAresItem = new ItemStack(Items.PLAYER_HEAD);
        sphereAresItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Сфера Ареса"));

        ItemStack sphereHydraItem = new ItemStack(Items.PLAYER_HEAD);
        sphereHydraItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Сфера Гидры"));

        ItemStack sphereIcarusItem = new ItemStack(Items.PLAYER_HEAD);
        sphereIcarusItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Сфера Икара"));

        ItemStack sphereTitanItem = new ItemStack(Items.PLAYER_HEAD);
        sphereTitanItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Сфера Титана"));

        ItemStack sphereEridaItem = new ItemStack(Items.PLAYER_HEAD);
        sphereEridaItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("Сфера Эрида"));

        ItemStack mendingBookItem = new ItemStack(Items.ENCHANTED_BOOK);
        mendingBookItem.setDisplayName(ITextComponent.getTextComponentOrEmpty("\u041a\u043d\u0438\u0433\u0430 \u041f\u043e\u0447\u0438\u043d\u043a\u0430"));

        donitem.addAll(List.of(
                desorientationItem,
                crusherSwordItem,
                plastItem,
                obviousDustItem,
                tridentCrusherItem,
                crusherBowItem,
                crossbowCrusherItem,
                trapItem,
                freezingSnowballItem,
                crusherHelmetItem,
                crusherChestplateItem,
                crusherLeggingsItem,
                crusherBootsItem,
                potionOfStrengthItem,
                potionOfInvisibilityItem,
                potionOfSwiftnessItem,
                potionOfRegenerationItem,
                fieryTornadoItem,
                godsAuraItem,
                popperItem,
                holyWaterItem,
                wrathPotionItem,
                paladinPotionItem,
                assassinPotionItem,
                radiationPotionItem,
                sleepingPotionItem,
                crusherElytraItem,
                talismanExecutionerItem,
                talismanCrusherItem,
                sphereTitanItem,
                sphereChaosItem,
                talismanDiscordItem,
                talismanTyrantItem,
                talismanRageItem,
                talismanWhirlItem,
                talismanDarknessItem,
                talismanDemonItem,
                sphereSatyrItem,
                sphereBeastItem,
                sphereAresItem,
                sphereHydraItem,
                sphereIcarusItem,
                sphereEridaItem,
                mendingBookItem
        ));

        HashMap<Enchantment, Integer> fake = new HashMap<>();
        fake.put(Enchantments.UNBREAKING, 0);
        for (ItemStack s : donitem) {
            EnchantmentHelper.setEnchantments(fake, s);
        }
    }

    public static ItemStack add(String texture, String name) {
        try {
            ItemStack magma = new ItemStack(Items.PLAYER_HEAD);
            magma.setTag(JsonToNBT.getTagFromJson(String.format("{SkullOwner:{Id:[I;-1949909288,1299464445,-1707774066,-249984712],Properties:{textures:[{Value:\"%s\"}]},Name:\"%s\"}}", texture, name)));
            magma.setDisplayName(new StringTextComponent(name));
            return magma;
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
