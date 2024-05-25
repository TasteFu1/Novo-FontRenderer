// =============================================== //
// Recompile disabled. Please run Recaf with a JDK //
// =============================================== //

// Decompiled with: CFR 0.152
// Class Version: 8
package bre;

import net.minecraft.client.Minecraft;

import bre.smoothfont.FontRasterizer;
import bre.smoothfont.FontShader;
import bre.smoothfont.FontTextureManager;
import bre.smoothfont.config.FontConfig;
import bre.smoothfont.renderer.FontRendererImpl;
import bre.smoothfont.util.GLUtils;
import bre.smoothfont.util.Logger;

public class Initializer {
    public static void preInit() {
        Logger.init();

        GLUtils.checkGlFuncSupported();
        FontShader.getInstance().setupShader();

        FontConfig config = FontConfig.getInstance();

        if (config.runMode == 0) {
            FontRasterizer.getInstance().initFontCache(false);
        }

        FontRendererImpl.initAfterConfigLoaded();
    }

    public static void postInit() {
        FontRendererImpl.modLoaded = true;
        Minecraft.getMinecraft().ttfFontRenderer.getFontRendererImpl().updateChangeFontFlag();
        FontTextureManager.getInstance().onResourceManagerReload(Minecraft.getMinecraft().getResourceManager());
    }
}
 