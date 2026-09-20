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

public class SparkCommand {
    private final Infuse plugin;
    private final String slot;

    public static LiteralCommandNode<CommandSourceStack> build(Infuse plugin, boolean lSpark) {
        SparkCommand cmd = new SparkCommand(plugin, lSpark ? "1" : "2");

        return Commands.literal(lSpark ? "lspark" : "rspark")
            .requires(c -> c.getSender().hasPermission("infuse.commands.spark") && c.getSender() instanceof Player)
            .executes(cmd::activateSpark)
            .build();
    }

    private SparkCommand(Infuse plugin, String slot) {
        this.plugin = plugin;
        this.slot = slot;
    }

    public int activateSpark(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(new Message(MessageType.ERROR_NOT_PLAYER).toComponent());
            return 1;
        }

        // Getting the name of the equipped effect.
        InfuseEffect equippedEffect = plugin.getDataManager().getEffect(player, slot);

        // Handling if the slot is empty.
        if (equippedEffect == null) {
            Message msg = new Message(MessageType.SLOT_EMPTY);
            msg.applyPlaceholder("slot", slot);
            player.sendMessage(msg.toComponent());
            return 1;
        }

        // Warning the player that they can't use the spark right now
        if (!plugin.getRegionBlocker().canUseSpark(player) || plugin.getRegionBlocker().isEffectBlocked(player, equippedEffect)) {
            sender.sendMessage(Message.toComponent("<red>You cannot activate your spark in this area!"));
        }

        equippedEffect.activateSpark(player, slot);

        return 1;
    }
}
