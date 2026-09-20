package com.catadmirer.infuseSMP.commands;

import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.Message.MessageType;
import com.catadmirer.infuseSMP.effects.InfuseEffect;
import com.catadmirer.infuseSMP.inventories.EffectChooser;
import com.catadmirer.infuseSMP.inventories.RecipeListGUI;
import com.catadmirer.infuseSMP.managers.CooldownManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

public class InfuseCommand implements CommandExecutor, TabCompleter {
    private final Infuse plugin;

    public InfuseCommand(Infuse plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(new Message(MessageType.INFUSE_INVALID_PARAM).toComponent());
            return true;
        }

        Player target;
        String effectKey;
        InfuseEffect effect;
        Message msg;
        switch (args[0].toLowerCase()) {
            case "gui":
                if (!player.isOp()) {
                    player.sendMessage(new Message(MessageType.ERROR_NOT_OP).toComponent());
                    return true;
                }

                player.openInventory(new EffectChooser(plugin).getInventory());
                break;

            case "reload":
                if (!player.isOp()) {
                    player.sendMessage(new Message(MessageType.ERROR_NOT_OP).toComponent());
                    return true;
                }

                plugin.getMainConfig().load();
                plugin.getRecipeManager().reload();

                player.sendMessage("Infuse configs reloaded");
                break;
            case "recipes":
                player.openInventory(new RecipeListGUI().getInventory());
                break;
            case "giveeffect":
                if (!player.isOp()) {
                    player.sendMessage(new Message(MessageType.ERROR_NOT_OP).toComponent());
                    return true;
                }

                if (args.length != 3) {
                    player.sendMessage(new Message(MessageType.INFUSE_GIVEEFFECT_USAGE).toComponent());
                    return true;
                }

                target = Bukkit.getPlayer(args[1]);
                if (target == null || !target.isOnline()) {
                    player.sendMessage(new Message(MessageType.ERROR_TARGET_NOT_FOUND).toComponent());
                    return true;
                }

                effectKey = args[2].toLowerCase();
                effect = InfuseEffect.fromString(effectKey);
                if (effect == null) {
                    player.sendMessage(new Message(MessageType.INFUSE_INVALID_PARAM).toComponent());
                    return true;
                }

                target.getInventory().addItem(effect.createItem());

                msg = new Message(MessageType.INFUSE_GIVEEFFECT_SUCCESS);
                msg.applyPlaceholder("effect_color", "<#" + Integer.toHexString(effect.getPotionColor().getRGB() & 0xffffff) + ">");
                msg.applyPlaceholder("effect_name", effect.getName());
                target.sendMessage(msg.toComponent());
                break;
            case "seteffect":
                if (!player.isOp()) {
                    player.sendMessage(new Message(MessageType.ERROR_NOT_OP).toComponent());
                    return true;
                }

                if (args.length != 4) {
                    player.sendMessage(new Message(MessageType.INFUSE_SETEFFECT_USAGE).toComponent());
                    return true;
                }

                // Getting the player and making sure they are online.
                target = Bukkit.getPlayer(args[1]);
                if (target == null || !target.isOnline()) {
                    player.sendMessage(new Message(MessageType.ERROR_TARGET_NOT_FOUND).toComponent());
                    return true;
                }

                // Getting the effect key and verifying its integrity.
                effectKey = args[2].toLowerCase();
                effect = InfuseEffect.fromString(effectKey);
                if (effect == null) {
                    player.sendMessage(new Message(MessageType.INFUSE_INVALID_PARAM).toComponent());
                    return true;
                }

                // Getting the slot to put the effect into and validating it.
                String slot = args[3];
                if (!slot.equals("1") && !slot.equals("2")) {
                    msg = new Message(MessageType.INFUSE_INVALID_SLOT);
                    msg.applyPlaceholder("slot", slot);
                    player.sendMessage(msg.toComponent());
                    return true;
                }

                // Setting the effect
                plugin.getEffectManager().setEffect(target, effect, args[3]);
                msg = new Message(MessageType.INFUSE_SETEFFECT_SUCCESS);
                msg.applyPlaceholder("slot", slot);
                msg.applyPlaceholder("player_name", target.getName());
                msg.applyPlaceholder("effect_name", effect.getName());
                player.sendMessage(msg.toComponent());
                break;
            case "cleareffects":
                if (!player.isOp()) {
                    player.sendMessage(new Message(MessageType.ERROR_NOT_OP).toComponent());
                    return true;
                }

                if (args.length != 2) {
                    player.sendMessage(new Message(MessageType.INFUSE_CLEAREFFECTS_USAGE).toComponent());
                    return true;
                }

                // Getting the player and making sure they are online
                target = Bukkit.getPlayer(args[1]);
                if (target == null || !target.isOnline()) {
                    player.sendMessage(new Message(MessageType.ERROR_TARGET_NOT_FOUND).toComponent());
                    return true;
                }

                // Removing the effects from the player
                plugin.getEffectManager().unequipEffect(target, "1");
                plugin.getEffectManager().unequipEffect(target, "2");
                msg = new Message(MessageType.INFUSE_CLEAREFFECTS_SUCCESS);
                msg.applyPlaceholder("player_name", target.getName());
                player.sendMessage(msg.toComponent());
                break;
            case "cooldown":
                if (!player.isOp()) {
                    player.sendMessage(new Message(MessageType.ERROR_NOT_OP).toComponent());
                    return true;
                }

                if (args.length != 2) {
                    player.sendMessage(new Message(MessageType.INFUSE_COOLDOWN_USAGE).toComponent());
                    return true;
                }

                // Getting the player and making sure they are online
                target = Bukkit.getPlayer(args[1]);
                if (target == null || !target.isOnline()) {
                    player.sendMessage(new Message(MessageType.ERROR_TARGET_NOT_FOUND).toComponent());
                    return true;
                }

                // Removing cooldowns from the player
                CooldownManager.removeAllCooldowns(target.getUniqueId());
                msg = new Message(MessageType.INFUSE_COOLDOWN_SUCCESS);
                msg.applyPlaceholder("player_name", target.getName());
                player.sendMessage(msg.toComponent());
                break;
            case "controls":
                if (args.length != 2) {
                    player.sendMessage(new Message(MessageType.INFUSE_CONTROLS_USAGE).toComponent());
                    return true;
                }

                // Getting the control mode and validating the input.
                String choice = args[1].toLowerCase();
                if (!choice.equals("offhand") && !choice.equals("command")) {
                    player.sendMessage(new Message(MessageType.INFUSE_CONTROLS_USAGE).toComponent());
                    return true;
                }

                // Setting the control mode for the user.
                plugin.getDataManager().setControlMode(player, choice);

                // Assigning the permission for offhand use if the user chose offhand mode
                boolean offhandEnabled = choice.equalsIgnoreCase("offhand");
                player.addAttachment(plugin, "ability.use", !offhandEnabled);

                msg = new Message(MessageType.INFUSE_CONTROLS_SUCCESS);
                msg.applyPlaceholder("control_mode", choice);
                player.sendMessage(msg.toComponent());
                break;
            default:
                sender.sendMessage(new Message(MessageType.INFUSE_INVALID_PARAM).toComponent());
                break;
        }

        return true;
    }


    @Override
    public List<String> onTabComplete(@NonNull CommandSender sender, @NonNull Command command, @NonNull String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(Arrays.asList("recipes", "controls"));

            if (sender.isOp()) {
                completions.addAll(Arrays.asList("gui", "reload", "giveEffect", "setEffect", "clearEffects", "cooldown"));
            }

            return completions.stream().filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase())).sorted().toList();
        }

        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "controls":
                    return Stream.of("offhand", "command").filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).toList();
                case "giveeffect":
                case "seteffect":
                case "cleareffects":
                case "cooldown":
                    if (!sender.isOp()) return List.of();
                    return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).toList();
            }
        }

        if (args.length == 3) {
            switch(args[0].toLowerCase()) {
                case "giveeffect":
                case "seteffect":
                    if (!sender.isOp()) return List.of();
                    return InfuseEffect.getRegisteredEffects().values().stream().map(InfuseEffect::toString).filter(key -> key.toLowerCase().startsWith(args[2].toLowerCase())).toList();
            }
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("setEffect") && sender.isOp()) {
            return Stream.of("1", "2").filter(key -> key.toLowerCase().startsWith(args[2].toLowerCase())).toList();
        }

        return List.of();
    }
}
