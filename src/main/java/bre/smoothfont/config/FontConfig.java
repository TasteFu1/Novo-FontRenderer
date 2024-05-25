// =============================================== //
// Recompile disabled. Please run Recaf with a JDK //
// =============================================== //

// Decompiled with: CFR 0.152
// Class Version: 8
package bre.smoothfont.config;

public class FontConfig {
    private static final FontConfig instance = new FontConfig();

    @SuppressWarnings({"UnusedAssignment", "RedundantSuppression"})
    public float fontScaleRoundingToleranceRate = 0.0F, charWidthFractionToRoundUpFloat = 0.5F, mipmapLodBiasFloat = 0.0F, overlayLodBiasFloat = 0.0F;
    public boolean enableInterpolation = true;
    public boolean enableAlphaBlend = true;
    public boolean enablePremultipliedAlpha = true;
    public boolean excludeIntMultiple = true;
    public double fontScaleRoundingTolerance = 0.5;
    public boolean excludeHighMag = true;
    public double limitMagnification = 3.0;
    public boolean disableSmallItalic = false;
    public boolean enableMipmap = true;
    public int mipmapLevel = 4;
    public int mipmapLodBias = -3;
    public int overlayLodBias = -5;
    public int brightness = 3;
    public boolean autoBrightness = true;
    public int shadowLength = 5;
    public int blurReduction = 10;
    public boolean allowNPOTTexture = false;
    public boolean enableAnisotropicFilter = true;
    public boolean forceUnicode = false;
    public double charWidthFractionToRoundUp = 0.3333335;
    public boolean performanceMode = false;
    public int runMode = 0;
    public boolean widthErrorCorrection = true;
    public int smoothShadowThreshold = 24;
    public String fontName = "Poppins Medium";
    public String secondaryFontName = "Arial";
    public int primaryFontStyle = 0;
    public int secondaryFontStyle = 0;
    public int fontAntiAlias = 2;
    public int fontEmphasis = 0;
    public int fontResIndex = 7;
    public boolean fontAutoSizing = true;
    public double fontSizeScaleFactor = 1.0;
    public double widthFactorDefaultCharset = 1.0;
    public double widthFactorUnicodeCharset = 1.0;
    public int fontGap = 0;
    public int fontSpaceWidth = 0;
    public boolean fontAlignBaseline = true;
    public int glyphImageMargin = 1;
    public String fontSizingRefChars = "";

    public int multiThread = 2;
    public float autosizingTgtAscent = 0.85F;
    public float referenceBaseline = 0.875F;
    public boolean workaroundWrongGlState;

    private FontConfig() {
        this.fontScaleRoundingToleranceRate = (float) this.fontScaleRoundingTolerance * 0.01F;
        this.charWidthFractionToRoundUpFloat = (float) this.charWidthFractionToRoundUp;
        this.mipmapLodBiasFloat = (float) this.mipmapLodBias / 10.0F;
        this.overlayLodBiasFloat = (float) this.overlayLodBias / 10.0F;
    }

    public static FontConfig getInstance() {
        return instance;
    }
}
 