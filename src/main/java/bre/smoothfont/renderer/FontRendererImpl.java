// =============================================== //
// Recompile disabled. Please run Recaf with a JDK //
// =============================================== //

// Decompiled with: CFR 0.152
// Class Version: 8
package bre.smoothfont.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;

import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import bre.smoothfont.BoldChecker;
import bre.smoothfont.ErrorCorrector;
import bre.smoothfont.FontProperty;
import bre.smoothfont.FontRasterizer;
import bre.smoothfont.FontShader;
import bre.smoothfont.FontTexture;
import bre.smoothfont.FontTextureManager;
import bre.smoothfont.FontUtils;
import bre.smoothfont.GlStateManagerHelper;
import bre.smoothfont.GlyphImage;
import bre.smoothfont.RenderCharReplacedChecker;
import bre.smoothfont.config.FontConfig;
import bre.smoothfont.util.Logger;
import bre.smoothfont.util.ModLib;

public class FontRendererImpl {
    private float fontScale;
    public float roundedFontScale;
    public int autoBrightnessDefault;
    public int autoBrightnessUnicode;
    public float brightnessBoundaryScaleFactorDefault;
    public float brightnessBoundaryScaleFactorUnicode;
    private boolean exclusionCondUnicode;
    private boolean exclusionCondDefault;
    private boolean alignToPixelCond;
    public boolean orthographic;
    public boolean rotation;
    private boolean fractionCoord;
    private boolean needToResetTexEnvAndBlend;
    private boolean onRenderString = false;
    private static final Minecraft mc = Minecraft.getMinecraft();
    public TTFFontRenderer fontRenderer;
    public int[] mcCharWidth;
    private boolean immutableSpcWidth = false;
    public String reasonForDisable = null;
    public boolean changeFont = false;
    public int precisionMode = 1;
    public boolean keepMcFontWidth = false;
    public boolean optimized = true;
    public boolean boldFlag = false;
    public boolean shadowFlag = false;
    public boolean thinFontFlag = false;
    private boolean mcDefaultFontFlag;
    private static final ResourceLocation[] unicodePageLocations = new ResourceLocation[256];
    private static final ResourceLocation[] osFontUnicodePageLocations = new ResourceLocation[256];
    private static final ResourceLocation osFontDefaultPageLocation = new ResourceLocation("smoothfont", "osFontDefaultPage");
    private final FontShader fontShader;
    private boolean anisotropicFilterEnabled = false;
    private final FontTextureManager fontTextureManager;
    private final FontRasterizer rasterizer;
    public float[] optifineCharWidthFloat = null;
    private final RenderCharReplacedChecker renderCharReplacedChecker = new RenderCharReplacedChecker();
    private boolean renderStringAtPosWorked = false;
    private boolean renderStringAtPosInoperative = false;
    private boolean renderStringWorked = false;
    public boolean renderStringInoperative = false;
    private boolean renderCharWorked = false;
    public static boolean modLoaded = false;
    public static List<FontRendererImpl> uninitFontRendererImplList = new ArrayList<>();
    public static float shadowAdjustVal = 0.0f;
    public static int texFilterSettingId = 0;
    public static boolean extShaderWorking = false;
    private final ErrorCorrector errCorrector;
    private final ErrorCorrector errCorrectorShadow;
    private final BoldChecker boldChecker;
    private final FloatBuffer floatBuf = BufferUtils.createFloatBuffer(16);

    public FontRendererImpl(TTFFontRenderer renderer) {
        this.mcCharWidth = new int[256];
        this.fontRenderer = renderer;
        this.fontTextureManager = FontTextureManager.getInstance();
        this.rasterizer = FontRasterizer.getInstance();
        this.fontShader = FontShader.getInstance();
        this.errCorrector = new ErrorCorrector();
        this.errCorrectorShadow = new ErrorCorrector();
        this.boldChecker = new BoldChecker();
    }

    public void fontRendererExitHook() {
        uninitFontRendererImplList.add(this);
    }

    public static void initAfterConfigLoaded() {
        for (FontRendererImpl fontRendererImpl : uninitFontRendererImplList) {
            fontRendererImpl.initAfterConfigLoaded(true);
        }
    }

    public void initAfterConfigLoaded(boolean deferredInit) {
        this.updateChangeFontFlag();

        switch (FontConfig.getInstance().runMode) {
            case 0: {
                if (this.rasterizer.glyphsGenerationError) {
                    this.changeFont = false;
                }

                this.optimized = true;
                break;
            }
            case 1: {
                this.optimized = false;
                break;
            }
            case 2: {
                this.optimized = true;
                break;
            }
        }

        if (this.changeFont) {
            this.rasterizer.restoreGlyphWidth(this);
        }

        shadowAdjustVal = FontUtils.getShadowAdjustVal(FontConfig.getInstance().shadowLength);
        texFilterSettingId = FontUtils.getTexFilterSettingId();

        FontRendererImpl.setUnicodeFlagSuitably(this);

        if (deferredInit) {
            this.readFontTextureExitHook();
            this.readGlyphSizesExitHook();
        }
    }

