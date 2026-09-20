package com.catadmirer.infuseSMP;

import java.util.stream.Stream;

import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.TagPattern;

import java.util.ArrayList;
import java.util.List;

public class Message {
    // Text serializers
    public static final MiniMessage mm = MiniMessage.miniMessage();
    public static final MessageComponentSerializer mcs = MessageComponentSerializer.message();

    private static final MessageTranslator translator = new MessageTranslator();

    private String message;
    private final List<String> placeholders;

    public Message(MessageType messageType) {
        this.message = translator.translate(messageType.name().toLowerCase());
        this.placeholders = new ArrayList<>(messageType.placeholders);
    }

    public void applyPlaceholder(@TagPattern String placeholder, Message value) {
        applyPlaceholder(placeholder, mm.serialize(value.toComponent()));
    }

    public void applyPlaceholder(@TagPattern String placeholder, Component component) {
        applyPlaceholder(placeholder, mm.serialize(component));
    }

    public void applyPlaceholder(@TagPattern String placeholder, Object value) {
        placeholders.remove(placeholder);

        this.message = message.replace("%" + placeholder + "%", String.valueOf(value));
    }

    public List<Component> toComponentList() {
        if (!placeholders.isEmpty()) {
            throw new IllegalStateException("Not all placeholders have been registered.");
        }

        return Stream.of(message.split("\n"))
            .map(e -> "<i:false>" + e)
            .map(mm::deserialize).toList();
    }

    public Component toComponent() {
        if (!placeholders.isEmpty()) {
            throw new IllegalStateException("Not all placeholders have been registered.");
        }

        return mm.deserialize("<i:false>" + message);
    }

    public String toString() {
        if (!placeholders.isEmpty()) {
            throw new IllegalStateException("Not all placeholders have been registered.");
        }

        return mm.stripTags(message);
    }

    public List<String> toStringList() {
        if (!placeholders.isEmpty()) {
            throw new IllegalStateException("Not all placeholders have been registered.");
        }

        return List.of(mm.stripTags(message).split("\n"));
    }

    /**
     * Helper function that allows minimessage translation for an arbitrary string.
     *
     * @param message A minimessage string to translate
     *
     * @return The {@link Component} that can be sent to players.
     */
    public static Component toComponent(String message) {
        return mm.deserialize("<i:false>" + message);
    }

    public enum MessageType {
        EFFECT_BROADCAST("player", "item", "x", "y", "z", "dimension"),
        DISCORD_BROADCAST("player", "item", "x", "y", "z", "dimension"),
        EFFECT_FINISHED("item"),
        REGULAR_BROADCAST("item", "x", "y", "z", "dimension"),
        SLOT_EMPTY("slot"),
        EFFECT_NONE_EQUIPPED("slot"),
        WITHDRAW_INVALID,
        TRUST_CONSOLE_USAGE,
        TRUST_INCORRECT_USAGE("label"),
        TRUST_NO_PLAYER,
        TRUST_SELF,
        TRUST_ADDED("target"),
        TRUST_ALREADY_TRUSTED("target"),
        TRUST_REMOVED("target"),
        TRUST_NOT_TRUSTED("target"),
        EFFECT_NO_BREWING,
        DEATH_MESSAGE("victim", "killer"),
        CONTROLS_USAGE,
        CONTROLS_INVALID_PARAM,
        INFUSE_INVALID_PARAM,
        INFUSE_INVALID_SLOT("slot"),
        INFUSE_HELP,
        INFUSE_CONTROLS_USAGE,
        INFUSE_CONTROLS_SUCCESS("control_mode"),
        INFUSE_SETEFFECT_USAGE,
        INFUSE_SETEFFECT_SUCCESS("slot", "player_name", "effect_name"),
        INFUSE_GIVEEFFECT_USAGE,
        INFUSE_GIVEEFFECT_SUCCESS("effect_color", "effect_name"),
        INFUSE_CLEAREFFECTS_USAGE,
        INFUSE_CLEAREFFECTS_SUCCESS("player_name"),
        INFUSE_COOLDOWN_USAGE,
        INFUSE_COOLDOWN_SUCCESS("player_name"),
        CONTROL_MODE_NOTIFY("control_mode"),
        DRAIN_SUCCESS("effect_name"),
        DRAIN_CANCELLED,
        EFFECT_EQUIPPED("effect_name"),
        SWAP_NO_EFFECTS,
        SWAP_SUCCESS,
        THIEF_STEAL("victim", "effect_name"),
        RECIPE_NOT_FOUND,
        RECIPE_DISABLED,
        ERROR_INV_FULL,
        ERROR_NOT_PLAYER,
        ERROR_NOT_OP,
        ERROR_INVALID_COMMAND,
        ERROR_RITUAL_ACTIVE,
        ERROR_TARGET_NOT_FOUND,
        RITUAL_INTERRUPTED("effect_name"),

        // Effect messages
        EMERALD_NAME,
        EMERALD_LORE,
        AUG_EMERALD_NAME,
        AUG_EMERALD_LORE,
        ENDER_NAME,
        ENDER_LORE,
        AUG_ENDER_NAME,
        AUG_ENDER_LORE,
        FEATHER_NAME,
        FEATHER_LORE,
        AUG_FEATHER_NAME,
        AUG_FEATHER_LORE,
        FIRE_NAME,
        FIRE_LORE,
        AUG_FIRE_NAME,
        AUG_FIRE_LORE,
        FROST_NAME,
        FROST_LORE,
        AUG_FROST_NAME,
        AUG_FROST_LORE,
        HASTE_NAME,
        HASTE_LORE,
        AUG_HASTE_NAME,
        AUG_HASTE_LORE,
        HEART_NAME,
        HEART_LORE,
        AUG_HEART_NAME,
        AUG_HEART_LORE,
        INVIS_NAME,
        INVIS_LORE,
        AUG_INVIS_NAME,
        AUG_INVIS_LORE,
        OCEAN_NAME,
        OCEAN_LORE,
        AUG_OCEAN_NAME,
        AUG_OCEAN_LORE,
        REGEN_NAME,
        REGEN_LORE,
        AUG_REGEN_NAME,
        AUG_REGEN_LORE,
        SPEED_NAME,
        SPEED_LORE,
        AUG_SPEED_NAME,
        AUG_SPEED_LORE,
        STRENGTH_NAME,
        STRENGTH_LORE,
        AUG_STRENGTH_NAME,
        AUG_STRENGTH_LORE,
        THUNDER_NAME,
        THUNDER_LORE,
        AUG_THUNDER_NAME,
        AUG_THUNDER_LORE,

        // Extra effect messages
        APOPHIS_NAME,
        APOPHIS_LORE,
        AUG_APOPHIS_NAME,
        AUG_APOPHIS_LORE,
        THIEF_NAME,
        THIEF_LORE,
        AUG_THIEF_NAME,
        AUG_THIEF_LORE;

        public final List<String> placeholders;

        MessageType(String... placeholders) {
            this.placeholders = List.of(placeholders);
        }
    }
}