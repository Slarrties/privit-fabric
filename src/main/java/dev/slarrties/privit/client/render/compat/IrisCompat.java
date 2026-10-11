package dev.slarrties.privit.client.render.compat;

import net.irisshaders.iris.api.v0.IrisApi;

public final class IrisCompat {

    private IrisCompat() {}

    public static boolean isShaderPackInUse() {
        return IrisApi.getInstance().isShaderPackInUse();
    }
}