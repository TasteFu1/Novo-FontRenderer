// =============================================== //
// Recompile disabled. Please run Recaf with a JDK //
// =============================================== //

// Decompiled with: CFR 0.152
// Class Version: 8
package bre.smoothfont.util;

import net.minecraft.launchwrapper.Launch;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

public class ModLib {
    private static final Map<String, Long> startTime = new HashMap<String, Long>();
    private static final Map<String, Long> startTimeNano = new HashMap<String, Long>();

    public static void startCounter(String str) {
        startTime.put(str, System.currentTimeMillis());
        startTimeNano.put(str, System.nanoTime());
    }

    public static long stopCounterMs(String str) {
        return ModLib.stopCounterMs(str, true);
    }

    public static long stopCounterMs(String str, boolean logging) {
        long time = 0L;
        if (startTime.get(str) != null) {
            time = System.currentTimeMillis() - startTime.get(str);
            startTime.remove(str);
            if (logging) {
                Logger.info(str + ": " + time + "ms");
            }
        } else {
            Logger.error(str + ": Error. The timer(ms) has not been started.");
        }
        return time;
    }

    public static float roundIf(float origVal, float tolerance) {
        float newVal = ModLib.round(origVal);
        return Math.abs(newVal - origVal) < tolerance ? newVal : origVal;
    }

    public static int round(float origVal) {
        return ModLib.roundAny(origVal, 0.5f);
    }

    public static int round(double origVal) {
        return ModLib.roundAny((float) origVal, 0.5f);
    }

    public static int roundHalfEven(float origVal) {
        int floorVal = (int) origVal;
        float diff = origVal - (float) floorVal;

        if (diff == 0.5f) {
            return floorVal % 2 == 0 ? floorVal : floorVal + 1;
        } else if (diff == -0.5f) {
            return floorVal % 2 == 0 ? floorVal : floorVal - 1;
        } else {
            return ModLib.roundAny(origVal, 0.5f);
        }
    }

    public static int roundAny(float origVal, float loc) {
        int intVal = (int) origVal;
        if (origVal >= 0.0f) {
            if (origVal - (float) intVal < loc) {
                return intVal;
            }
            return intVal + 1;
        }
        if ((float) intVal - origVal < loc) {
            return intVal;
        }
        return intVal - 1;
    }
}
 