    public static void setUnicodeFlagSuitably(FontRendererImpl frh) {
        if (FontConfig.getInstance().forceUnicode && mc.isUnicode()) {
            frh.fontRenderer.setUnicodeFlag(true);
        }
    }

    private void setLodBias() {
        if (this.orthographic) {
            if (this.fractionCoord) {
                GlStateManagerHelper.setTexLodBias(FontConfig.getInstance().mipmapLodBiasFloat);
            } else {
                GlStateManagerHelper.setTexLodBias(FontConfig.getInstance().overlayLodBiasFloat);
            }
        } else {
            GlStateManagerHelper.setTexLodBias(FontConfig.getInstance().mipmapLodBiasFloat);
        }
    }

    private void setLodBiasPerformance() {
        GlStateManagerHelper.setTexLodBias(FontConfig.getInstance().overlayLodBiasFloat);
    }

    public float renderCharHook(char ch) {
        this.renderCharWorked = true;

        if (ch == ' ' || ch == '\u00A0') {
            return this.getSpaceWidth();
        } else {
            return -1.0f;
        }
    }

    public float renderDefaultCharHook(int id, boolean italic, float posX, float posY) {
        float texWidthPx;
        float width;
        this.renderCharReplacedChecker.renderDefaultCharWorked = true;
        ResourceLocation curResLoc = this.changeFont ? osFontDefaultPageLocation : this.fontRenderer.locationFontTexture;
        FontTexture texture = this.fontTextureManager.bindTexture(curResLoc);
        char ch = FontUtils.asciiSheetChars[id];
        float factor = texture.actualFontRes / 16.0f;
        if (this.changeFont) {
            this.mcDefaultFontFlag = false;
            if (this.renderStringAtPosInoperative) {
                this.boldFlag = this.boldChecker.isBold(posX, id, this.shadowFlag, false);
                if (this.boldFlag) {
                    posX -= 0.5f;
                }
                if (this.shadowFlag) {
                    posX -= 0.5f;
                    posY -= 0.5f;
                }
            }
            GlyphImage gi = this.rasterizer.getGlyphImage(256);
            switch (this.precisionMode) {
                default: {
                    width = this.rasterizer.charWidthFloat[id];
                    if (!FontConfig.getInstance().widthErrorCorrection) {
                        break;
                    }
                    if (this.shadowFlag) {
                        posX = this.errCorrectorShadow.getCorrectedPosX(posX, posY, width, this.boldFlag, false);
                        break;
                    }
                    posX = this.errCorrector.getCorrectedPosX(posX, posY, width, this.boldFlag, false);
                    break;
                }
                case 0: {
                    width = this.rasterizer.charWidthFloat[id];
                    break;
                }
                case 2: {
                    width = this.optifineCharWidthFloat != null ? this.rasterizer.charWidthFloat[id] : (float) this.rasterizer.charWidthInt[id];
                }
            }
            if (FontConfig.getInstance().fontAlignBaseline) {
                posY += gi.baselineGap + this.rasterizer.sizeAdjPosY;
            } else {
                int fontId = this.rasterizer.fontId[ch];
                FontProperty fontProp = this.rasterizer.fontProp[0][fontId];
                posY += gi.baselineGap + fontProp.ascentGap;
            }
            texWidthPx = gi.drawingChWidth[id] * texture.scaleFactor;
            posX -= gi.fontOriginPosX;
        } else {
            this.mcDefaultFontFlag = true;
            width = this.optifineCharWidthFloat != null ? this.optifineCharWidthFloat[id] : (float) this.mcCharWidth[id];
            texWidthPx = (width - 0.01f) * 2.0f * factor;
        }
        this.resetTexEnvAndBlend();
        if (!FontConfig.getInstance().performanceMode) {
            this.fontTextureManager.setAnisotropicFilter(curResLoc, this.anisotropicFilterEnabled);
            if (!this.exclusionCondDefault) {
                this.fontShader.setShaderParams(this, false);
                this.fontTextureManager.setTexParams(curResLoc, texFilterSettingId);
            } else {
                this.fontTextureManager.setTexParamsNearest(curResLoc);
            }
        } else {
            this.fontTextureManager.setTexParams(curResLoc, texFilterSettingId);
        }
        this.renderCharCommon(id, italic, posX, posY, 0.0f, texWidthPx, factor, texture);
        return width;
    }

