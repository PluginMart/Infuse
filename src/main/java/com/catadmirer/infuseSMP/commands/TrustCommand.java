package com.catadmirer.infuseSMP.commands;

import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.Message.MessageType;
import com.catadmirer.infuseSMP.playerdata.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class TrustCommand implements CommandExecutor {
    private final DataManager dataManager;

    public TrustCommand(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Limiting this command to only players.
        if (!(sender instanceof Player caster)) {
            sender.sendMessage(new Message(MessageType.TRUST_CONSOLE_USAGE).toComponent());
            return true;
        }

        // Validating the number of args
        if (args.length != 1) {
            Message msg = new Message(MessageType.TRUST_INCORRECT_USAGE);
            msg.applyPlaceholder("label", label);
            caster.sendMessage(msg.toComponent());
            return true;
        }

        // Getting the target to trust/untrust
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            caster.sendMessage(new Message(MessageType.TRUST_NO_PLAYER).toComponent());
            return true;
        }

        // Preventing the caster from trusting/untrusting themselves.
        if (caster.getUniqueId().equals(target.getUniqueId())) {
            caster.sendMessage(new Message(MessageType.TRUST_SELF).toComponent());
            return true;
        }

        // Making the caster trust the target.
        if (label.equalsIgnoreCase("trust")) {
            // Preventing duplicate trust entries
            if (dataManager.getTrusted(caster).contains(target)) {
                Message msg = new Message(MessageType.TRUST_ALREADY_TRUSTED);
                msg.applyPlaceholder("target", target.getName());
                caster.sendMessage(msg.toComponent());
                return true;
            }

            dataManager.addTrust(caster, target);
            Message msg = new Message(MessageType.TRUST_ADDED);
            msg.applyPlaceholder("target", target.getName());
            caster.sendMessage(msg.toComponent());
            return true;
        }

        // Making the caster untrust the target.
        if (label.equalsIgnoreCase("untrust")) {
            // Handling if the player already didnt trust the target
            if (!dataManager.getTrusted(caster).contains(target)) {
                Message msg = new Message(MessageType.TRUST_NOT_TRUSTED);
                msg.applyPlaceholder("target", target.getName());
                caster.sendMessage(msg.toComponent());
                return true;
            }

            dataManager.removeTrust(caster, target);
            Message msg = new Message(MessageType.TRUST_REMOVED);
            msg.applyPlaceholder("target", target.getName());
            caster.sendMessage(msg.toComponent());
            return true;
        }

        return false;
    }
}
