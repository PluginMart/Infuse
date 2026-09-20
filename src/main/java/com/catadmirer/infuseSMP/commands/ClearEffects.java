package com.catadmirer.infuseSMP.commands;

import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.Message.MessageType;
import com.catadmirer.infuseSMP.managers.EffectManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

public class ClearEffects implements CommandExecutor {
    private final EffectManager effectManager;

    public ClearEffects(EffectManager effectManager) {
        this.effectManager = effectManager;
    }

    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
        if (!command.getName().equalsIgnoreCase("cleareffects")) return false;

        if (args.length != 1) {
            sender.sendMessage(new Message(MessageType.INFUSE_CLEAREFFECTS_USAGE).toComponent());
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target != null) {
            effectManager.unequipEffect(target, "1");
            effectManager.unequipEffect(target, "2");
        }

        return true;
    }
}
