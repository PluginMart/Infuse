package com.catadmirer.infuseSMP.events;

import com.catadmirer.infuseSMP.effects.InfuseEffect;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class EffectCraftEvent extends Event {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final HumanEntity player;
    private final InfuseEffect effect;

    public EffectCraftEvent(HumanEntity player, InfuseEffect effect) {
        this.player = player;
        this.effect = effect;
    }

    public HumanEntity getPlayer() {
        return player;
    }

    public InfuseEffect getEffect() {
        return effect;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