    public float renderUnicodeCharHook(char ch, boolean italic, byte[] glyphWidth, float posX, float posY) {
        float texWidthPx;
        float left;
        float factor;
        FontTexture texture;
        float width;

        this.renderCharReplacedChecker.renderUnicodeCharWorked = true;
        this.mcDefaultFontFlag = false;

        if (!FontConfig.getInstance().performanceMode && FontConfig.getInstance().disableSmallItalic && this.fontScale < 1.05f) {
            italic = false;
        }

        int page = ch / 256;
        ResourceLocation unicodePageLocation = this.changeFont ? this.getOsFontUnicodePageLocation(page) : this.getUnicodePageLocation(page);

        if (this.changeFont) {
            width = this.rasterizer.glyphWidthFloat8[ch];
            if (width == 0.0f) {
                return 0.0f;
            }
            texture = this.fontTextureManager.bindTexture(unicodePageLocation);
            factor = texture.actualFontRes / 16.0f;
            if (this.renderStringAtPosInoperative) {
                this.boldFlag = this.boldChecker.isBold(posX, ch, this.shadowFlag, true);
            }
            left = 0.0f;
            GlyphImage gi = this.rasterizer.getGlyphImage(page);
            switch (this.precisionMode) {
                default: {
                    if (!FontConfig.getInstance().widthErrorCorrection) {
                        break;
                    }
                    if (this.shadowFlag) {
                        posX = this.errCorrectorShadow.getCorrectedPosX(posX, posY, width, this.boldFlag, true);
                        break;
                    }
                    posX = this.errCorrector.getCorrectedPosX(posX, posY, width, this.boldFlag, true);
                    break;
                }
                case 0: {
                    break;
                }
                case 2: {
                    float right = (this.rasterizer.glyphWidthByte[ch] & 0xF) + 1;
                    width = (right - left) / 2.0f;
                }
            }
            if (FontConfig.getInstance().fontAlignBaseline) {
                posY += gi.baselineGap + this.rasterizer.sizeAdjPosY;
            } else {
                int fontId = this.rasterizer.fontId[ch];
                FontProperty fontProp = this.rasterizer.fontProp[1][fontId];
                posY += gi.baselineGap + fontProp.ascentGap;
            }
            texWidthPx = gi.drawingChWidth[ch % 256] * texture.scaleFactor;
            posX -= gi.fontOriginPosX;
        } else {
            if (glyphWidth[ch] == 0) {
                return 0.0f;
            }
            texture = this.fontTextureManager.bindTexture(unicodePageLocation);
            factor = texture.actualFontRes / 16.0f;
            left = (glyphWidth[ch] & 0xF0) >>> 4;
            float right = (glyphWidth[ch] & 0xF) + 1;
            width = (right - left) / 2.0f + 1.0f;
            texWidthPx = (right - left - 0.02f) * factor;
        }
        float leftPx = left * factor;
        this.resetTexEnvAndBlend();
        if (!FontConfig.getInstance().performanceMode) {
            this.fontTextureManager.setAnisotropicFilter(unicodePageLocation, this.anisotropicFilterEnabled);
            if (!this.exclusionCondUnicode) {
                this.fontShader.setShaderParams(this, true);
                this.fontTextureManager.setTexParams(unicodePageLocation, texFilterSettingId);
            } else {
                this.fontTextureManager.setTexParamsNearest(unicodePageLocation);
            }
        } else {
            this.fontTextureManager.setTexParams(unicodePageLocation, texFilterSettingId);
        }
        this.renderCharCommon(ch, italic, posX, posY, leftPx, texWidthPx, factor, texture);
        return width;
    }

    private void renderCharCommon(int ch, boolean italic, float posX, float posY, float leftPx, float texWidthPx, float factor, FontTexture texture) {
        float chImageSize = texture.chImageSizePx;
        float borderWidth = texture.borderWidthPx;
        int texSize = texture.texSizePx;
        float boxSize = chImageSize + borderWidth * 2.0f;
        float texX = (float) (ch % 16) * boxSize + borderWidth + leftPx;
        float texY = (float) ((ch & 0xFF) / 16) * boxSize + borderWidth;
        float it = italic ? 1.0f : 0.0f;

        if (FontConfig.getInstance().performanceMode) {
            if (this.boldFlag && this.getScaleFactor() >= 3) {
                this.renderChar(posX - 0.25f, posY, texX, texY, texWidthPx, it, texSize, chImageSize, factor);
            }

        } else {
            if (this.alignToPixelCond) {
                if (this.shadowFlag && this.fontScale == 1.0f && !this.mcDefaultFontFlag) {
                    posY += 0.5f;
                }
                posY = this.alignToPixel(posY);
            }

            if (this.boldFlag && this.roundedFontScale >= 3.0f) {
                this.renderChar(posX - 0.25f, posY, texX, texY, texWidthPx, it, texSize, chImageSize, factor);
            }
        }

        this.renderChar(posX, posY, texX, texY, texWidthPx, it, texSize, chImageSize, factor);
    }

