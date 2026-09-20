package com.catadmirer.infuseSMP.commands;

import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.Message.MessageType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

public class DrainCommand implements CommandExecutor {
    private final Infuse plugin;

    public DrainCommand(Infuse plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(new Message(MessageType.ERROR_NOT_PLAYER).toComponent());
            return true;
        }

        // Getting the slot to drain based on the command used.  Accepts /ldrain or /rdrain
        if (label.equals("ldrain")){
            plugin.getEffectManager().drainEffect(player, "1");
        } else if (label.equals("rdrain")) {
            plugin.getEffectManager().drainEffect(player, "2");
        } else {
            player.sendMessage(new Message(MessageType.WITHDRAW_INVALID).toComponent());
        }
        return true;
    }
}