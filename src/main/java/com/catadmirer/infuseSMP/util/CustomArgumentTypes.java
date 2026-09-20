package com.catadmirer.infuseSMP.util;

import java.util.List;

public class CustomArgumentTypes {
    public static final InfuseEffectArgumentType INFUSE_EFFECT = new InfuseEffectArgumentType();
    public static final SelectStringArgumentType SLOT = new SelectStringArgumentType(List.of("1", "2"));
    public static final SelectStringArgumentType CONTROL_MODE = new SelectStringArgumentType(List.of("offhand", "command"));
}
