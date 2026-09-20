package com.catadmirer.infuseSMP.commands;

import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.managers.ParticleManager;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.FinePositionResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DrawCommand {
    public static LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("draw")
            .requires(c -> c.getSender().hasPermission("infuse.commands.draw") && c.getSender() instanceof Player)
            .then(Commands.argument("loc1", ArgumentTypes.finePosition())
                .then(Commands.argument("loc2", ArgumentTypes.finePosition())
                    .then(Commands.argument("count", IntegerArgumentType.integer(0, 500))
                        .executes(c -> drawLine(c, c.getArgument("loc1", FinePositionResolver.class), c.getArgument("loc2", FinePositionResolver.class), c.getArgument("count", Integer.class)))
                    )
                )
            )
            .build();
    }

    public static int drawLine(CommandContext<CommandSourceStack> ctx, FinePositionResolver loc1, FinePositionResolver loc2, Integer count) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only a player can use this command!", NamedTextColor.RED));
            return 1;
        }

        Location start;
        try {
            start = loc1.resolve(ctx.getSource()).toLocation(player.getWorld());
        } catch (CommandSyntaxException err) {
            sender.sendMessage(Message.mcs.deserialize(err.getRawMessage()));
            return 1;
        }
        
        Location end;
        try {
            end = loc2.resolve(ctx.getSource()).toLocation(player.getWorld());
        } catch (CommandSyntaxException err) {
            sender.sendMessage(Message.mcs.deserialize(err.getRawMessage()));
            return 1;
        }

        ParticleManager.drawLine(start, end, count);

        return 1;
    }
}