    private void renderChar(float posX, float posY, float texX, float texY, float width, float italic, int texSize, float chImageSize, float factor) {
        if (this.shadowFlag) {
            posX += shadowAdjustVal;
            posY += shadowAdjustVal;
        }

        float borderWidth = 1.0f;

        float posL = posX - borderWidth / (factor *= 2.0f);
        float posR = posX + (width + borderWidth) / factor;
        float posT = posY - borderWidth / factor;
        float posB = posY + (chImageSize + borderWidth) / factor;
        float texL = (texX - borderWidth) / (float) texSize;
        float texR = (texX + width + borderWidth) / (float) texSize;
        float texT = (texY - borderWidth) / (float) texSize;
        float texB = (texY + chImageSize + borderWidth) / (float) texSize;

        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        GL11.glTexCoord2f(texL, texT);
        GL11.glVertex2f(posL + italic, posT);
        GL11.glTexCoord2f(texL, texB);
        GL11.glVertex2f(posL - italic, posB);
        GL11.glTexCoord2f(texR, texT);
        GL11.glVertex2f(posR + italic, posT);
        GL11.glTexCoord2f(texR, texB);
        GL11.glVertex2f(posR - italic, posB);
        GL11.glEnd();
    }

    public final int drawStringHook(String text, float x, float y, int color, boolean dropShadow, boolean unicodeFlag) {
        int i;
        this.fontRenderer.resetStyles();
        if (this.renderStringInoperative) {
            int newColor = this.renderStringHook(text, color, dropShadow, unicodeFlag);
            if (dropShadow) {
                i = this.fontRenderer.renderString(text, x + 1.0f, y + 1.0f, newColor, true);
                this.renderStringExitHook(text);
                newColor = this.renderStringHook(text, color, false, unicodeFlag);
                i = Math.max(i, this.fontRenderer.renderString(text, x, y, newColor, false));
            } else {
                i = this.fontRenderer.renderString(text, x, y, newColor, false);
            }
            this.renderStringExitHook(text);
        } else {
            if (dropShadow) {
                i = this.fontRenderer.renderString(text, x + 1.0f, y + 1.0f, color, true);
                i = Math.max(i, this.fontRenderer.renderString(text, x, y, color, false));
            } else {
                i = this.fontRenderer.renderString(text, x, y, color, false);
            }
            this.checkRenderStringWorked(text);
        }
        return i;
    }

    private void checkRenderStringWorked(String text) {
        if (!this.renderStringWorked && this.fontRenderer.getStringWidth(text) > 0) {
            Logger.warn("renderString method might be replaced. Enabled fallback hooks in drawString().");
            this.renderStringInoperative = true;
        }
    }

    public void renderStringAtPosEnterHook(String text, boolean unicodeFlag, boolean shadow) {
        this.renderStringAtPosWorked = true;
        this.thinFontFlag = unicodeFlag;

        if (this.renderCharReplacedChecker.needToCheck() && this.renderCharReplacedChecker.isReplaced(this.fontRenderer, text)) {
            this.disableFeatures("renderChar methods might be replaced.");
            return;
        }
        this.thinFontFlag = unicodeFlag || this.changeFont;
        this.shadowFlag = shadow;
        if (!FontConfig.getInstance().performanceMode) {
            Matrix4f mtxMod;
            Matrix4f mtxPrj;
            try {
                this.floatBuf.clear();
                GlStateManager.getFloat(2983, this.floatBuf);
                this.floatBuf.rewind();
                mtxPrj = new Matrix4f();
                mtxPrj.load(this.floatBuf);
                this.floatBuf.clear();
                GlStateManager.getFloat(2982, this.floatBuf);
                this.floatBuf.rewind();
                mtxMod = new Matrix4f();
                mtxMod.load(this.floatBuf);
            } catch (Exception e) {
                this.disableFeatures(e.getMessage());
                return;
            }
            float fontScaleX = ModLib.roundIf(Math.abs(FontRendererImpl.mc.displayWidth * mtxPrj.m00 * mtxMod.m00), 1.0E-6f);
            float fontScaleY = ModLib.roundIf(Math.abs(FontRendererImpl.mc.displayHeight * mtxPrj.m11 * mtxMod.m11), 1.0E-6f);
            this.fontScale = (fontScaleX + fontScaleY) / 4.0f;
            this.rotation = mtxMod.m01 != 0.0f || mtxMod.m10 != 0.0f;
            if (mtxPrj.m22 == -0.001f && mtxPrj.m32 == -2.0f && mtxMod.m32 == -2000.0f) {
                this.orthographic = true;
            } else {
                if (mtxPrj.m33 == 0.0f) {
                    this.orthographic = false;
                    this.fontScale /= Math.abs(mtxMod.m32);
                } else {
                    this.orthographic = true;
                }
            }

            float fontResUnicode = this.fontTextureManager.getUnicodeFontRes(this.changeFont);
            float fontResDefault = this.fontTextureManager.getDefaultFontRes(this.fontRenderer.locationFontTexture, this.changeFont);

            if (this.orthographic) {
                this.fractionCoord = mtxMod.m30 * 10.0f % 5.0f != 0.0f || mtxMod.m31 * 10.0f % 5.0f != 0.0f;
            } else {
                this.fractionCoord = true;
            }

            this.roundedFontScale = ModLib.roundIf(this.fontScale, this.fontScale * FontConfig.getInstance().fontScaleRoundingToleranceRate);
            this.exclusionCondDefault = !shadow || !(fontResDefault >= (float) FontConfig.getInstance().smoothShadowThreshold);
            this.exclusionCondUnicode = !shadow || !(fontResUnicode >= (float) FontConfig.getInstance().smoothShadowThreshold);
            this.exclusionCondDefault &= this.orthographic && FontConfig.getInstance().excludeIntMultiple && this.roundedFontScale % (fontResDefault / 8.0f) == 0.0f || FontConfig.getInstance().excludeHighMag && (double) (this.roundedFontScale * 8.0f) >= (double) fontResDefault * FontConfig.getInstance().limitMagnification;
            this.exclusionCondUnicode &= this.orthographic && FontConfig.getInstance().excludeIntMultiple && this.roundedFontScale % (fontResUnicode / 8.0f) == 0.0f || FontConfig.getInstance().excludeHighMag && (double) (this.roundedFontScale * 8.0f) >= (double) fontResUnicode * FontConfig.getInstance().limitMagnification;
            this.alignToPixelCond = this.changeFont && this.orthographic && !this.rotation && this.roundedFontScale * 2.0f == fontResDefault / 8.0f;

            if (!(!FontConfig.getInstance().enableInterpolation || this.exclusionCondDefault && this.exclusionCondUnicode)) {
                this.fontShader.prepareShader(this);
                this.fontShader.useShader(this);
            }

            if (FontConfig.getInstance().enableMipmap) {
                this.setLodBias();
            }

            this.anisotropicFilterEnabled = !this.orthographic && FontConfig.getInstance().enableAnisotropicFilter;
        } else if (FontConfig.getInstance().enableMipmap) {
            this.setLodBiasPerformance();
        }

        this.setAlphaBlend(false);
        this.onRenderString = true;
    }

