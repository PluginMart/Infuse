package com.catadmirer.infuseSMP;

import com.catadmirer.infuseSMP.inventories.AugOrRegChooser;
import java.awt.Color;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Material;

public class EffectConstants {
    /**
     * Gets the {@link Material} to use in the background of this effect's {@link AugOrRegChooser} menu.
     *
     * @param effectId The id of the infuse effect.
     *
     * @return the {@link Material} for the effect.
     */
    public static Material menuBackgroundColor(int effectId) {
        return switch (effectId) {
            case EffectIds.EMERALD -> Material.LIME_STAINED_GLASS_PANE;
            case EffectIds.ENDER -> Material.PURPLE_STAINED_GLASS_PANE;
            case EffectIds.FEATHER -> Material.WHITE_STAINED_GLASS_PANE;
            case EffectIds.FIRE,
                 EffectIds.HASTE -> Material.ORANGE_STAINED_GLASS_PANE;
            case EffectIds.FROST,
                 EffectIds.SPEED -> Material.LIGHT_BLUE_STAINED_GLASS_PANE;
            case EffectIds.HEART,
                 EffectIds.REGEN,
                 EffectIds.STRENGTH,
                 EffectIds.THIEF -> Material.RED_STAINED_GLASS_PANE;
            case EffectIds.INVIS -> Material.LIGHT_GRAY_STAINED_GLASS_PANE;
            case EffectIds.OCEAN -> Material.BLUE_STAINED_GLASS_PANE;
            case EffectIds.THUNDER -> Material.YELLOW_STAINED_GLASS_PANE;
            case EffectIds.APOPHIS -> Material.MAGENTA_STAINED_GLASS_PANE;
            default -> null;
        };
    }

    /**
     * Gets the {@link Color} for this effect's potion and related text.
     *
     * @param effectId The id of the infuse effect.
     *
     * @return the {@link Color} for the effect.
     */
    public static Color potionColor(int effectId) {
        return switch (effectId) {
            case EffectIds.EMERALD -> Color.GREEN;
            case EffectIds.ENDER -> new Color(0x800080);
            case EffectIds.FEATHER -> new Color(0xBEA3CA);
            case EffectIds.FIRE -> new Color(0xEE5522);
            case EffectIds.FROST -> new Color(0x55FFFF);
            case EffectIds.HASTE -> new Color(0xFFCC33);
            case EffectIds.HEART -> Color.RED;
            case EffectIds.INVIS -> new Color(0xAA00AA);
            case EffectIds.OCEAN -> new Color(0x0066FF);
            case EffectIds.REGEN -> new Color(0xFF5555);
            case EffectIds.SPEED -> new Color(0xEEBB77);
            case EffectIds.STRENGTH -> new Color(0x800000);
            case EffectIds.THUNDER -> Color.YELLOW;
            case EffectIds.APOPHIS -> new Color(0x440044);
            case EffectIds.THIEF -> new Color(0xAA0000);
            default -> null;
        };
    }

    /**
     * Gets the {@link BossBar.Color} for this effect's ritual.
     *
     * @param effectId The id of the infuse effect.
     *
     * @return the {@link BossBar.Color} for the effect.
     */
    public static BossBar.Color ritualColor(int effectId) {
        return switch (effectId) {
            case EffectIds.APOPHIS,
                 EffectIds.ENDER,
                 EffectIds.INVIS -> BossBar.Color.PURPLE;
            case EffectIds.EMERALD -> BossBar.Color.GREEN;
            case EffectIds.FEATHER -> BossBar.Color.WHITE;
            case EffectIds.FIRE,
                 EffectIds.HEART,
                 EffectIds.STRENGTH,
                 EffectIds.THIEF -> BossBar.Color.RED;
            case EffectIds.FROST,
                 EffectIds.OCEAN -> BossBar.Color.BLUE;
            case EffectIds.HASTE,
                 EffectIds.SPEED,
                 EffectIds.THUNDER -> BossBar.Color.YELLOW;
            case EffectIds.REGEN -> BossBar.Color.PINK;
            default -> null;
        };
    }
}
