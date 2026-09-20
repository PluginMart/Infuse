package com.catadmirer.infuseSMP.commands;

import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.Message.MessageType;
import com.catadmirer.infuseSMP.effects.InfuseEffect;
import com.catadmirer.infuseSMP.util.regions.RegionBlocker;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Abilities implements CommandExecutor {
    private final Infuse plugin;

    public Abilities(Infuse plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(new Message(MessageType.ERROR_NOT_PLAYER).toComponent());
            return true;
        }

        // Finding which slot to activate the spark for.
        String slot;
        if (label.contains("lspark")) {
            slot = "1";
        } else if (label.contains("rspark")) {
            slot = "2";
        } else {
            sender.sendMessage(new Message(MessageType.ERROR_INVALID_COMMAND).toComponent());
            return true;
        }

        // Getting the name of the equipped effect.
        InfuseEffect equippedEffect = plugin.getDataManager().getEffect(player, slot);

        // Handling if the slot is empty.
        if (equippedEffect == null) {
            Message msg = new Message(MessageType.SLOT_EMPTY);
            msg.applyPlaceholder("slot", slot);
            player.sendMessage(msg.toComponent());
            return true;
        }

        // Warning the player that they can't use the spark right now
        if (!RegionBlocker.getInstance().canUseSpark(player) || RegionBlocker.getInstance().isEffectBlocked(player, equippedEffect)) {
            sender.sendMessage(Message.toComponent("<red>You cannot activate your spark in this area!"));
        }

        equippedEffect.activateSpark(player);

        return true;
    }
}