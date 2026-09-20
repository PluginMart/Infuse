package com.catadmirer.infuseSMP.expansions;

import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.effects.InfuseEffect;
import com.catadmirer.infuseSMP.managers.CooldownManager;
import com.catadmirer.infuseSMP.util.MessageUtil;
import java.util.UUID;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public class InfusePlaceholders extends PlaceholderExpansion {
    private final Infuse plugin;

    public InfusePlaceholders(Infuse plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NonNull String getAuthor() {
        return "catadmirer";
    }

    @Override
    public @NonNull String getIdentifier() {
        return "infuse";
    }

    @Override
    public @NonNull String getVersion() {
        return plugin.getVersion();
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        UUID uuid = player.getUniqueId();

        return switch (params.toLowerCase()) {
            case "first_time" -> getTime(player, "1");
            case "second_time" -> getTime(player, "2");
            case "first_effect_raw" -> getEffectRaw(player, "1");
            case "second_effect_raw" -> getEffectRaw(player, "2");
            case "first_effect_name" -> getEffectName(player, "1");
            case "second_effect_name" -> getEffectName(player, "2");
            case "controls" -> plugin.getDataManager().getControlMode(player);
            default -> null;
        };
    }

    public String getTime(OfflinePlayer player, String slot) {
        UUID uuid = player.getUniqueId();
        InfuseEffect effect = plugin.getDataManager().getEffect(player, slot);
        if (effect == null) return "";
        String key = effect.getPlainKey();
        if (CooldownManager.isEffectActive(uuid, key)) {
            long timeLeft = CooldownManager.getEffectTimeLeft(uuid, key) / 1000;
            return "<#" + Integer.toHexString(effect.getPotionColor().getRGB() & 0xFFFFFF) + ">" + MessageUtil.formatTime(timeLeft);
        } else if (CooldownManager.isOnCooldown(uuid, key)) {
            long timeLeft = CooldownManager.getCooldownTimeLeft(uuid, key) / 1000;
            return "<white>" + MessageUtil.formatTime(timeLeft);
        } else {
            return "";
        }
    }

    public String getEffectRaw(OfflinePlayer player, String slot) {
        InfuseEffect effect = plugin.getDataManager().getEffect(player, slot);
        if (effect== null) return "";

        return PlainTextComponentSerializer.plainText().serialize(effect.getName().toComponent());
    }

    public String getEffectName(OfflinePlayer player, String slot) {
        InfuseEffect effect = plugin.getDataManager().getEffect(player, slot);
        if (effect == null) return "";

        return effect.getName().toString();
    }
}
