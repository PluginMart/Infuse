package com.catadmirer.infuseSMP.events;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class TenHitsTakenEvent extends PlayerEvent {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final LivingEntity lastAttacker;

    public TenHitsTakenEvent(Player target, LivingEntity lastAttacker) {
        super(target);
        this.lastAttacker = lastAttacker;
    }

    public LivingEntity getLastAttacker() {
        return lastAttacker;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
