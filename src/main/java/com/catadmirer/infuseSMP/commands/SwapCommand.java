package com.catadmirer.infuseSMP.commands;

import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.Message.MessageType;
import com.catadmirer.infuseSMP.effects.InfuseEffect;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class SwapCommand {
    private final Infuse plugin;

    public static LiteralCommandNode<CommandSourceStack> build(Infuse plugin) {
        SwapCommand cmd = new SwapCommand(plugin);

        return Commands.literal("swap")
            .requires(c -> c.getSender().hasPermission("infuse.commands.swap") && c.getSender() instanceof Player)
            .executes(cmd::swap)
            .build();
    }

    private SwapCommand(Infuse plugin) {
        this.plugin = plugin;
    }

    public int swap(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(new Message(MessageType.ERROR_NOT_PLAYER).toComponent());
            return 1;
        }

        // Getting the equipped effects
        InfuseEffect effect1 = plugin.getDataManager().getEffect(player, "1");
        InfuseEffect effect2 = plugin.getDataManager().getEffect(player, "2");
        if (effect1 == null && effect2 == null) {
            player.sendMessage(new Message(MessageType.SWAP_NO_EFFECTS).toComponent());
            return 1;
        }

        // Swapping the effects
        plugin.getDataManager().setEffect(player, "1", effect2);
        plugin.getDataManager().setEffect(player, "2", effect1);

        player.sendMessage(new Message(MessageType.SWAP_SUCCESS).toComponent());
        return 1;
    }
}