    public void renderStringAtPosExitHook() {
        this.onRenderString = false;

        GlStateManagerHelper.restoreGlTexEnvMode();
        GlStateManagerHelper.restoreBlendFunc(false);
        GlStateManagerHelper.restoreBlendEx(false);
        GlStateManagerHelper.restoreTexLodBias();

        this.fontShader.restoreShader();

        GlStateManager.bindTexture(0);
    }

    public int renderStringAtPosGetCharIndexHook(char ch) {
        if (!this.optimized) {
            return "\u00C0\u00C1\u00C2\u00C8\u00CA\u00CB\u00CD\u00D3\u00D4\u00D5\u00DA\u00DF\u00E3\u00F5\u011F\u0130\u0131\u0152\u0153\u015E\u015F\u0174\u0175\u017E\u0207\u0000\u0000\u0000\u0000\u0000\u0000\u0000 !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u0000\u00C7\u00FC\u00E9\u00E2\u00E4\u00E0\u00E5\u00E7\u00EA\u00EB\u00E8\u00EF\u00EE\u00EC\u00C4\u00C5\u00C9\u00E6\u00C6\u00F4\u00F6\u00F2\u00FB\u00F9\u00FF\u00D6\u00DC\u00F8\u00A3\u00D8\u00D7\u0192\u00E1\u00ED\u00F3\u00FA\u00F1\u00D1\u00AA\u00BA\u00BF\u00AE\u00AC\u00BD\u00BC\u00A1\u00AB\u00BB\u2591\u2592\u2593\u2502\u2524\u2561\u2562\u2556\u2555\u2563\u2551\u2557\u255D\u255C\u255B\u2510\u2514\u2534\u252C\u251C\u2500\u253C\u255E\u255F\u255A\u2554\u2569\u2566\u2560\u2550\u256C\u2567\u2568\u2564\u2565\u2559\u2558\u2552\u2553\u256B\u256A\u2518\u250C\u2588\u2584\u258C\u2590\u2580\u03B1\u03B2\u0393\u03C0\u03A3\u03C3\u03BC\u03C4\u03A6\u0398\u03A9\u03B4\u221E\u2205\u2208\u2229\u2261\u00B1\u2265\u2264\u2320\u2321\u00F7\u2248\u00B0\u2219\u00B7\u221A\u207F\u00B2\u25A0\u0000".indexOf(ch);
        }
        return FontUtils.getDefaultGlyphIndex(ch);
    }

    public ResourceLocation getUnicodePageLocation(int page) {
        if (unicodePageLocations[page] == null) {
            FontRendererImpl.unicodePageLocations[page] = new ResourceLocation(String.format("textures/font/unicode_page_%02x.png", page));
        }
        return unicodePageLocations[page];
    }

