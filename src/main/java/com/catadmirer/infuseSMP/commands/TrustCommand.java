package com.catadmirer.infuseSMP.commands;

import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.Message.MessageType;
import com.catadmirer.infuseSMP.util.trust.TrustManager;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

import java.util.List;

@NullMarked
public class TrustCommand {
    private final TrustManager trustManager;
    private final boolean trust;

    public static LiteralCommandNode<CommandSourceStack> build(TrustManager manager, boolean trust) {
        TrustCommand cmd = new TrustCommand(manager, trust);

        return Commands.literal(trust ? "trust" : "untrust")
            .requires(c -> c.getSender().hasPermission("infuse.commands.trust") && c.getSender() instanceof Player)
            .then(Commands.argument("target", ArgumentTypes.players()).executes(c -> cmd.trust(c, c.getArgument("target", PlayerSelectorArgumentResolver.class))))
            .build();
    }

    private TrustCommand(TrustManager trustManager, boolean trust) {
        this.trustManager = trustManager;
        this.trust = trust;
    }

    public int trust(CommandContext<CommandSourceStack> ctx, PlayerSelectorArgumentResolver resolver) {
        CommandSender sender = ctx.getSource().getSender();

        // Limiting this command to only players.
        if (!(sender instanceof Player caster)) {
            sender.sendMessage(new Message(MessageType.TRUST_CONSOLE_USAGE).toComponent());
            return 1;
        }

        // Getting the targets
        List<Player> targets;
        try {
            targets = resolver.resolve(ctx.getSource());
        } catch (CommandSyntaxException err) {
            sender.sendMessage(Message.mcs.deserialize(err.getRawMessage()));
            return 1;
        }

        targets.remove(caster);

        if (trust) {
            targets.forEach(t -> addTrust(caster, t));
        } else {
            targets.forEach(t -> removeTrust(caster, t));
        }

        return 1;
    }

    public void addTrust(Player caster, Player target) {
        // Preventing duplicate trust entries
        Message msg = new Message(MessageType.TRUST_ALREADY_TRUSTED);

        if (!trustManager.getTrusted(caster).contains(target)) {
            trustManager.addTrust(caster, target);
            msg = new Message(MessageType.TRUST_ADDED);
        }

        msg.applyPlaceholder("target", target.getName());
        caster.sendMessage(msg.toComponent());
    }

    public void removeTrust(Player caster, Player target) {
        // Removing trust
        Message msg = new Message(MessageType.TRUST_NOT_TRUSTED);

        if (trustManager.getTrusted(caster).contains(target)) {
            trustManager.removeTrust(caster, target);
            msg = new Message(MessageType.TRUST_REMOVED);
        }

        msg.applyPlaceholder("target", target.getName());
        caster.sendMessage(msg.toComponent());
    }
}
