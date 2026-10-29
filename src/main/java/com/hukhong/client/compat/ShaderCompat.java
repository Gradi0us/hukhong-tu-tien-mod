package com.hukhong.client.compat;

import net.neoforged.fml.ModList;

/** Phát hiện Iris đang dùng shaderpack (qua reflection, không phụ thuộc biên dịch). Lỗi -> coi như không có shader. */
public final class ShaderCompat {
    private ShaderCompat() {}

    private static Boolean irisLoaded;
    private static Object api;
    private static java.lang.reflect.Method inUse;
    private static long nextCheck;
    private static boolean cached;

    public static boolean shadersActive() {
        long now = System.currentTimeMillis();
        if (now < nextCheck) return cached;
        nextCheck = now + 1000;
        cached = check();
        return cached;
    }

    private static boolean check() {
        try {
            if (irisLoaded == null) irisLoaded = ModList.get().isLoaded("iris");
            if (!irisLoaded) return false;
            if (api == null) {
                Class<?> c = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                api = c.getMethod("getInstance").invoke(null);
                inUse = c.getMethod("isShaderPackInUse");
            }
            return (Boolean) inUse.invoke(api);
        } catch (Throwable t) {
            irisLoaded = false;
            return false;
        }
    }
}
