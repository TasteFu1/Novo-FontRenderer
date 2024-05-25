// =============================================== //
// Recompile disabled. Please run Recaf with a JDK //
// =============================================== //

// Decompiled with: CFR 0.152
// Class Version: 8
package bre.smoothfont;

import net.minecraft.client.renderer.GlStateManager;

import org.lwjgl.opengl.GL11;

import bre.smoothfont.config.FontConfig;

public class GlStateManagerHelper {
    private static final GlState stateTexEnvMode = new GlState();
    private static boolean combineParamsChanged = false;
    private static final GlState stateBlendEx = new GlState();
    private static final GlState stateBlendFunc = new GlState();
    private static final GlState stateLodBias = new GlState();

    public static void setGlTexEnvMode(int newMode, boolean reset) {
        if (!reset) {
            GlStateManagerHelper.stateTexEnvMode.prevInt[0] = GL11.glGetTexEnvi(8960, 8704);
            if (newMode == GlStateManagerHelper.stateTexEnvMode.prevInt[0]) {
                combineParamsChanged = false;
                GlStateManagerHelper.stateTexEnvMode.changed = false;
                return;
            }
        }
        GlStateManagerHelper.setGlTexEnvi(0, 8704, newMode, reset);
        combineParamsChanged = false;
        GlStateManagerHelper.stateTexEnvMode.changed = true;
    }

    public static void setGlTexEnvModeCombine(boolean reset) {
        if (!reset) {
            GlStateManagerHelper.stateTexEnvMode.prevInt[0] = GL11.glGetTexEnvi(8960, 8704);
            GlStateManagerHelper.stateTexEnvMode.prevInt[1] = GL11.glGetTexEnvi(8960, 34161);
            GlStateManagerHelper.stateTexEnvMode.prevInt[2] = GL11.glGetTexEnvi(8960, 34176);
            GlStateManagerHelper.stateTexEnvMode.prevInt[3] = GL11.glGetTexEnvi(8960, 34192);
            GlStateManagerHelper.stateTexEnvMode.prevInt[4] = GL11.glGetTexEnvi(8960, 34162);
            GlStateManagerHelper.stateTexEnvMode.prevInt[5] = GL11.glGetTexEnvi(8960, 34184);
            GlStateManagerHelper.stateTexEnvMode.prevInt[6] = GL11.glGetTexEnvi(8960, 34185);
            GlStateManagerHelper.stateTexEnvMode.prevInt[7] = GL11.glGetTexEnvi(8960, 34200);
            GlStateManagerHelper.stateTexEnvMode.prevInt[8] = GL11.glGetTexEnvi(8960, 34201);
        }
        GlStateManagerHelper.setGlTexEnvi(0, 8704, 34160, reset);
        GlStateManagerHelper.setGlTexEnvi(1, 34161, 7681, reset);
        GlStateManagerHelper.setGlTexEnvi(2, 34176, 34168, reset);
        GlStateManagerHelper.setGlTexEnvi(3, 34192, 768, reset);
        GlStateManagerHelper.setGlTexEnvi(4, 34162, 8448, reset);
        GlStateManagerHelper.setGlTexEnvi(5, 34184, 5890, reset);
        GlStateManagerHelper.setGlTexEnvi(6, 34185, 34168, reset);
        GlStateManagerHelper.setGlTexEnvi(7, 34200, 770, reset);
        GlStateManagerHelper.setGlTexEnvi(8, 34201, 770, reset);
        combineParamsChanged = true;
        GlStateManagerHelper.stateTexEnvMode.changed = true;
    }

    private static void setGlTexEnvi(int id, int mode, int newParam, boolean reset) {
        GlStateManagerHelper.stateTexEnvMode.newInt[id] = newParam;
        if (!reset && GlStateManagerHelper.stateTexEnvMode.prevInt[id] == newParam) {
            return;
        }
        GlStateManager.glTexEnvi(8960, mode, newParam);
    }

    private static void restoreGlTexEnvi(int id, int mode, int prevParam) {
        if (GlStateManagerHelper.stateTexEnvMode.newInt[id] == prevParam) {
            return;
        }
        GlStateManager.glTexEnvi(8960, mode, prevParam);
    }

    public static void restoreGlTexEnvMode() {
        if (!GlStateManagerHelper.stateTexEnvMode.changed) {
            return;
        }
        GlStateManagerHelper.restoreGlTexEnvi(0, 8704, GlStateManagerHelper.stateTexEnvMode.prevInt[0]);
        if (combineParamsChanged) {
            GlStateManagerHelper.restoreGlTexEnvi(1, 34161, GlStateManagerHelper.stateTexEnvMode.prevInt[1]);
            GlStateManagerHelper.restoreGlTexEnvi(2, 34176, GlStateManagerHelper.stateTexEnvMode.prevInt[2]);
            GlStateManagerHelper.restoreGlTexEnvi(3, 34192, GlStateManagerHelper.stateTexEnvMode.prevInt[3]);
            GlStateManagerHelper.restoreGlTexEnvi(4, 34162, GlStateManagerHelper.stateTexEnvMode.prevInt[4]);
            GlStateManagerHelper.restoreGlTexEnvi(5, 34184, GlStateManagerHelper.stateTexEnvMode.prevInt[5]);
            GlStateManagerHelper.restoreGlTexEnvi(6, 34185, GlStateManagerHelper.stateTexEnvMode.prevInt[6]);
            GlStateManagerHelper.restoreGlTexEnvi(7, 34200, GlStateManagerHelper.stateTexEnvMode.prevInt[7]);
            GlStateManagerHelper.restoreGlTexEnvi(8, 34201, GlStateManagerHelper.stateTexEnvMode.prevInt[8]);
        }
        combineParamsChanged = false;
        GlStateManagerHelper.stateTexEnvMode.changed = false;
    }

