package com.catadmirer.infuseSMP.util;

import com.catadmirer.infuseSMP.effects.InfuseEffect;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.FlagContext;
import com.sk89q.worldguard.protection.flags.InvalidFlagFormat;
import com.sk89q.worldguard.protection.flags.RegionGroup;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;

public class EffectFlag extends Flag<InfuseEffect> {
    private final InfuseEffect defaultValue;

    public EffectFlag(String name) {
        super(name);
        this.defaultValue = null;
    }

    public EffectFlag(String name, InfuseEffect defaultValue) {
        super(name);
        this.defaultValue = defaultValue;
    }

    public EffectFlag(String name, RegionGroup defaultGroup) {
        super(name, defaultGroup);
        this.defaultValue = null;
    }

    public EffectFlag(String name, RegionGroup defaultGroup, InfuseEffect defaultValue) {
        super(name, defaultGroup);
        this.defaultValue = defaultValue;
    }

    @Nullable
    @Override
    public InfuseEffect getDefault() {
        return defaultValue;
    }

    @Override
    public InfuseEffect parseInput(FlagContext context) throws InvalidFlagFormat {
        String key = context.getUserInput();
        InfuseEffect effect = InfuseEffect.getEffect(Key.key("infuse", key));

        if (effect != null) return effect;
        
        throw new InvalidFlagFormat("Invalid InfuseEffect key '" + key + "'.  Is it registered?");
    }

    @Override
    public InfuseEffect unmarshal(@javax.annotation.Nullable Object o) {
        if (!(o instanceof String key)) return null;

        return InfuseEffect.getEffect(Key.key("infuse", key));
    }

    @Override
    public Object marshal(InfuseEffect o) {
        return o.toString();
    }    
}
