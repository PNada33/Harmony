
package xd.harm.modules.impl.player.autobuy;

import xd.harm.utils.client.ClientUtility;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.PotionUtils;

public class AutoBuyItems {
    public List<AutoBuyItem> list = new ArrayList();
    private final Map<Item, List<AutoBuyItem>> itemMap = new HashMap<>();

    public AutoBuyItems() {
        this.addItems();
        this.rebuildMap();
    }

    private void addItems() {
        if (!this.list.isEmpty()) {
            this.list.clear();
        }

        List<Enchant> repairBookEnchants = new ArrayList();
        repairBookEnchants.add(new Enchant(Enchantments.MENDING, 1));
        HashMap<Attribute, Map.Entry<Float, AttributeModifier.Operation>> karatel = new HashMap();
        if (!ClientUtility.isConnectedToServer("spookytime")) {
            karatel.putAll(Map.of(Attributes.MOVEMENT_SPEED, Map.entry(0.1F, Operation.MULTIPLY_BASE), Attributes.MAX_HEALTH, Map.entry(-4.0F, Operation.ADDITION), Attributes.ATTACK_DAMAGE, Map.entry(7.0F, Operation.ADDITION)));
        }

        HashMap<Attribute, Map.Entry<Float, AttributeModifier.Operation>> krush = new HashMap();
        if (!ClientUtility.isConnectedToServer("spookytime")) {
            krush.putAll(Map.of(Attributes.ARMOR, Map.entry(2.0F, Operation.ADDITION), Attributes.ARMOR_TOUGHNESS, Map.entry(2.0F, Operation.ADDITION), Attributes.ATTACK_DAMAGE, Map.entry(3.0F, Operation.ADDITION), Attributes.MAX_HEALTH, Map.entry(4.0F, Operation.ADDITION)));
        }

        HashMap<Attribute, Map.Entry<Float, AttributeModifier.Operation>> yarosti = new HashMap();
        if (!ClientUtility.isConnectedToServer("spookytime")) {
            yarosti.putAll(Map.of(Attributes.ATTACK_DAMAGE, Map.entry(5.0F, Operation.ADDITION), Attributes.MAX_HEALTH, Map.entry(-4.0F, Operation.ADDITION)));
        }

        HashMap<Attribute, Map.Entry<Float, AttributeModifier.Operation>> tirana = new HashMap();
        if (!ClientUtility.isConnectedToServer("spookytime")) {
            tirana.putAll(Map.of(Attributes.ARMOR, Map.entry(2.0F, Operation.ADDITION), Attributes.ATTACK_DAMAGE, Map.entry(2.0F, Operation.ADDITION), Attributes.MAX_HEALTH, Map.entry(-4.0F, Operation.ADDITION)));
        }

        HashMap<Attribute, Map.Entry<Float, AttributeModifier.Operation>> razdora = new HashMap();
        razdora.putAll(Map.of(Attributes.ARMOR, Map.entry(-3.0F, Operation.ADDITION), Attributes.MOVEMENT_SPEED, Map.entry(0.1F, Operation.MULTIPLY_BASE), Attributes.ATTACK_DAMAGE, Map.entry(4.0F, Operation.ADDITION), Attributes.ATTACK_SPEED, Map.entry(0.1F, Operation.MULTIPLY_BASE), Attributes.MAX_HEALTH, Map.entry(2.0F, Operation.ADDITION)));

        HashMap<Attribute, Map.Entry<Float, AttributeModifier.Operation>> mraka = new HashMap();
        if (!ClientUtility.isConnectedToServer("spookytime")) {
            mraka.putAll(Map.of(Attributes.ARMOR, Map.entry(1.5F, Operation.ADDITION), Attributes.MAX_HEALTH, Map.entry(1.5F, Operation.ADDITION)));
        }

        HashMap<Attribute, Map.Entry<Float, AttributeModifier.Operation>> demona = new HashMap();
        if (!ClientUtility.isConnectedToServer("spookytime")) {
            demona.putAll(Map.of(Attributes.ATTACK_SPEED, Map.entry(0.1F, Operation.MULTIPLY_BASE), Attributes.ATTACK_DAMAGE, Map.entry(2.5F, Operation.ADDITION)));
        }

        HashMap<Attribute, Map.Entry<Float, AttributeModifier.Operation>> vixrya = new HashMap();
        if (!ClientUtility.isConnectedToServer("spookytime")) {
            vixrya.putAll(Map.of(Attributes.MOVEMENT_SPEED, Map.entry(0.15F, Operation.MULTIPLY_BASE), Attributes.ATTACK_SPEED, Map.entry(0.15F, Operation.MULTIPLY_BASE), Attributes.MAX_HEALTH, Map.entry(2.0F, Operation.ADDITION)));
        }

        HashMap<Attribute, Map.Entry<Float, AttributeModifier.Operation>> erida = new HashMap();
        if (!ClientUtility.isConnectedToServer("spookytime")) {
            erida.putAll(Map.of(Attributes.LUCK, Map.entry(1.0F, Operation.ADDITION), Attributes.MAX_HEALTH, Map.entry(2.0F, Operation.ADDITION)));
        }

        HashMap<Attribute, Map.Entry<Float, AttributeModifier.Operation>> xaosa = new HashMap();
        if (!ClientUtility.isConnectedToServer("spookytime")) {
            xaosa.putAll(Map.of(Attributes.ARMOR, Map.entry(2.0F, Operation.ADDITION), Attributes.MOVEMENT_SPEED, Map.entry(0.07F, Operation.MULTIPLY_BASE), Attributes.ATTACK_DAMAGE, Map.entry(3.0F, Operation.ADDITION), Attributes.ATTACK_SPEED, Map.entry(0.13F, Operation.MULTIPLY_BASE), Attributes.MAX_HEALTH, Map.entry(-4.0F, Operation.ADDITION)));
        }

        HashMap<Attribute, Map.Entry<Float, AttributeModifier.Operation>> titana = new HashMap();
        if (!ClientUtility.isConnectedToServer("spookytime")) {
            titana.putAll(Map.of(Attributes.ARMOR, Map.entry(3.0F, Operation.ADDITION), Attributes.ARMOR_TOUGHNESS, Map.entry(3.0F, Operation.ADDITION), Attributes.MOVEMENT_SPEED, Map.entry(-0.15F, Operation.MULTIPLY_BASE)));
        }

        HashMap<Attribute, Map.Entry<Float, AttributeModifier.Operation>> satira = new HashMap();
        if (!ClientUtility.isConnectedToServer("spookytime")) {
            satira.putAll(Map.of(Attributes.ATTACK_DAMAGE, Map.entry(2.0F, Operation.ADDITION), Attributes.ATTACK_SPEED, Map.entry(0.15F, Operation.MULTIPLY_BASE)));
        }

        HashMap<Attribute, Map.Entry<Float, AttributeModifier.Operation>> ikara = new HashMap();
        if (!ClientUtility.isConnectedToServer("spookytime")) {
            ikara.putAll(Map.of(Attributes.ATTACK_DAMAGE, Map.entry(2.0F, Operation.ADDITION), Attributes.MAX_HEALTH, Map.entry(2.0F, Operation.ADDITION)));
        }

        HashMap<Attribute, Map.Entry<Float, AttributeModifier.Operation>> gidry = new HashMap();
        if (!ClientUtility.isConnectedToServer("spookytime")) {
            gidry.putAll(Map.of(Attributes.ARMOR, Map.entry(2.0F, Operation.ADDITION), Attributes.MAX_HEALTH, Map.entry(4.0F, Operation.ADDITION)));
        }

        HashMap<Attribute, Map.Entry<Float, AttributeModifier.Operation>> bestii = new HashMap();
        if (!ClientUtility.isConnectedToServer("spookytime")) {
            bestii.putAll(Map.of(Attributes.ARMOR, Map.entry(1.0F, Operation.ADDITION), Attributes.MOVEMENT_SPEED, Map.entry(0.1F, Operation.MULTIPLY_BASE), Attributes.ATTACK_SPEED, Map.entry(0.1F, Operation.MULTIPLY_BASE), Attributes.MAX_HEALTH, Map.entry(4.0F, Operation.ADDITION)));
        }

        HashMap<Attribute, Map.Entry<Float, AttributeModifier.Operation>> afiny = new HashMap();
        if (!ClientUtility.isConnectedToServer("spookytime")) {
            afiny.putAll(Map.of(Attributes.MOVEMENT_SPEED, Map.entry(0.15F, Operation.MULTIPLY_BASE), Attributes.ATTACK_SPEED, Map.entry(0.15F, Operation.MULTIPLY_BASE), Attributes.ATTACK_DAMAGE, Map.entry(3.0F, Operation.ADDITION), Attributes.MAX_HEALTH, Map.entry(-2.0F, Operation.ADDITION)));
        }

        HashMap<Attribute, Map.Entry<Float, AttributeModifier.Operation>> aresa = new HashMap();
        if (!ClientUtility.isConnectedToServer("spookytime")) {
            aresa.putAll(Map.of(Attributes.ARMOR, Map.entry(-2.0F, Operation.ADDITION), Attributes.ATTACK_DAMAGE, Map.entry(6.0F, Operation.ADDITION), Attributes.MAX_HEALTH, Map.entry(-2.0F, Operation.ADDITION)));
        }

        List<Enchant> krushhelmet = new ArrayList(Arrays.asList(new Enchant(Enchantments.AQUA_AFFINITY, -1), new Enchant(Enchantments.BLAST_PROTECTION, 5), new Enchant(Enchantments.FIRE_PROTECTION, 5), new Enchant(Enchantments.MENDING, -1), new Enchant(Enchantments.PROJECTILE_PROTECTION, 5), new Enchant(Enchantments.PROTECTION, 5), new Enchant(Enchantments.RESPIRATION, 3), new Enchant(Enchantments.UNBREAKING, 5)));
        List<Enchant> krushChestplateEnchants = new ArrayList(Arrays.asList(new Enchant(Enchantments.BLAST_PROTECTION, 5), new Enchant(Enchantments.FIRE_PROTECTION, 5), new Enchant(Enchantments.MENDING, -1), new Enchant(Enchantments.PROJECTILE_PROTECTION, 5), new Enchant(Enchantments.PROTECTION, 5), new Enchant(Enchantments.UNBREAKING, 5)));
        List<Enchant> krushLegginsEnchants = new ArrayList(Arrays.asList(new Enchant(Enchantments.BLAST_PROTECTION, 5), new Enchant(Enchantments.FIRE_PROTECTION, 5), new Enchant(Enchantments.MENDING, -1), new Enchant(Enchantments.PROJECTILE_PROTECTION, 5), new Enchant(Enchantments.PROTECTION, 5), new Enchant(Enchantments.UNBREAKING, 5)));
        List<Enchant> krushBootsEnchants = new ArrayList(Arrays.asList(new Enchant(Enchantments.BLAST_PROTECTION, 5), new Enchant(Enchantments.DEPTH_STRIDER, 3), new Enchant(Enchantments.FEATHER_FALLING, 4), new Enchant(Enchantments.FIRE_PROTECTION, 5), new Enchant(Enchantments.MENDING, -1), new Enchant(Enchantments.PROJECTILE_PROTECTION, 5), new Enchant(Enchantments.PROTECTION, 5), new Enchant(Enchantments.SOUL_SPEED, 3), new Enchant(Enchantments.UNBREAKING, 5)));
        List<Enchant> krushSwordEnchants = new ArrayList(Arrays.asList(new Enchant(Enchantments.BANE_OF_ARTHROPODS, 7), new Enchant(Enchantments.FIRE_ASPECT, 2), new Enchant(Enchantments.LOOTING, 5), new Enchant(Enchantments.MENDING, -1), new Enchant(Enchantments.SHARPNESS, 7), new Enchant(Enchantments.SMITE, 7), new Enchant(Enchantments.SWEEPING, 3), new Enchant(Enchantments.UNBREAKING, 5), new Enchant("Яд", 3), new Enchant("Опытный", 3), new Enchant("Вампиризм", 2), new Enchant("Окисление", 2), new Enchant("Детекция", 3)));
        List<Enchant> krushTrebEnchants = new ArrayList(Arrays.asList(new Enchant(Enchantments.CHANNELING, -1), new Enchant(Enchantments.FIRE_ASPECT, 2), new Enchant(Enchantments.IMPALING, 5), new Enchant(Enchantments.LOYALTY, 3), new Enchant(Enchantments.MENDING, -1), new Enchant(Enchantments.SHARPNESS, 7), new Enchant(Enchantments.UNBREAKING, 5), new Enchant("Яд", 3), new Enchant("Опытный", 3), new Enchant("Вампиризм", 2), new Enchant("Окисление", 2), new Enchant("Детекция", 3), new Enchant("Ступор", 3), new Enchant("Скаут", 3), new Enchant("Притяжение", 2)));
        List<Enchant> krushArbEnchants = new ArrayList(Arrays.asList(new Enchant(Enchantments.MENDING, -1), new Enchant(Enchantments.MULTISHOT, -1), new Enchant(Enchantments.PIERCING, 5), new Enchant(Enchantments.QUICK_CHARGE, 3), new Enchant(Enchantments.UNBREAKING, 3)));
        List<Enchant> krushPickaxeEnchants = new ArrayList(Arrays.asList(new Enchant(Enchantments.EFFICIENCY, 10), new Enchant(Enchantments.FORTUNE, 5), new Enchant(Enchantments.MENDING, -1), new Enchant(Enchantments.UNBREAKING, 5), new Enchant("Бульдозер", 2), new Enchant("Авто-плавка", -1), new Enchant("Опытный", -3), new Enchant("Пингер", -1), new Enchant("Магнит", -1), new Enchant("Паутина", -1)));
        List<Enchant> krushElytraEnchants = new ArrayList(Arrays.asList(new Enchant(Enchantments.MENDING, -1), new Enchant(Enchantments.UNBREAKING, 5)));
        new ArrayList(Arrays.asList(new Enchant(Enchantments.MENDING, -1)));
        this.list.addAll(List.of(
                new AutoBuyItem("Шлем Крушителя", 0, Items.NETHERITE_HELMET, krushhelmet, null, ""),
                new AutoBuyItem("  Нагрудник Крушителя", 0, Items.NETHERITE_CHESTPLATE, krushChestplateEnchants, null, ""),
                new AutoBuyItem("Поножи Крушителя", 0, Items.NETHERITE_LEGGINGS, krushLegginsEnchants, null, ""),
                new AutoBuyItem("Ботинки Крушителя", 0, Items.NETHERITE_BOOTS, krushBootsEnchants, null, ""),
                new AutoBuyItem("Меч Крушителя", 0, Items.NETHERITE_SWORD, krushSwordEnchants, null, ""),
                new AutoBuyItem("Трезубец Крушителя", 0, Items.TRIDENT, krushTrebEnchants, null, ""),
                new AutoBuyItem("Арбалет Крушителя", 0, Items.CROSSBOW, krushArbEnchants, null, ""),
                new AutoBuyItem("Кирка Крушителя", 0, Items.NETHERITE_PICKAXE, krushPickaxeEnchants, null, ""),
                new AutoBuyItem("Талисман Карателя", 0, Items.TOTEM_OF_UNDYING, karatel, "attribute-item-tkaratela", "1"),
                new AutoBuyItem("Талисман Крушителя", 0, Items.TOTEM_OF_UNDYING, krush, "attribute-item-tkryshitela", "1"),
                new AutoBuyItem("Талисман Ярости", 0, Items.TOTEM_OF_UNDYING, yarosti, "attribute-item-tyarosti", "1"),
                new AutoBuyItem("Талисман Тирана", 0, Items.TOTEM_OF_UNDYING, tirana, "attribute-item-ttirana", "1"),
                new AutoBuyItem("Талисман Раздора", 0, Items.TOTEM_OF_UNDYING, razdora, "attribute-item-trazdora", "1"),
                new AutoBuyItem("Талисман Мрака", 0, Items.TOTEM_OF_UNDYING, mraka, "attribute-item-tmraka", "1"),
                new AutoBuyItem("Талисман Демона", 0, Items.TOTEM_OF_UNDYING, demona, "attribute-item-tdemona", "1"),
                new AutoBuyItem("Талисман Вихря", 0, Items.TOTEM_OF_UNDYING, vixrya, "attribute-item-tvixrya", "1"),
                new AutoBuyItem("Тотем бессмертия", 0, Items.TOTEM_OF_UNDYING),
                new AutoBuyItem("Сфера Эрида", 0, Items.PLAYER_HEAD, erida, "attribute-item-serida", "ewogICJ0aW1lc3RhbXAiIDogMTc1MDM0Mzg2MTE4NywKICAicHJvZmlsZUlkIiA6ICJlZGUyYzdhMGFjNjM0MTNiYjA5ZDNmMGJlZTllYzhlYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJ0aGVEZXZKYWRlIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzZlNGUyZjEwNDdmM2VjNmU5ZTQ1OTE4NDczOWUzM2I3YzFmYzYzYWQ4MjAyYmRhYjlmMDI0NTA4YWRkMjNlNWIiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ=="),
                new AutoBuyItem("Сфера Хаоса", 0, Items.PLAYER_HEAD, xaosa, "attribute-item-sxaosa", "ewogICJ0aW1lc3RhbXAiIDogMTc1MDI3ODY0MTkwMCwKICAicHJvZmlsZUlkIiA6ICIxNzRjZmRiNGEzY2I0M2I1YmZjZGU0MjRjM2JiMmM2ZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJtYXJhZWwxOCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9lN2E3YWU3Y2RjZjYxNmU4YjdhNDIyMWE2MjFiMjQzNTc1M2M2MGVkNmEyNThlYTA2MGRhZTMwMDJmZmU5ZTI4IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0="),
                new AutoBuyItem("Сфера Титана", 0, Items.PLAYER_HEAD, titana, "attribute-item-stitana", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODFlOTY5ODQ1OGI3ODQxYzk2YWU0ZjI0ZWM4NGFlMDE3MjQxMDA2NDFjNTY0ZTJhN2IxODVmNDA2ZThlZDIzIn19fQ=="),
                new AutoBuyItem("Сфера Сатира", 0, Items.PLAYER_HEAD, satira, "attribute-item-ssatira", "ewogICJ0aW1lc3RhbXAiIDogMTc1MDI3ODYwODUyOCwKICAicHJvZmlsZUlkIiA6ICJkMTQ4NjFiM2UwZmM0Njk5OTFlMTcyNTllMzdiZjZhZCIsCiAgInByb2ZpbGVOYW1lIiA6ICJyYXhpdG9jbCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83NzFhOWE0OThiNGZhNWVjNDkzNjJmOWJjODhlZGE0ZjUyYjA0ZGU0OWQ3NWFhM2NhMzMyYTFmZWExYWEwZTU3IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0="),
                new AutoBuyItem("Сфера Икара", 0, Items.PLAYER_HEAD, ikara, "attribute-item-sikara", "ewogICJ0aW1lc3RhbXAiIDogMTc1MDI3ODU4MjQ5MSwKICAicHJvZmlsZUlkIiA6ICJhZWNkODIxZTQyYzE0ZDJlOThmNTA1OTg1MWI5OWMzNyIsCiAgInByb2ZpbGVOYW1lIiA6ICJSb2RyaVgyMDc1IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2M2ODAzZTZkNTY2N2EyZDYxMDYyOGJjM2IzMmY4NjNjZGE0OTVjNDY1NjE2ZGU2NTVjYjMyOTkzM2I2MWFmNzciLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ=="),
                new AutoBuyItem("Сфера Гидры", 0, Items.PLAYER_HEAD, gidry, "attribute-item-sgidry", "ewogICJ0aW1lc3RhbXAiIDogMTc1MDI3ODUzMjE4MywKICAicHJvZmlsZUlkIiA6ICI1OGZmZWI5NTMxNGQ0ODcwYTQwYjVjYjQyZDRlYTU5OCIsCiAgInByb2ZpbGVOYW1lIiA6ICJTa2luREJuZXQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2UzYzExOGQ2OTZkOTEwZTU0ZGUwMmNhNGQ4MDc1NDNmOWIxOGMwMDhjOTgzOGQyZmY2OTM3NzYyMmZiMWQzMiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9"),
                new AutoBuyItem("Сфера Бестии", 0, Items.PLAYER_HEAD, bestii, "attribute-item-sbestii", "ewogICJ0aW1lc3RhbXAiIDogMTc1MDM0MzgzNDkzMCwKICAicHJvZmlsZUlkIiA6ICI1MzUzNWIxN2M0ZDY0NWQ0YWUwY2U2ZjM4Zjk0NTFjYSIsCiAgInByb2ZpbGVOYW1lIiA6ICJVYml2aXMiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTQxMWFjMTczODFiOWZjZTliYWIzYzcyYWZkYjdmMTk4NTcwZGFmNDczMmJkODExZDMxYzIyN2Q4MGZhMzliMSIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9"),
                new AutoBuyItem("Сфера Ареса", 0, Items.PLAYER_HEAD, aresa, "attribute-item-saresa", "ewogICJ0aW1lc3RhbXAiIDogMTc1MDM0Mzc3NDI1NSwKICAicHJvZmlsZUlkIiA6ICJhYWMxYjA2OWNkMjE0NWE2ODNlNzQxNzE4MDcxMGU4MiIsCiAgInByb2ZpbGVOYW1lIiA6ICJqdXNhbXUiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzE2YWRjNmJhZmNiNTdmZDcwN2RlZTdkZDZhNzM2ZmUxMjY3MTFkNTNhMWZkNmNlNzg5ZGE0MWIzYmUxM2YyYSIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9"),
                new AutoBuyItem("Пузырёк опыта", 0, Items.EXPERIENCE_BOTTLE),
                new AutoBuyItem("Чарка", 0, Items.ENCHANTED_GOLDEN_APPLE),
                new AutoBuyItem("Золотое яблоко", 0, Items.GOLDEN_APPLE),
                new AutoBuyItem("Яблоко", 0, Items.APPLE),
                new AutoBuyItem("Перка", 0, Items.ENDER_PEARL),
                new AutoBuyItem("Незеритовый слиток", 0, Items.NETHERITE_INGOT),
                new AutoBuyItem("Голова дракона", 0, Items.DRAGON_HEAD),
                new AutoBuyItem("Яйцо призыва крестьянина", 0, Items.VILLAGER_SPAWN_EGG),
                new AutoBuyItem("Яйцо зомби-крестьянина", 0, Items.ZOMBIE_VILLAGER_SPAWN_EGG),
                new AutoBuyItem("Элитры Крушителя", 0, Items.ELYTRA, krushElytraEnchants, null, ""),
                new AutoBuyItem("Элитры", 0, Items.ELYTRA),
                new AutoBuyItem("Золотая морковь", 0, Items.GOLDEN_CARROT),
                new AutoBuyItem("Шалкер", 0, Items.SHULKER_BOX),
                new AutoBuyItem("Маяк", 0, Items.BEACON),
                new AutoBuyItem("Спавнер", 0, Items.SPAWNER),
                new AutoBuyItem("Порох", 0, Items.GUNPOWDER),
                new AutoBuyItem("Проклятая душа", 0, Items.SOUL_LANTERN, "soul-currency", ""),
                new AutoBuyItem("Трапка", 0, Items.NETHERITE_SCRAP, "schematic-item-trap", ""),
                new AutoBuyItem("Дезориентация", 0, Items.ENDER_EYE, "effect-item-diz", ""),
                new AutoBuyItem("Явная пыль", 0, Items.SUGAR, "effect-item-dust", ""),
                new AutoBuyItem("Пласт", 0, Items.DRIED_KELP, "schematic-item-plast", ""),
                new AutoBuyItem("Божья аура", 0, Items.PHANTOM_MEMBRANE, "effect-item-god", ""),

                new AutoBuyItem("Снотворное", 0, Items.SPLASH_POTION, Arrays.asList(
                        new PotionEffectMatcher(18, 1, 90),
                        new PotionEffectMatcher(4, 0, 10),
                        new PotionEffectMatcher(20, 2, 90),
                        new PotionEffectMatcher(15, 0, 10))),

                new AutoBuyItem("Зелье Радиации", 0, Items.SPLASH_POTION, Arrays.asList(
                        new PotionEffectMatcher(19, 0, 20),
                        new PotionEffectMatcher(20, 0, 20),
                        new PotionEffectMatcher(2, 2, 20),
                        new PotionEffectMatcher(17, 4, 20),
                        new PotionEffectMatcher(24, 0, 30))),

                new AutoBuyItem("Святая вода", 0, Items.SPLASH_POTION, Arrays.asList(
                        new PotionEffectMatcher(10, 2, 60),
                        new PotionEffectMatcher(14, 1, 600),
                        new PotionEffectMatcher(6, 1, -1))),

                new AutoBuyItem("Зелье Палладина", 0, Items.SPLASH_POTION, Arrays.asList(
                        new PotionEffectMatcher(11, 0, 600),
                        new PotionEffectMatcher(12, 0, 600),
                        new PotionEffectMatcher(22, 2, 60),
                        new PotionEffectMatcher(14, 2, 900))),

                new AutoBuyItem("Зелье Гнева", 0, Items.SPLASH_POTION, Arrays.asList(
                        new PotionEffectMatcher(5, 4, 30),
                        new PotionEffectMatcher(2, 3, 30))),

                new AutoBuyItem("Зелье Ассасина", 0, Items.SPLASH_POTION, Arrays.asList(
                        new PotionEffectMatcher(5, 3, 60),
                        new PotionEffectMatcher(1, 2, 300),
                        new PotionEffectMatcher(3, 0, 60),
                        new PotionEffectMatcher(7, 1, -1))),

                new AutoBuyItem("Молот Тора", 0, Items.NETHERITE_PICKAXE, "radius-item-mega-buldozer", ""),
                new AutoBuyItem("Божье касание", 0, Items.GOLDEN_PICKAXE, "spawner-item-spawner-break", ""),
                new AutoBuyItem("Книга починка", 0, Items.ENCHANTED_BOOK, new HashMap(), (String)null, "", repairBookEnchants, new ArrayList()),
                new AutoBuyItem("Отмычка к сферам", 0, Items.TRIPWIRE_HOOK, "spheres", ""),
                new AutoBuyItem("Обычный мист", 0, Items.CAMPFIRE, "MILD", ""),
                new AutoBuyItem("Богатый мист", 0, Items.CAMPFIRE, "WEAK", ""),
                new AutoBuyItem("Легендарный мист", 0, Items.CAMPFIRE, "1", ""),
                new AutoBuyItem("Прогрузчик чанков 1x1", 0, Items.STRUCTURE_BLOCK, "executable-block-chunker-1", ""),
                new AutoBuyItem("Прогрузчик чанков 2x2", 0, Items.STRUCTURE_BLOCK, "executable-block-chunker-2", ""),
                new AutoBuyItem("Прогрузчик чанков 3x3", 0, Items.STRUCTURE_BLOCK, "executable-block-chunker-3", ""),
                new AutoBuyItem("Дамагер", 0, Items.JIGSAW, "executable-block-damager", ""),
                new AutoBuyItem("Таер вайт", 0, Items.TNT, "tnt-item-white", ""),
                new AutoBuyItem("Таер блэк", 0, Items.TNT, "tnt-item-black", "")));
    }

    private void rebuildMap() {
        itemMap.clear();
        for (AutoBuyItem item : list) {
            itemMap.computeIfAbsent(item.itemStack.getItem(), k -> new ArrayList<>()).add(item);
        }
    }

    private List<AutoBuyItem> getCandidates(ItemStack stack) {
        return itemMap.getOrDefault(stack.getItem(), Collections.emptyList());
    }
    public AutoBuyItem isNeedToBuyEnchanted(ItemStack stack) {
        if (stack.getItem() != Items.ENCHANTED_BOOK && !stack.isEnchanted()) {
            return null;
        }

        if (stack.getItem() == Items.ENCHANTED_BOOK) {
            Map<Enchantment, Integer> bookEnchants = EnchantmentHelper.getEnchantments(stack);
            if (bookEnchants.isEmpty()) {
                return null;
            }
            for(AutoBuyItem registeredItem : this.list) {
                if (!registeredItem.parsingEnabled) continue;
                if (registeredItem.itemStack.getItem() == Items.ENCHANTED_BOOK && registeredItem.enchants != null && !registeredItem.enchants.isEmpty()) {
                    boolean allMatch = true;

                    for(Enchant required : registeredItem.enchants) {
                        boolean hasEnchant = false;

                        for(Map.Entry<Enchantment, Integer> existing : bookEnchants.entrySet()) {
                            if (existing.getKey() == required.enchantment && (Integer)existing.getValue() >= required.level) {
                                hasEnchant = true;
                                break;
                            }
                        }

                        if (!hasEnchant) {
                            allMatch = false;
                            break;
                        }
                    }

                    if (allMatch) {
                        int totalPrice = AutoBuyUtil.getPrice(stack);
                        if (totalPrice == -1) continue;
                        int pricePerItem = totalPrice / stack.getCount();
                        if (registeredItem.buyPrice >= pricePerItem) {
                            return registeredItem;
                        }
                    }
                }
            }
            return null;
        }
        
        Map<Enchantment, Integer> itemEnchants = EnchantmentHelper.getEnchantments(stack);
        if (itemEnchants.containsKey(Enchantments.THORNS)) return null;
        
        for(AutoBuyItem registeredItem : this.list) {
            if (!registeredItem.parsingEnabled) continue;
            if (registeredItem.itemStack.getItem() != stack.getItem()) continue;
            if (registeredItem.enchants == null || registeredItem.enchants.isEmpty()) continue;
            
            boolean allMatch = true;
            for(Enchant required : registeredItem.enchants) {
                boolean hasEnchant = false;
                for(Map.Entry<Enchantment, Integer> existing : itemEnchants.entrySet()) {
                    if (existing.getKey() == required.enchantment && (Integer)existing.getValue() >= required.level) {
                        hasEnchant = true;
                        break;
                    }
                }
                if (!hasEnchant) {
                    allMatch = false;
                    break;
                }
            }
            if (allMatch) {
                int totalPrice = AutoBuyUtil.getPrice(stack);
                if (totalPrice == -1) continue;
                int pricePerItem = totalPrice / stack.getCount();
                if (registeredItem.buyPrice >= pricePerItem) {
                    return registeredItem;
                }
            }
        }
        return null;
    }

    public AutoBuyItem isNeedToBuyPotion(ItemStack stack) {
        List<EffectInstance> itemEffects = PotionUtils.getFullEffectsFromItem(stack);

        for(AutoBuyItem registeredItem : this.list) {
            if (!registeredItem.parsingEnabled) continue;
            if (registeredItem.potionEffects != null && !registeredItem.potionEffects.isEmpty() && registeredItem.itemStack.getItem() == stack.getItem() && itemEffects.size() == registeredItem.potionEffects.size()) {
                boolean allEffectsMatch = true;

                for(PotionEffectMatcher requiredEffect : registeredItem.potionEffects) {
                    boolean foundMatch = false;

                    for(EffectInstance itemEffect : itemEffects) {
                        int id = Effect.getId(itemEffect.getPotion());
                        int amplifier = itemEffect.getAmplifier();
                        int duration = itemEffect.getDuration() / 20;
                        if (id == requiredEffect.id && amplifier == requiredEffect.amplifier) {
                            if (requiredEffect.duration == -1) {
                                foundMatch = true;
                                break;
                            }

                            if (duration == requiredEffect.duration) {
                                foundMatch = true;
                                break;
                            }
                        }
                    }

                    if (!foundMatch) {
                        allEffectsMatch = false;
                        break;
                    }
                }

                if (allEffectsMatch) {
                    int totalPrice = AutoBuyUtil.getPrice(stack);
                    if (totalPrice == -1) continue;
                    int pricePerItem = totalPrice / stack.getCount();
                    if (registeredItem.buyPrice >= pricePerItem) {
                        return registeredItem;
                    }
                }
            }
        }

        return null;
    }

    public void clear() {
        this.list.clear();
        this.addItems();
        this.rebuildMap();
    }

    public AutoBuyItem isNeedToBuy(ItemStack stack) {
        for(AutoBuyItem registeredItem : getCandidates(stack)) {
            if (!registeredItem.parsingEnabled) continue;
            if ((registeredItem.potionEffects == null || registeredItem.potionEffects.isEmpty()) && (registeredItem.attributes == null || registeredItem.attributes.isEmpty()) && (registeredItem.spookyItemType == null || registeredItem.spookyItemType.isEmpty()) && (registeredItem.enchants == null || registeredItem.enchants.isEmpty())) {
                int totalPrice = AutoBuyUtil.getPrice(stack);
                if (totalPrice == -1) continue;
                int pricePerItem = totalPrice / stack.getCount();
                if (registeredItem.buyPrice >= pricePerItem) {
                    return registeredItem;
                }
            }
        }

        return null;
    }

    public AutoBuyItem isNeedToBuy(ItemStack stack, String spookyItemType) {
        if (spookyItemType == null) {
            return this.isNeedToBuy(stack);
        } else {
            for(AutoBuyItem registeredItem : getCandidates(stack)) {
                if (!registeredItem.parsingEnabled) continue;
                if (registeredItem.spookyItemType != null && spookyItemType.endsWith(registeredItem.spookyItemType)) {
                    int totalPrice = AutoBuyUtil.getPrice(stack);
                    if (totalPrice == -1) continue;
                    int pricePerItem = totalPrice / stack.getCount();
                    if (registeredItem.buyPrice >= pricePerItem) {
                        return registeredItem;
                    }
                }
            }

            return null;
        }
    }

    public AutoBuyItem isNeedToBuy(ItemStack stack, HashMap<Attribute, Map.Entry<Float, AttributeModifier.Operation>> attributes) {
        for(AutoBuyItem registeredItem : getCandidates(stack)) {
            if (!registeredItem.parsingEnabled) continue;
            if (registeredItem.attributes != null && registeredItem.attributes.equals(attributes)) {
                int totalPrice = AutoBuyUtil.getPrice(stack);
                if (totalPrice == -1) continue;
                int pricePerItem = totalPrice / stack.getCount();
                if (registeredItem.buyPrice >= pricePerItem) {
                    return registeredItem;
                }
            }
        }

        return null;
    }
}




