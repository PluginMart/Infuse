package com.catadmirer.infuseSMP.managers;

import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.effects.InfuseEffect;
import com.catadmirer.infuseSMP.util.MessageUtil;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

public class ActionBarUpdater extends BukkitRunnable {
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Infuse plugin;

    public ActionBarUpdater(Infuse plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            UUID uuid = player.getUniqueId();

            // Composing the action bar
            String key;
            InfuseEffect effect;

            String placeholder = plugin.getMainConfig().emptyEffectIcon() ? "\ue901\ue904" : "";

            String leftPad = "";
            String leftTime = "";
            String leftEmoji = placeholder;
            String rightEmoji = placeholder;
            String rightTime = "";
            String rightPad = "";

            // Loading info for the first effect
            effect = plugin.getDataManager().getEffect(player, "1");
            if (effect != null) {
                leftEmoji = effect.getIcon() + "\ue904";

                key = effect.getPlainKey();
                if (CooldownManager.isEffectActive(uuid, key)) {
                    leftEmoji = String.valueOf(effect.getActiveIcon());

                    long timeLeft = CooldownManager.getEffectTimeLeft(uuid, key) / 1000L;
                    leftTime = "<#" + Integer.toHexString(effect.getPotionColor().getRGB() & 0xFFFFFF) + ">" + MessageUtil.formatTime(timeLeft);
                    rightPad = getSpaceTimeStr(mm.stripTags(leftTime));
                } else if (CooldownManager.isOnCooldown(uuid, key)) {
                    long timeLeft = CooldownManager.getCooldownTimeLeft(uuid, key) / 1000L;
                    leftTime = MessageUtil.formatTime(timeLeft);
                    rightPad = getSpaceTimeStr(mm.stripTags(leftTime));
                }
            }

            // Loading info for the second effect
            effect = plugin.getDataManager().getEffect(player, "2");
            if (effect != null) {
                rightEmoji = effect.getIcon() + "\ue904";

                key = effect.getPlainKey();
                if (CooldownManager.isEffectActive(uuid, key)) {
                    rightEmoji = String.valueOf(effect.getActiveIcon());

                    long timeLeft = CooldownManager.getEffectTimeLeft(uuid, key) / 1000L;
                    rightTime = "<#" + Integer.toHexString(effect.getPotionColor().getRGB() & 0xFFFFFF) + ">" + MessageUtil.formatTime(timeLeft);
                    leftPad = getSpaceTimeStr(mm.stripTags(rightTime));
                } else if (CooldownManager.isOnCooldown(uuid, key)) {
                    long timeLeft = CooldownManager.getCooldownTimeLeft(uuid, key) / 1000L;
                    rightTime = MessageUtil.formatTime(timeLeft);
                    leftPad = getSpaceTimeStr(mm.stripTags(rightTime));
                }
            }

            // Sending the action bar
            player.sendActionBar(mm.deserialize(String.format("<b>%s%s</b> <white>%s %s <b>%s%s</b>", leftPad, leftTime, leftEmoji, rightEmoji, rightTime, rightPad)));
        });
    }

    public String getSpaceTimeStr(String timeStr) {
        return "\ue905".repeat(timeStr.length() - 1) + (timeStr.contains(":") ? "\ue904" : "\ue905");
    }

    public void run2() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            UUID uuid = player.getUniqueId();

            // Composing the action bar
            String key;
            InfuseEffect effect;

            String placeholder = plugin.getMainConfig().emptyEffectIcon() ? "\uE901" : "";

            String lSide;
            // Loading info for the first effect
            effect = plugin.getDataManager().getEffect(player, "1");
            if (effect == null) {
                lSide = " " + placeholder + "\ue904";
            } else {
                key = effect.getPlainKey();
                if (CooldownManager.isEffectActive(uuid, key)) {
                    long timeLeft = CooldownManager.getEffectTimeLeft(uuid, key) / 1000L;
                    lSide = "<b><#" + Integer.toHexString(effect.getPotionColor().getRGB() & 0xFFFFFF) + ">" + MessageUtil.formatTime(timeLeft) + "</b><white> \ue905" + effect.getActiveIcon();
                } else if (CooldownManager.isOnCooldown(uuid, key)) {
                    long timeLeft = CooldownManager.getCooldownTimeLeft(uuid, key) / 1000L;
                    lSide = "<b><white>" + MessageUtil.formatTime(timeLeft) + "</b> " + effect.getIcon() + "\ue904";
                } else {
                    lSide = " " + effect.getIcon() + "\ue904";
                }
            }

            // Loading info for the second effect
            String rSide;
            effect = plugin.getDataManager().getEffect(player, "2");
            if (effect == null) {
                rSide = "<white>" + placeholder + "\ue904 ";
            } else {
                key = effect.getPlainKey();
                if (CooldownManager.isEffectActive(uuid, key)) {
                    long timeLeft = CooldownManager.getEffectTimeLeft(uuid, key) / 1000L;
                    rSide = "<white>" + effect.getActiveIcon() + "\ue905 <#" + Integer.toHexString(effect.getPotionColor().getRGB() & 0xFFFFFF) + "><b>" + MessageUtil.formatTime(timeLeft) + "</b>";
                } else if (CooldownManager.isOnCooldown(uuid, key)) {
                    long timeLeft = CooldownManager.getCooldownTimeLeft(uuid, key) / 1000L;
                    rSide = "<white>" + effect.getIcon() + "\ue904 <b>" + MessageUtil.formatTime(timeLeft) + "</b>";
                } else {
                    rSide = "<white>" + effect.getIcon() + "\ue904 ";
                }
            }

            // Making sure both sides are the same length
            int lSize = mm.stripTags(lSide).length();
            int rSize = mm.stripTags(rSide).length();
            Component msg = mm.deserialize(lSide + " " + rSide);

            int diff = lSize - rSize;
            Component spacing = Component.text("\ue904\ue904\ue904\ue904".repeat(Math.abs(diff)));

            if (diff > 0) {
                msg = msg.append(spacing);
            } else {
                msg = spacing.append(msg);
            }

            // Sending the action bar
            player.sendActionBar(msg);
        });
    }
}
