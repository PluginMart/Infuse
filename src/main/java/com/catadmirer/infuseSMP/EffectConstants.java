package com.catadmirer.infuseSMP;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;

import java.awt.Color;

public class EffectConstants {
    public static class Id {
        public static final int EMERALD = 0;
        public static final int ENDER = 1;
        public static final int FEATHER = 2;
        public static final int FIRE = 3;
        public static final int FROST = 4;
        public static final int HASTE = 5;
        public static final int HEART = 6;
        public static final int INVIS = 7;
        public static final int OCEAN = 8;
        public static final int REGEN = 9;
        public static final int SPEED = 10;
        public static final int STRENGTH = 11;
        public static final int THUNDER = 12;
        public static final int APOPHIS = 13;
        public static final int THIEF = 14;
    }

    public static class Keys {
        public static final Key EMERALD = Key.key("infuse", "emerald");
        public static final Key AUG_EMERALD = Key.key("infuse", "aug_emerald");
        public static final Key ENDER = Key.key("infuse", "ender");
        public static final Key AUG_ENDER = Key.key("infuse", "aug_ender");
        public static final Key FEATHER = Key.key("infuse", "feather");
        public static final Key AUG_FEATHER = Key.key("infuse", "aug_feather");
        public static final Key FIRE = Key.key("infuse", "fire");
        public static final Key AUG_FIRE = Key.key("infuse", "aug_fire");
        public static final Key FROST = Key.key("infuse", "frost");
        public static final Key AUG_FROST = Key.key("infuse", "aug_frost");
        public static final Key HASTE = Key.key("infuse", "haste");
        public static final Key AUG_HASTE = Key.key("infuse", "aug_haste");
        public static final Key HEART = Key.key("infuse", "heart");
        public static final Key AUG_HEART = Key.key("infuse", "aug_heart");
        public static final Key INVIS = Key.key("infuse", "invis");
        public static final Key AUG_INVIS = Key.key("infuse", "aug_invis");
        public static final Key OCEAN = Key.key("infuse", "ocean");
        public static final Key AUG_OCEAN = Key.key("infuse", "aug_ocean");
        public static final Key REGEN = Key.key("infuse", "regen");
        public static final Key AUG_REGEN = Key.key("infuse", "aug_regen");
        public static final Key SPEED = Key.key("infuse", "speed");
        public static final Key AUG_SPEED = Key.key("infuse", "aug_speed");
        public static final Key STRENGTH = Key.key("infuse", "strength");
        public static final Key AUG_STRENGTH = Key.key("infuse", "aug_strength");
        public static final Key THUNDER = Key.key("infuse", "thunder");
        public static final Key AUG_THUNDER = Key.key("infuse", "aug_thunder");
        public static final Key APOPHIS = Key.key("infuse", "apophis");
        public static final Key AUG_APOPHIS = Key.key("infuse", "aug_apophis");
        public static final Key THIEF = Key.key("infuse", "thief");
        public static final Key AUG_THIEF = Key.key("infuse", "aug_thief");
    }

    public static class BackgroundColor {
        public static final Material EMERALD = Material.LIME_STAINED_GLASS_PANE;
        public static final Material ENDER = Material.PURPLE_STAINED_GLASS_PANE;
        public static final Material FEATHER = Material.WHITE_STAINED_GLASS_PANE;
        public static final Material FIRE = Material.ORANGE_STAINED_GLASS_PANE;
        public static final Material FROST = Material.LIGHT_BLUE_STAINED_GLASS_PANE;
        public static final Material HASTE = Material.ORANGE_STAINED_GLASS_PANE;
        public static final Material HEART = Material.RED_STAINED_GLASS_PANE;
        public static final Material INVIS = Material.LIGHT_GRAY_STAINED_GLASS_PANE;
        public static final Material OCEAN = Material.BLUE_STAINED_GLASS_PANE;
        public static final Material REGEN = Material.RED_STAINED_GLASS_PANE;
        public static final Material SPEED = Material.LIGHT_BLUE_STAINED_GLASS_PANE;
        public static final Material STRENGTH = Material.RED_STAINED_GLASS_PANE;
        public static final Material THUNDER = Material.YELLOW_STAINED_GLASS_PANE;
        public static final Material APOPHIS = Material.MAGENTA_STAINED_GLASS_PANE;
        public static final Material THIEF = Material.RED_STAINED_GLASS_PANE;
    }

    public static class PotionColor {
        public static final Color EMERALD = Color.GREEN;
        public static final Color ENDER = new Color(0x800080);
        public static final Color FEATHER = new Color(0xBEA3CA);
        public static final Color FIRE = new Color(0xEE5522);
        public static final Color FROST = new Color(0x55FFFF);
        public static final Color HASTE = new Color(0xFFCC33);
        public static final Color HEART = Color.RED;
        public static final Color INVIS = new Color(0xAA00AA);
        public static final Color OCEAN = new Color(0x0066FF);
        public static final Color REGEN = new Color(0xFF5555);
        public static final Color SPEED = new Color(0xEEBB77);
        public static final Color STRENGTH = new Color(0x800000);
        public static final Color THUNDER = Color.YELLOW;
        public static final Color APOPHIS = new Color(0x440044);
        public static final Color THIEF = new Color(0xAA0000);
    }

    public static class RitualColor {
        public static final BossBar.Color EMERALD = BossBar.Color.GREEN;
        public static final BossBar.Color ENDER = BossBar.Color.PURPLE;
        public static final BossBar.Color FEATHER = BossBar.Color.WHITE;
        public static final BossBar.Color FIRE = BossBar.Color.RED;
        public static final BossBar.Color FROST = BossBar.Color.BLUE;
        public static final BossBar.Color HASTE = BossBar.Color.YELLOW;
        public static final BossBar.Color HEART = BossBar.Color.RED;
        public static final BossBar.Color INVIS = BossBar.Color.PURPLE;
        public static final BossBar.Color OCEAN = BossBar.Color.BLUE;
        public static final BossBar.Color REGEN = BossBar.Color.PINK;
        public static final BossBar.Color SPEED = BossBar.Color.YELLOW;
        public static final BossBar.Color STRENGTH = BossBar.Color.RED;
        public static final BossBar.Color THUNDER = BossBar.Color.RED;
        public static final BossBar.Color APOPHIS = BossBar.Color.PURPLE;
        public static final BossBar.Color THIEF = BossBar.Color.YELLOW;
    }
}
