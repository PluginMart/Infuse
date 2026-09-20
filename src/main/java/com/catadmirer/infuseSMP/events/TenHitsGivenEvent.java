package com.catadmirer.infuseSMP.events;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class TenHitsGivenEvent extends PlayerEvent {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final LivingEntity lastTarget;

    public TenHitsGivenEvent(Player attacker, LivingEntity lastTarget) {
        super(attacker);
        this.lastTarget = lastTarget;
    }

    public LivingEntity getLastTarget() {
        return lastTarget;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
