// =============================================== //
// Recompile disabled. Please run Recaf with a JDK //
// =============================================== //

// Decompiled with: CFR 0.152
// Class Version: 8
package bre.smoothfont.util;

import org.apache.logging.log4j.LogManager;

public class Logger {
    private static org.apache.logging.log4j.Logger logger;

    public static void init() {
        logger = LogManager.getLogger();
    }

    public static void info(String str) {
        if (logger != null) {
            logger.info(str);
        } else {
            System.out.println("info: " + str);
        }
    }

    public static void warn(String str) {
        if (logger != null) {
            logger.warn(str);
        } else {
            System.out.println("warn: " + str);
        }
    }

    public static void error(String str) {
        if (logger != null) {
            logger.error(str);
        } else {
            System.out.println("error: " + str);
        }
    }
}
 