    private ResourceLocation getOsFontUnicodePageLocation(int page) {
        if (osFontUnicodePageLocations[page] == null) {
            FontRendererImpl.osFontUnicodePageLocations[page] = new ResourceLocation("smoothfont", String.format("osFontUnicodePage_%02x", page));
        }
        return osFontUnicodePageLocations[page];
    }

    public void readFontTextureExitHook() {
        this.mcCharWidth = this.fontRenderer.charWidth;
        this.rasterizer.saveCharWidthOrig(this.mcCharWidth);

        if (this.changeFont) {
            this.rasterizer.restoreCharWidth(this);
            if (this.optifineCharWidthFloat != null) {
                this.rasterizer.restoreCharWidthFloat(this);
            }
        }
    }

    public void readGlyphSizesExitHook() {
        this.rasterizer.saveGlyphWidthOrig(this.fontRenderer.glyphWidth);

        if (this.changeFont) {
            this.rasterizer.restoreGlyphWidth(this);
        }

        char[] sampleChars = new char[]{'1', '/', 'I'};
        this.autoBrightnessDefault = 0;
        this.autoBrightnessUnicode = 0;
        this.brightnessBoundaryScaleFactorDefault = 0.0f;
        this.brightnessBoundaryScaleFactorUnicode = 0.0f;
        int counterDefault = 0;
        int counterUnicode = 0;
        for (char ch : sampleChars) {
            int opacity;
            BufferedImage bufImg = this.getDefaultCharImage(ch);
            if (bufImg != null && (opacity = FontUtils.getTotalOpacityPosY(bufImg, bufImg.getHeight() / 2)) != 0) {
                this.autoBrightnessDefault += FontUtils.getEstimatedBrightness(bufImg.getWidth(), opacity);
                this.brightnessBoundaryScaleFactorDefault += FontUtils.getEstimatedBrightnessBoundaryScaleFactor(bufImg.getWidth(), opacity);
                ++counterDefault;
            }
            if ((bufImg = this.getUnicodeCharImage(ch)) == null || (opacity = FontUtils.getTotalOpacityPosY(bufImg, bufImg.getHeight() / 2)) == 0) {
                continue;
            }
            this.autoBrightnessUnicode += FontUtils.getEstimatedBrightness(bufImg.getWidth(), opacity);
            this.brightnessBoundaryScaleFactorUnicode += FontUtils.getEstimatedBrightnessBoundaryScaleFactor(bufImg.getWidth(), opacity);
            ++counterUnicode;
        }
        if (counterDefault != 0) {
            this.autoBrightnessDefault /= counterDefault;
            this.brightnessBoundaryScaleFactorDefault /= (float) counterDefault;
        } else {
            this.autoBrightnessDefault = 0;
            this.brightnessBoundaryScaleFactorDefault = 0.0f;
        }
        if (counterUnicode != 0) {
            this.autoBrightnessUnicode /= counterUnicode;
            this.brightnessBoundaryScaleFactorUnicode /= (float) counterUnicode;
        } else {
            this.autoBrightnessUnicode = 0;
            this.brightnessBoundaryScaleFactorUnicode = 0.0f;
        }
    }

    public boolean setUnicodeFlagHook(boolean unicodeFlag) {
        if (FontConfig.getInstance().forceUnicode && mc.isUnicode()) {
            return true;
        }

        return unicodeFlag;
    }

    private float getCharWidthFloat(char character) {
        switch (character) {
            case '\u00A7': {
                return -1.0f;
            }
            case ' ':
            case '\u00A0': {
                return this.getSpaceWidth();
            }
        }
        int i = FontUtils.getDefaultGlyphIndex(character);
        if (character > '\u0000' && i != -1 && !this.fontRenderer.getUnicodeFlag()) {
            if (this.changeFont) {
                switch (this.precisionMode) {
                    default: {
                        return this.rasterizer.charWidthFloat[i];
                    }
                    case 1: {
                        return FontUtils.toNormalWidth(this.rasterizer.charWidthFloat[i]);
                    }
                    case 2:
                }
                return this.rasterizer.charWidthInt[i];
            }

            return this.mcCharWidth[i];
        }
        if (this.fontRenderer.glyphWidth[character] != 0) {
            if (this.changeFont) {
                switch (this.precisionMode) {
                    default: {
                        return this.rasterizer.glyphWidthFloat8[character];
                    }
                    case 1: {
                        return FontUtils.toNormalWidth(this.rasterizer.glyphWidthFloat8[character]);
                    }
                    case 2:
                }
                int j = this.rasterizer.glyphWidthByte[character] & 0xFF;
                int k = j >>> 4;
                int l = j & 0xF;
                return (float) (++l - k) / 2;
            }
            int j = this.fontRenderer.glyphWidth[character] & 0xFF;
            int k = j >>> 4;
            int l = j & 0xF;
            ++l;

            return (float) (l - k) / 2 + 1;
        }
        return 0.0f;
    }