    public static void setBlendEx(boolean reset) {
        boolean blend;
        boolean glBlend = blend = GlStateManager.blendState.blend.currentState;
        if (FontConfig.getInstance().workaroundWrongGlState) {
            glBlend = GL11.glIsEnabled(3042);

            if (!reset) {
                GlStateManagerHelper.stateBlendEx.prevBoolean2 = glBlend;

                if (blend != glBlend) {
                    GlStateManagerHelper.stateBlendEx.inconsistent = true;
                }
            }
        }
        if (!reset) {
            GlStateManagerHelper.stateBlendEx.prevBoolean1 = blend;
        }
        if (!blend) {
            GlStateManager.enableBlend();
            GlStateManagerHelper.stateBlendEx.changed = true;
        } else if (FontConfig.getInstance().workaroundWrongGlState && !glBlend) {
            GL11.glEnable(3042);
            GlStateManagerHelper.stateBlendEx.changed = true;
        }
    }

    public static void restoreBlendEx(boolean temporary) {
        if (!GlStateManagerHelper.stateBlendEx.changed) {
            return;
        }
        if (!GlStateManagerHelper.stateBlendEx.prevBoolean1) {
            GlStateManager.disableBlend();
            if (!temporary && FontConfig.getInstance().workaroundWrongGlState && GlStateManagerHelper.stateBlendEx.inconsistent) {
                GL11.glEnable(3042);
                GlStateManagerHelper.stateBlendEx.inconsistent = false;
            }
        } else if (!temporary && FontConfig.getInstance().workaroundWrongGlState && GlStateManagerHelper.stateBlendEx.inconsistent) {
            GL11.glDisable(3042);
            GlStateManagerHelper.stateBlendEx.inconsistent = false;
        }
        GlStateManagerHelper.stateBlendEx.changed = false;
    }

    public static void setBlendFunc(int blendSrcRGB, int blendDstRGB, boolean reset) {
        int src = GlStateManager.blendState.srcFactor;
        int dst = GlStateManager.blendState.dstFactor;
        int glSrc = src;
        int glDst = dst;

        if (FontConfig.getInstance().workaroundWrongGlState) {
            glSrc = GL11.glGetInteger(32969);
            glDst = GL11.glGetInteger(32968);

            if (!reset) {
                GlStateManagerHelper.stateBlendFunc.prevInt[2] = glSrc;
                GlStateManagerHelper.stateBlendFunc.prevInt[3] = glDst;
                if (glSrc != src || glDst != dst) {
                    GlStateManagerHelper.stateBlendFunc.inconsistent = true;
                }
            }
        }
        if (!reset) {
            GlStateManagerHelper.stateBlendFunc.prevInt[0] = src;
            GlStateManagerHelper.stateBlendFunc.prevInt[1] = dst;
        }
        if (src != blendSrcRGB || dst != blendDstRGB) {
            GlStateManager.blendFunc(blendSrcRGB, blendDstRGB);
            GlStateManagerHelper.stateBlendFunc.changed = true;
        } else if (FontConfig.getInstance().workaroundWrongGlState && (glSrc != src || glDst != dst)) {
            GL11.glBlendFunc(blendSrcRGB, blendDstRGB);
            GlStateManagerHelper.stateBlendFunc.changed = true;
        }
    }

    public static void restoreBlendFunc(boolean temporary) {
        if (!GlStateManagerHelper.stateBlendFunc.changed) {
            return;
        }
        GlStateManager.blendFunc(GlStateManagerHelper.stateBlendFunc.prevInt[0], GlStateManagerHelper.stateBlendFunc.prevInt[1]);
        if (!temporary && FontConfig.getInstance().workaroundWrongGlState && GlStateManagerHelper.stateBlendFunc.inconsistent) {
            GL11.glBlendFunc(GlStateManagerHelper.stateBlendFunc.prevInt[2], GlStateManagerHelper.stateBlendFunc.prevInt[3]);
            GlStateManagerHelper.stateBlendFunc.inconsistent = false;
        }
        GlStateManagerHelper.stateBlendFunc.changed = false;
    }

    public static void setTexLodBias(float lodBias) {
        GlStateManagerHelper.stateLodBias.prevFloat = GL11.glGetTexEnvf(34048, 34049);
        if (GlStateManagerHelper.stateLodBias.prevFloat == lodBias) {
            return;
        }
        GlStateManager.glTexEnvf(34048, 34049, lodBias);
        GlStateManagerHelper.stateLodBias.changed = true;
    }

    public static void restoreTexLodBias() {
        if (!GlStateManagerHelper.stateLodBias.changed) {
            return;
        }
        GlStateManager.glTexEnvf(34048, 34049, GlStateManagerHelper.stateLodBias.prevFloat);
        GlStateManagerHelper.stateLodBias.changed = false;
    }

    private static class GlState {
        boolean changed = false;
        boolean inconsistent = false;
        int[] prevInt = new int[9];
        int[] newInt = new int[9];
        float prevFloat;
        boolean prevBoolean1;
        boolean prevBoolean2;

        private GlState() {
        }
    }
}
 