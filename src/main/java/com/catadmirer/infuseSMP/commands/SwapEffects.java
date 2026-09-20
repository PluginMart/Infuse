package com.catadmirer.infuseSMP.commands;

import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.Message.MessageType;
import com.catadmirer.infuseSMP.effects.InfuseEffect;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class SwapEffects implements CommandExecutor {
    private final Infuse plugin;

    public SwapEffects(Infuse plugin) {
        this.plugin = plugin;
    }

    // Defining the command for swapping effects...
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(new Message(MessageType.ERROR_NOT_PLAYER).toComponent());
            return true;
        }

        // Getting the equipped effects
        InfuseEffect effect1 = plugin.getDataManager().getEffect(player, "1");
        InfuseEffect effect2 = plugin.getDataManager().getEffect(player, "2");
        if (effect1 == null && effect2 == null) {
            player.sendMessage(new Message(MessageType.SWAP_NO_EFFECTS).toComponent());
            return true;
        }

        // Swapping the effects
        plugin.getDataManager().setEffect(player, "1", effect2);
        plugin.getDataManager().setEffect(player, "2", effect1);

        player.sendMessage(new Message(MessageType.SWAP_SUCCESS).toComponent());
        return true;
    }
}