    public int renderStringHook(String text, int color, boolean dropShadow, boolean unicodeFlag) {
        this.renderStringWorked = true;

        if (this.renderStringAtPosInoperative) {
            this.renderStringAtPosEnterHook(text, unicodeFlag, dropShadow);
        }

        return color;
    }

    public void renderStringExitHook(String text) {
        if (this.renderStringAtPosInoperative) {
            this.renderStringAtPosExitHook();
        }

        if (text == null) {
            return;
        }

        if (!this.renderStringAtPosWorked && !this.renderStringAtPosInoperative && this.fontRenderer.getStringWidth(text) > 0) {
            Logger.warn("renderStringAtPos method might be replaced.");
            this.renderStringAtPosInoperative = true;
        }

        if (!this.renderCharWorked && !this.immutableSpcWidth && this.fontRenderer.getStringWidth(text) > 0) {
            Logger.warn("renderChar method might be replaced. Fix the space width to 4 (MC default).");
            this.immutableSpcWidth = true;
        }
    }

    public int getStringWidthFloatHook(String text) {
        if (text == null) {
            return 0;
        }
        float width = 0.0f;
        boolean bold = false;
        for (int i = 0; i < text.length(); ++i) {
            char ch = text.charAt(i);
            float chWidth = this.changeFont && this.precisionMode == 0 ? this.getCharWidthFloat(ch) : (float) this.fontRenderer.getCharWidth(ch);
            if (chWidth < 0.0f && i < text.length() - 1) {
                ch = text.charAt(++i);
                chWidth = 0.0f;
                switch (ch) {
                    case 'L':
                    case 'l': {
                        bold = true;
                        break;
                    }
                    case 'R':
                    case 'r': {
                        bold = false;
                        break;
                    }
                }
            }

            width += chWidth;

            if (!bold || !(chWidth > 0.0f)) {
                continue;
            }

            width += 1.0f;
        }

        return (int) width;
    }

    public int getCharWidthHook(char ch) {
        float chWidth = this.getCharWidthFloat(ch);
        return ModLib.roundAny(chWidth, FontConfig.getInstance().charWidthFractionToRoundUpFloat);
    }

    public String trimStringToWidthFloatHook(String text, int width, boolean reverse) {
        StringBuilder stringbuilder = new StringBuilder();
        float totalWidth = 0.0f;
        int length = text.length();
        int start = reverse ? length - 1 : 0;
        int step = reverse ? -1 : 1;
        boolean deco = false;
        boolean bold = false;
        for (int cur = start; cur >= 0 && cur < length && (int) totalWidth < width; cur += step) {
            char ch = text.charAt(cur);
            if (deco) {
                deco = false;
                switch (ch) {
                    case 'L':
                    case 'l': {
                        bold = true;
                        break;
                    }
                    case 'R':
                    case 'r': {
                        bold = false;
                        break;
                    }
                }
            } else if (ch == '\u00A7') {
                deco = true;
            } else {
                totalWidth = this.changeFont && this.precisionMode == 0 ? totalWidth + this.getCharWidthFloat(ch) : totalWidth + (float) this.fontRenderer.getCharWidth(ch);

                if (bold) {
                    totalWidth = totalWidth + 1.0f;
                }
            }

            if ((int) totalWidth > width) {
                break;
            }

            if (reverse) {
                stringbuilder.insert(0, ch);
                continue;
            }

            stringbuilder.append(ch);
        }

        return stringbuilder.toString();
    }

    public int sizeStringToWidthFloatHook(String str, int wrapWidth) {
        int pos;
        int len = str.length();
        float width = 0.0f;
        int breakPos = -1;
        boolean bold = false;
        block9:
        for (pos = 0; pos < len; ++pos) {
            char ch = str.charAt(pos);
            block0:
            switch (ch) {
                case '\n': {
                    breakPos = pos;
                    break block9;
                }
                case '\u00A7': {
                    if (pos >= len - 1) {
                        break;
                    }
                    char decoCode = str.charAt(++pos);
                    switch (decoCode) {
                        case 'L':
                        case 'l': {
                            bold = true;
                            break block0;
                        }
                        case 'R':
                        case 'r': {
                            bold = false;
                            break block0;
                        }
                    }
                    if (!this.isFormatColor(decoCode)) {
                        break;
                    }
                    bold = false;
                    break;
                }
                case ' ': {
                    breakPos = pos;
                }
                default: {
                    width = this.changeFont && this.precisionMode == 0 ? width + this.getCharWidthFloat(ch) : width + (float) this.fontRenderer.getCharWidth(ch);

                    if (!bold) {
                        break;
                    }

                    width += 1.0f;
                }
            }

            if ((int) width > wrapWidth) {
                break;
            }
        }

        return pos != len && breakPos != -1 && breakPos < pos ? breakPos : pos;
    }

    public void doDrawEnterHook() {
        if (this.fontRenderer.strikethroughStyle || this.fontRenderer.underlineStyle) {
            this.fontShader.restoreShaderTemporarily();
        }
    }

