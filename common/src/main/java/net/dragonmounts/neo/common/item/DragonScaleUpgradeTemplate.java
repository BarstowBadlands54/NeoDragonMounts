package net.dragonmounts.neo.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.SmithingTemplateItem;

import java.util.List;

import static net.dragonmounts.neo.common.DragonMountsShared.makeId;

/// Factory for the Dragon Scale Upgrade smithing template.
///
/// ONE untyped template. The dragon type comes from the scales in the addition slot, so a template
/// pulled from an End chest is never the "wrong" type, adding a dragon type needs no new template
/// item / texture / loot entry, and the End loot table doesn't carry N entries diluting each other.
public final class DragonScaleUpgradeTemplate {
    private static final ChatFormatting TITLE = ChatFormatting.GRAY;
    private static final ChatFormatting BODY = ChatFormatting.BLUE;

    private static final String KEY = "item.neodragonmounts.smithing_template.dragon_scale_upgrade";

    /// Vanilla's empty-slot icons. Reused rather than re-authored: they already ship in the
    /// block atlas, and the smithing screen expects sprites of exactly that footprint.
    private static final ResourceLocation EMPTY_HELMET = ResourceLocation.withDefaultNamespace("item/empty_armor_slot_helmet");
    private static final ResourceLocation EMPTY_CHESTPLATE = ResourceLocation.withDefaultNamespace("item/empty_armor_slot_chestplate");
    private static final ResourceLocation EMPTY_LEGGINGS = ResourceLocation.withDefaultNamespace("item/empty_armor_slot_leggings");
    private static final ResourceLocation EMPTY_BOOTS = ResourceLocation.withDefaultNamespace("item/empty_armor_slot_boots");

    public static ResourceLocation id() {
        return makeId("dragon_scale_upgrade_smithing_template");
    }

    public static SmithingTemplateItem create() {
        return new SmithingTemplateItem(
                Component.translatable(KEY + ".applies_to").withStyle(BODY),
                Component.translatable(KEY + ".ingredients").withStyle(BODY),
                Component.translatable(KEY).withStyle(TITLE),
                Component.translatable(KEY + ".base_slot_description"),
                Component.translatable(KEY + ".additions_slot_description"),
                List.of(EMPTY_HELMET, EMPTY_CHESTPLATE, EMPTY_LEGGINGS, EMPTY_BOOTS),
                // No vanilla "empty dragon scales" sprite exists, so the addition slot shows
                // nothing. Swap in a custom sprite here once the texture is finalised.
                List.of()
        );
    }

    private DragonScaleUpgradeTemplate() {}
}
