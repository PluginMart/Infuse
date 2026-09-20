package com.catadmirer.infuseSMP.commands;

import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.Message;
import com.catadmirer.infuseSMP.Message.MessageType;
import com.catadmirer.infuseSMP.effects.InfuseEffect;
import com.catadmirer.infuseSMP.inventories.EffectChooser;
import com.catadmirer.infuseSMP.inventories.RecipeListGUI;
import com.catadmirer.infuseSMP.managers.CooldownManager;
import com.catadmirer.infuseSMP.managers.EffectManager;
import com.catadmirer.infuseSMP.managers.EffectManager.EquipResult;
import com.catadmirer.infuseSMP.managers.EffectManager.EquipResultType;
import com.catadmirer.infuseSMP.util.CustomArgumentTypes;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class InfuseCommand {
    private final Infuse plugin;

    public static LiteralCommandNode<CommandSourceStack> build(Infuse plugin) {
        InfuseCommand cmd = new InfuseCommand(plugin);

        return Commands.literal("infuse")
            .then(Commands.literal("gui")
                .requires(c -> c.getSender().hasPermission("infuse.commands.infuse.gui") && (c.getSender() instanceof Player))
                .executes(cmd::gui)
            )
            .then(Commands.literal("reload")
                .requires(c -> c.getSender().hasPermission("infuse.commands.infuse.reload") )
                .executes(cmd::reload)
            )
            .then(Commands.literal("reroll")
                .requires(c -> c.getSender().hasPermission("infuse.commands.infuse.reroll"))
                .executes(c -> cmd.reroll(c, null))
                .then(Commands.argument("target", ArgumentTypes.players())
                    .executes(c -> cmd.reroll(c, c.getArgument("target", PlayerSelectorArgumentResolver.class)))
                )
            )
            .then(Commands.literal("recipes")
                .requires(c -> c.getSender().hasPermission("infuse.commands.infuse.recipes"))
                .executes(InfuseCommand::recipes)
            )
            .then(Commands.literal("giveEffect")
                .requires(c -> c.getSender().hasPermission("infuse.commands.infuse.giveEffect"))
                .then(Commands.argument("target", ArgumentTypes.player())
                    .then(Commands.argument("effect", CustomArgumentTypes.INFUSE_EFFECT)
                        .executes(c -> cmd.giveEffect(c, c.getArgument("target", PlayerSelectorArgumentResolver.class), c.getArgument("effect", InfuseEffect.class)))
                    )
                )
            )
            .then(Commands.literal("setEffect")
                .requires(c -> c.getSender().hasPermission("infuse.commands.infuse.setEffect"))
                .then(Commands.argument("target", ArgumentTypes.player())
                    .then(Commands.argument("effect", CustomArgumentTypes.INFUSE_EFFECT)
                        .then(Commands.argument("slot", CustomArgumentTypes.SLOT)
                            .executes(c -> cmd.setEffect(c, c.getArgument("target", PlayerSelectorArgumentResolver.class), c.getArgument("effect", InfuseEffect.class), c.getArgument("slot", String.class)))
                        )
                    )
                )
            )
            .then(Commands.literal("clearEffects")
                .requires(c -> c.getSender().hasPermission("infuse.commands.infuse.clearEffects"))
                .executes(c -> cmd.clearEffects(c, null))
                .then(Commands.argument("target", ArgumentTypes.player())
                    .executes(c -> cmd.clearEffects(c, c.getArgument("target", PlayerSelectorArgumentResolver.class)))
                )
            )
            .then(Commands.literal("cooldown")
                .requires(c -> c.getSender().hasPermission("infuse.commands.infuse.cooldown"))
                .executes(c -> cmd.cooldown(c, null))
                .then(Commands.argument("target", ArgumentTypes.player())
                    .executes(c -> cmd.cooldown(c, c.getArgument("target", PlayerSelectorArgumentResolver.class)))
                )
            )
            .then(Commands.literal("getTrusted")
                .requires(c -> c.getSender().hasPermission("infuse.commands.infuse.getTrusted"))
                .executes(c -> cmd.getTrusted(c, null))
                .then(Commands.argument("target", ArgumentTypes.player())
                    .executes(c -> cmd.getTrusted(c, c.getArgument("target", PlayerSelectorArgumentResolver.class)))
                )
            )
            .then(Commands.literal("controls")
                .requires(c -> c.getSender().hasPermission("infuse.commands.infuse.controls") && c.getSender() instanceof Player)
                .executes(cmd::showControls)
                .then(Commands.argument("choice", CustomArgumentTypes.CONTROL_MODE)
                    .executes(c -> cmd.controls(c, c.getArgument("choice", String.class)))
                )
            )
            .then(Commands.literal("help")
                .requires(c -> c.getSender().hasPermission("infuse.commands.infuse.help"))
                .executes(cmd::help)
            )
            .build();
    }

    public InfuseCommand(Infuse plugin) {
        this.plugin = plugin;
    }

    public int gui(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command", NamedTextColor.RED));
            return 1;
        }

        player.openInventory(new EffectChooser(plugin).getInventory());
        return 1;
    }

    public int reload(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();

        plugin.getMainConfig().load();
        plugin.getRecipeManager().reload();
        sender.sendMessage("Infuse configs reloaded");
        return 1;
    }

    public int reroll(CommandContext<CommandSourceStack> ctx, @Nullable PlayerSelectorArgumentResolver resolver) {
        CommandSender sender = ctx.getSource().getSender();

        List<Player> targets;
        if (resolver == null) {
            if (sender instanceof Player p) {
                targets = List.of(p);
            } else {
                sender.sendMessage(Component.text("Invalid target.  Please specify a player to reroll.", NamedTextColor.RED));
                return 1;
            }
        } else {
            try {
                targets = resolver.resolve(ctx.getSource());
            } catch (CommandSyntaxException e) {
                sender.sendMessage(Message.mcs.deserialize(e.getRawMessage()));
                return 1;
            }
        }

        EffectManager manager = plugin.getEffectManager();

        // Looping over the players
        for (Player p : targets) {
            manager.removeEffects(p);
            EquipResult result = manager.giveJoinEffect(p);
            if (result.type() == EquipResultType.FAIL) {
                sender.sendMessage(Component.text("There are no join effects in the config.", NamedTextColor.RED));
                return 1;
            } else if (result.type() == EquipResultType.CANCELLED) {
                sender.sendMessage(Component.text("giveJoinEffects was cancelled for player " + p.getName(), NamedTextColor.RED));
            }
            p.getPersistentDataContainer().set(Infuse.JOIN_EFFECT_KEY, PersistentDataType.BOOLEAN, true);
        }

        // Sending feedback
        if (targets.size() == 1) {
            sender.sendMessage(Component.text("Rerolled " + targets.getFirst() + "'s join effect."));
        } else {
            sender.sendMessage(Component.text("Rerolled " + targets.size() + " player's join effects."));
        }

        return 1;
    }

    public int giveEffect(CommandContext<CommandSourceStack> ctx, PlayerSelectorArgumentResolver resolver, InfuseEffect effect) {
        CommandSender sender = ctx.getSource().getSender();

        Player target;
        try {
            target = resolver.resolve(ctx.getSource()).getFirst();
        } catch (CommandSyntaxException err) {
            sender.sendMessage(Message.mcs.deserialize(err.getRawMessage()));
            return 1;
        }

        if (!target.isOnline()) {
            sender.sendMessage(new Message(MessageType.ERROR_TARGET_NOT_FOUND).toComponent());
            return 1;
        }

        if (effect == null) {
            sender.sendMessage(new Message(MessageType.INFUSE_INVALID_PARAM).toComponent());
            return 1;
        }

        target.getInventory().addItem(effect.createItem());

        Message msg = new Message(MessageType.INFUSE_GIVEEFFECT_SUCCESS);
        msg.applyPlaceholder("effect_color", "<#" + Integer.toHexString(effect.getPotionColor().getRGB() & 0xffffff) + ">");
        msg.applyPlaceholder("effect_name", effect.getName());
        target.sendMessage(msg.toComponent());

        return 1;
    }

    public int setEffect(CommandContext<CommandSourceStack> ctx, PlayerSelectorArgumentResolver resolver, InfuseEffect effect, String slot) {
        CommandSender sender = ctx.getSource().getSender();

        Player target;

        try {
            target = resolver.resolve(ctx.getSource()).getFirst();
        } catch (CommandSyntaxException err) {
            sender.sendMessage(Message.mcs.deserialize(err.getRawMessage()));
            return 1;
        }

        if (effect == null) {
            sender.sendMessage(new Message(MessageType.INFUSE_INVALID_PARAM).toComponent());
            return 1;
        }

        // Setting the effect
        plugin.getEffectManager().setEffect(target, effect, slot);
        Message msg = new Message(MessageType.INFUSE_SETEFFECT_SUCCESS);
        msg.applyPlaceholder("slot", slot);
        msg.applyPlaceholder("player_name", target.getName());
        msg.applyPlaceholder("effect_name", effect.getName());
        sender.sendMessage(msg.toComponent());

        return 1;
    }

    public int clearEffects(CommandContext<CommandSourceStack> ctx, @Nullable PlayerSelectorArgumentResolver resolver) {
        CommandSender sender = ctx.getSource().getSender();

        // Getting the player and making sure they are online
        Player target;
        if (resolver == null) {
            if (sender instanceof Player p) {
                target = p;
            } else {
                sender.sendMessage(Component.text("Invalid target.  Please specify a player.", NamedTextColor.RED));
                return 1;
            }
        } else {
            try {
                target = resolver.resolve(ctx.getSource()).getFirst();
            } catch (CommandSyntaxException e) {
                sender.sendMessage(Message.mcs.deserialize(e.getRawMessage()));
                return 1;
            }
        }

        // Removing the effects from the player
        plugin.getEffectManager().removeEffects(target);
        Message msg = new Message(MessageType.INFUSE_CLEAREFFECTS_SUCCESS);
        msg.applyPlaceholder("player_name", target.getName());
        sender.sendMessage(msg.toComponent());

        return 1;
    }

    public int cooldown(CommandContext<CommandSourceStack> ctx, @Nullable PlayerSelectorArgumentResolver resolver) {
        CommandSender sender = ctx.getSource().getSender();

        // Getting the player and making sure they are online
        Player target;
        if (resolver == null) {
            if (sender instanceof Player p) {
                target = p;
            } else {
                sender.sendMessage(Component.text("Invalid target.  Please specify a player.", NamedTextColor.RED));
                return 1;
            }
        } else {
            try {
                target = resolver.resolve(ctx.getSource()).getFirst();
            } catch (CommandSyntaxException err) {
                sender.sendMessage(Message.mcs.deserialize(err.getRawMessage()));
                return 1;
            }
        }

        if (!target.isOnline()) {
            sender.sendMessage(new Message(MessageType.ERROR_TARGET_NOT_FOUND).toComponent());
            return 1;
        }

        // Removing cooldowns from the player
        CooldownManager.removeAllCooldowns(target.getUniqueId());
        Message msg = new Message(MessageType.INFUSE_COOLDOWN_SUCCESS);
        msg.applyPlaceholder("player_name", target.getName());
        sender.sendMessage(msg.toComponent());

        return 1;
    }

    public int getTrusted(CommandContext<CommandSourceStack> ctx, @Nullable PlayerSelectorArgumentResolver resolver) {
        CommandSender sender = ctx.getSource().getSender();

        // Getting the player and making sure they are online
        Player target;
        if (resolver == null) {
            if (sender instanceof Player p) {
                target = p;
            } else {
                sender.sendMessage(Component.text("Invalid target.  Please specify a player.", NamedTextColor.RED));
                return 1;
            }
        } else {
            try {
                target = resolver.resolve(ctx.getSource()).getFirst();
            } catch (CommandSyntaxException err) {
                sender.sendMessage(Message.mcs.deserialize(err.getRawMessage()));
                return 1;
            }
        }

        // Listing trusted players
        if (sender.equals(target)) {
            sender.sendMessage(Component.text("You trust:"));
        } else {
            sender.sendMessage(target.displayName().append(Component.text(target.getName() + " trusts:").style(Style.style())));
        }

        plugin.getTrustManager().getTrusted(target).forEach(p -> sender.sendMessage(Component.text("- " + p.getName())));

        return 1;
    }

    public int showControls(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command", NamedTextColor.RED));
            return 1;
        }

        String controlMode = plugin.getDataManager().getControlMode(player);

        Message msg = new Message(MessageType.CONTROL_MODE_NOTIFY);
        msg.applyPlaceholder("control_mode", controlMode);
        player.sendMessage(msg.toComponent());

        return 1;
    }

    public int controls(CommandContext<CommandSourceStack> ctx, String choice) {
        CommandSender sender = ctx.getSource().getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command", NamedTextColor.RED));
            return 1;
        }

        // Setting the control mode for the user.
        plugin.getDataManager().setControlMode(player, choice);

        Message msg = new Message(MessageType.INFUSE_CONTROLS_SUCCESS);
        msg.applyPlaceholder("control_mode", choice);
        sender.sendMessage(msg.toComponent());

        return 1;
    }

    public static int recipes(CommandContext<CommandSourceStack> ctx) {
        if (ctx.getSource().getSender() instanceof Player player) {
            player.openInventory(new RecipeListGUI().getInventory());
        }
        return 1;
    }

    public int help(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();

        new Message(MessageType.INFUSE_HELP).toComponentList().forEach(sender::sendMessage);

        return 1;
    }
}