    public final int renderStringAlignedHook(String text, int x, int y, int color, boolean dropShadow, boolean unicodeFlag) {
        int posX;

        if (this.renderStringInoperative) {
            int newColor = this.renderStringHook(text, color, dropShadow, unicodeFlag);
            posX = this.fontRenderer.renderString(text, x, y, newColor, dropShadow);
            this.renderStringExitHook(text);
        } else {
            posX = this.fontRenderer.renderString(text, x, y, color, dropShadow);
            this.checkRenderStringWorked(text);
        }

        return posX;
    }

    private boolean isFormatColor(char colorChar) {
        return colorChar >= '0' && colorChar <= '9' || colorChar >= 'a' && colorChar <= 'f' || colorChar >= 'A' && colorChar <= 'F';
    }

    public void bindTextureEnterHook() {
        if (!this.onRenderString) {
            return;
        }

        this.fontShader.restoreShaderTemporarily();
        GlStateManagerHelper.restoreGlTexEnvMode();
        this.needToResetTexEnvAndBlend = true;
    }

    private void resetTexEnvAndBlend() {
        if (this.needToResetTexEnvAndBlend) {
            this.setAlphaBlend(true);
            this.fontShader.resetShader(this);
            this.needToResetTexEnvAndBlend = false;
        }
    }

    private void setAlphaBlend(boolean reset) {
        if (FontConfig.getInstance().enableAlphaBlend) {
            GlStateManagerHelper.setBlendEx(reset);
            if (FontConfig.getInstance().enablePremultipliedAlpha) {
                GlStateManagerHelper.setBlendFunc(1, 771, reset);
            } else {
                GlStateManagerHelper.setBlendFunc(770, 771, reset);
            }
        }
    }

    public void reloadResources() {
        try {
            this.fontRenderer.readFontTexture();
        } catch (Exception e) {
            Logger.info("Exception from readFontTexture(). The font texture file may not exist.");
        }

        try {
            this.fontRenderer.readGlyphSizes();
        } catch (Exception e) {
            Logger.info("Exception from readGlyphSizes(). The glyph_sizes.bin may not exist.");
        }
    }

    private void disableFeatures(String reason) {
        Logger.info("Disabled smoothfont functions.(reason:" + reason + "):" + this.fontRenderer.toString());
        this.reasonForDisable = reason;
        this.reloadResources();
    }

    private BufferedImage getDefaultCharImage(char ch) {
        int id = FontUtils.getDefaultGlyphIndex(ch);

        if (id == -1) {
            return null;
        }

        BufferedImage fontImg = FontUtils.getMCFontImage(this.fontRenderer.locationFontTexture);

        if (fontImg == null) {
            return null;
        }

        int chSize = fontImg.getWidth() / 16;
        int posX = id % 16 * chSize;
        int posY = (id & 0xFF) / 16 * chSize;

        return fontImg.getSubimage(posX, posY, chSize, chSize);
    }

    private BufferedImage getUnicodeCharImage(char ch) {
        int page = ch / 256;
        BufferedImage fontImg = FontUtils.getMCFontImage(this.getUnicodePageLocation(page));
        if (fontImg == null) {
            return null;
        }
        int chSize = fontImg.getWidth() / 16;
        int posX = ch % 16 * chSize;
        int posY = (ch & 0xFF) / 16 * chSize;
        return fontImg.getSubimage(posX, posY, chSize, chSize);
    }

    public void updateChangeFontFlag() {
        if (!modLoaded) {
            return;
        }

        this.changeFont = true;
    }

    private float getSpaceWidth() {
        if (!this.immutableSpcWidth && this.changeFont) {
            if (FontConfig.getInstance().fontSpaceWidth == 0) {
                switch (this.precisionMode) {
                    case 0: {
                        if (this.fontRenderer.getUnicodeFlag()) {
                            return this.rasterizer.glyphWidthFloat8[32];
                        }

                        return this.rasterizer.charWidthFloat[32];
                    }
                    case 1: {
                        if (FontConfig.getInstance().widthErrorCorrection) {
                            return ErrorCorrector.getCorrectedSpaceWidth(this.fontRenderer.getUnicodeFlag());
                        }
                    }
                    case 2: {
                        if (this.fontRenderer.getUnicodeFlag()) {
                            return MathHelper.ceil(this.rasterizer.glyphWidthFloat8[32]);
                        }

                        return MathHelper.ceil(this.rasterizer.charWidthFloat[32]);
                    }
                }
            } else {
                return FontConfig.getInstance().fontSpaceWidth;
            }
        }

        return 4.0f;
    }

    private float alignToPixel(float pos) {
        float scaleFactor = this.fontScale;
        return (float) ModLib.round(pos * scaleFactor) / scaleFactor;
    }

    private int getScaleFactor() {
        return new ScaledResolution(mc).getScaleFactor();
    }
}
 