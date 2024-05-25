// =============================================== //
// Recompile disabled. Please run Recaf with a JDK //
// =============================================== //

// Decompiled with: CFR 0.152
// Class Version: 8
package bre.smoothfont;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import bre.smoothfont.config.FontConfig;
import bre.smoothfont.renderer.FontRendererImpl;
import bre.smoothfont.util.Logger;
import bre.smoothfont.util.ModLib;

public class FontRasterizer {
    private static final FontRasterizer INSTANCE = new FontRasterizer();
    private int fontRes;
    private int fontGap;
    private boolean antiAlias;
    private boolean fractionalMetrics;
    private int fontDouble;
    public FontProperty[][] fontProp = new FontProperty[2][3];
    public boolean glyphsGenerationError = false;
    public String glyphsGenerationErrorMessage;
    public int[] charWidthInt = new int[256];
    public byte[] glyphWidthByte = new byte[65536];
    private int[] charWidthOrig = null;
    private byte[] glyphWidthOrig = null;
    public float[] charWidthFloat = new float[256];
    public float[] glyphWidthFloat = new float[65536];
    public float[] glyphWidthFloat8 = new float[65536];
    public int[] fontId = new int[65536];
    public float fontGapAdjWidth;
    public float sizeAdjPosY;
    private float maxFontHeight;
    private final GlyphImage[] glyphImageCache = new GlyphImage[257];
    private boolean allFontCacheAvailable = false;
    public long totalImageSize;
    public int autoBrightnessValue = 3;
    public float brightnessBoundaryScaleFactor = 2.0f;

    public boolean initFontCache(boolean async) {
        if (this.glyphsGenerationError) {
            return false;
        }

        boolean result = true;
        FontConfig config = FontConfig.getInstance();

        if (!this.allFontCacheAvailable) {
            result = this.setFontSafely(config.fontName, config.secondaryFontName, config.primaryFontStyle, config.secondaryFontStyle, FontUtils.getFontRes(config.fontResIndex), config.fontGap, config.fontSizeScaleFactor, config.fontAntiAlias, config.fontEmphasis, config.widthFactorDefaultCharset, config.widthFactorUnicodeCharset, async);
        }

        return result;
    }

    public boolean setFontSafely(String fontName, String secondaryFontName, int primaryFontStyle, int secondaryFontStyle, int fontRes, int fontGap, double fontSizeAdjustment, int antiAlias, int fontDouble, double widthFactorDefFont, double widthFactorUniFont, boolean async) {
        try {
            this.glyphsGenerationError = false;
            this.glyphsGenerationErrorMessage = "";
            this.setFont(fontName, secondaryFontName, primaryFontStyle, secondaryFontStyle, fontRes, fontGap, fontSizeAdjustment, antiAlias, fontDouble, widthFactorDefFont, widthFactorUniFont, async);
        } catch (Throwable throwable) {
            this.glyphsGenerationError = true;
            this.glyphsGenerationErrorMessage = throwable instanceof TimeoutException ? "Glyph images pre-rendering timeout." : throwable.toString();
            Logger.error("***** Caught the exception during setFont(). Abort changing fonts. *****");
            throwable.printStackTrace();
            this.clearFontCache();
            return false;
        }
        return true;
    }

    public void setFont(String fontName, String secondaryFontName, int primaryFontStyle, int secondaryFontStyle, int fontRes, int fontGap, double fontSizeAdjustment, int antiAlias, int fontDouble, double widthFactorDefFont, double widthFactorUniFont, boolean async) throws InterruptedException, TimeoutException {
        float stdBaseline;
        FontMeasure basicFm;

        FontConfig config = FontConfig.getInstance();

        this.antiAlias = antiAlias >= 1;
        this.fractionalMetrics = antiAlias >= 2;
        this.fontDouble = fontDouble;
        this.fontGap = fontGap;
        this.fontRes = fontRes;
        this.fontGapAdjWidth = (float) -this.fontGap * (float) this.fontRes / 16.0f;

        this.fontProp[0][0] = new FontProperty(fontName, primaryFontStyle, fontRes, fontSizeAdjustment, this.antiAlias, this.fractionalMetrics, widthFactorDefFont);
        this.fontProp[1][0] = new FontProperty(fontName, primaryFontStyle, fontRes, fontSizeAdjustment, this.antiAlias, this.fractionalMetrics, widthFactorUniFont);

        this.fontProp[0][1] = new FontProperty(secondaryFontName, secondaryFontStyle, fontRes, fontSizeAdjustment, this.antiAlias, this.fractionalMetrics, widthFactorDefFont);
        this.fontProp[1][1] = new FontProperty(secondaryFontName, secondaryFontStyle, fontRes, fontSizeAdjustment, this.antiAlias, this.fractionalMetrics, widthFactorUniFont);

        this.fontProp[0][2] = new FontProperty("Arial", primaryFontStyle, fontRes, fontSizeAdjustment, this.antiAlias, this.fractionalMetrics, widthFactorDefFont);
        this.fontProp[1][2] = new FontProperty("Arial", primaryFontStyle, fontRes, fontSizeAdjustment, this.antiAlias, this.fractionalMetrics, widthFactorUniFont);

        float mcFontBaseline = this.getMcFontBaseline(fontRes);
        String typicalChars = FontMeasure.getStdSizingRefChars();

        if (this.fontProp[0][0].available && this.fontProp[0][0].font.canDisplayUpTo(typicalChars) == -1) {
            basicFm = new FontMeasure(this.fontProp[0][0].font, this.antiAlias, this.fractionalMetrics, typicalChars);
            stdBaseline = this.fontProp[0][0].stdBaseline;
        } else if (this.fontProp[0][1].available && this.fontProp[0][1].font.canDisplayUpTo(typicalChars) == -1) {
            basicFm = new FontMeasure(this.fontProp[0][1].font, this.antiAlias, this.fractionalMetrics, typicalChars);
            stdBaseline = this.fontProp[0][1].stdBaseline;
        } else {
            basicFm = new FontMeasure(this.fontProp[0][2].font, this.antiAlias, this.fractionalMetrics, typicalChars);
            stdBaseline = this.fontProp[0][2].stdBaseline;
        }

        float apparentBaseline = basicFm.getAscentProxy() + basicFm.getLeadingProxy();
        this.sizeAdjPosY = (float) ModLib.round((apparentBaseline - stdBaseline) / 2.0f) * (8.0f / (float) fontRes);

        for (FontProperty fntProp : this.fontProp[0]) {
            this.maxFontHeight = Math.max(this.maxFontHeight, fntProp.fm.getHeightProxy());
        }

        this.fontProp[0][0].ascentGap = this.fontProp[0][0].baseline * (8.0f / (float) fontRes) - mcFontBaseline;
        this.fontProp[1][0].ascentGap = this.fontProp[0][0].baseline * (8.0f / (float) fontRes) - mcFontBaseline;

        this.fontProp[0][1].ascentGap = this.fontProp[0][1].baseline * (8.0f / (float) fontRes) - mcFontBaseline;
        this.fontProp[1][1].ascentGap = this.fontProp[0][1].baseline * (8.0f / (float) fontRes) - mcFontBaseline;

        this.fontProp[0][2].ascentGap = this.fontProp[0][2].baseline * (8.0f / (float) fontRes) - mcFontBaseline;
        this.fontProp[1][2].ascentGap = this.fontProp[0][2].baseline * (8.0f / (float) fontRes) - mcFontBaseline;

        this.autoBrightnessValue = 255;
        this.brightnessBoundaryScaleFactor = 2.0f;

        this.clearFontCache();

        if (!async || FontRendererImpl.modLoaded) {
            FontTextureManager.getInstance().clearMapTextureObjects();
        }

        this.createFontCacheAll();

        if (this.autoBrightnessValue == 255) {
            config.brightness = this.autoBrightnessValue = 0;
            Logger.warn("Failed to detect auto-brightness value. Use the default value.");
        }

        ErrorCorrector.calcErrorAverage(fontRes);
    }

    public void clearFontCache() {
        for (int i = 0; i < 257; ++i) {
            this.glyphImageCache[i] = null;
        }

        this.totalImageSize = 0L;
        this.allFontCacheAvailable = false;
    }

    public void createFontCacheAll() throws InterruptedException, TimeoutException {
        int i;
        String prerenderTime = "Pre-rendering time for all textures";
        this.clearFontCache();
        ModLib.startCounter(prerenderTime);
        switch (FontConfig.getInstance().multiThread) {
            case 0: {
                for (i = 0; i < 257; ++i) {
                    this.generateGlyphImage(i);
                }
                break;
            }
            case 1: {
                int i2;
                CreateFontImageThread[] thread = new CreateFontImageThread[257];
                for (i2 = 0; i2 < 256; ++i2) {
                    thread[i2] = new CreateFontImageThread(i2);
                    thread[i2].start();
                }
                for (i2 = 0; i2 < 256; ++i2) {
                    try {
                        thread[i2].join();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                this.generateGlyphImage(256);
                break;
            }
            default: {
                int workingThreadNum = this.getSuitableThreadNum();
                Logger.info("Number of threads used for generating font images: " + workingThreadNum);
                ExecutorService threadPool = Executors.newFixedThreadPool(workingThreadNum);
                for (int i3 = 0; i3 < 256; ++i3) {
                    threadPool.submit(new CreateFontImageThread(i3));
                }
                threadPool.shutdown();
                try {
                    long timeout = (long) (10.0f * (workingThreadNum > 2 ? 1.0f : 1.5f));
                    boolean result = threadPool.awaitTermination(timeout, TimeUnit.MINUTES);
                    if (!result) {
                        ModLib.stopCounterMs(prerenderTime, false);
                        List<Runnable> unexecutedTasks = threadPool.shutdownNow();
                        int completed = 256 - unexecutedTasks.size();
                        throw new TimeoutException("Pre-rendering of glyph textures has timed out. (" + completed + "/256)");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    ModLib.stopCounterMs(prerenderTime, false);
                    List<Runnable> unexecutedTasks = threadPool.shutdownNow();
                    int completed = 256 - unexecutedTasks.size();
                    throw new InterruptedException("awaitTermination interrupted. (" + completed + "/256)");
                }
                this.generateGlyphImage(256);
            }
        }

        ModLib.stopCounterMs(prerenderTime);
        this.totalImageSize = 0L;

        for (i = 0; i < 257; ++i) {
            this.totalImageSize += this.glyphImageCache[i].dataSize;
        }

        this.totalImageSize += FontUtils.getBytesFromImage(GlyphImage.blankImage).length;
        Logger.info("Total memory used for the current platform font images: " + this.totalImageSize / 1024L / 1024L + "MB");
        this.allFontCacheAvailable = true;
    }

    public static FontRasterizer getInstance() {
        return INSTANCE;
    }

    public GlyphImage generateGlyphImage(int page) {
        int fontSet = page == 256 ? 0 : 1;
        this.glyphImageCache[page] = new GlyphImage(this, this.fontProp[fontSet], page, this.fontRes, this.antiAlias, this.fractionalMetrics, this.fontDouble);
        return this.glyphImageCache[page];
    }

    public GlyphImage getGlyphImage(int page) {
        return this.glyphImageCache[page];
    }

    public void saveCharWidthOrig(int[] cw) {
        if (this.charWidthOrig == null) {
            int i;
            int totalCharWidth = 0;
            for (i = 0; i < cw.length; ++i) {
                totalCharWidth += cw[i];
            }
            if (totalCharWidth > 0) {
                this.charWidthOrig = new int[256];
                for (i = 0; i < cw.length; ++i) {
                    this.charWidthOrig[i] = cw[i];
                }
            }
        }
    }

    public void saveGlyphWidthOrig(byte[] gw) {
        if (this.glyphWidthOrig == null) {
            this.glyphWidthOrig = new byte[65536];
            System.arraycopy(gw, 0, this.glyphWidthOrig, 0, gw.length);
        }
    }

    public void restoreCharWidth(FontRendererImpl frh) {
        int i;
        if (this.charWidthOrig != null) {
            for (i = 0; i < 256; ++i) {
                if (this.charWidthOrig[i] != 0) continue;
                this.charWidthInt[i] = 0;
            }
        }
        if (frh.keepMcFontWidth) {
            return;
        }
        if (this.charWidthInt != null) {
            for (i = 0; i < 256; ++i) {
                frh.mcCharWidth[i] = this.charWidthInt[i];
            }
        }
    }

    public void restoreCharWidthFloat(FontRendererImpl frh) {
        int i;
        if (this.charWidthOrig != null) {
            for (i = 0; i < 256; ++i) {
                if (this.charWidthOrig[i] != 0) continue;
                this.charWidthFloat[i] = 0.0f;
            }
        }
        if (frh.keepMcFontWidth) {
            return;
        }
        if (this.charWidthFloat != null) {
            for (i = 0; i < 256; ++i) {
                frh.optifineCharWidthFloat[i] = this.charWidthFloat[i];
            }
        }
    }

    public void restoreGlyphWidth(FontRendererImpl frh) {
        int i;
        if (this.glyphWidthOrig != null) {
            for (i = 0; i < 65536; ++i) {
                if (this.glyphWidthOrig[i] != 0) continue;
                this.glyphWidthByte[i] = 0;
            }
        }
        if (frh.keepMcFontWidth) {
            return;
        }
        if (this.glyphWidthByte != null) {
            for (i = 0; i < 65536; ++i) {
                frh.fontRenderer.glyphWidth[i] = this.glyphWidthByte[i];
            }
        }
    }

    private int getSuitableThreadNum() {
        return Runtime.getRuntime().availableProcessors();
    }

    public float getMcFontBaseline(int fontRes) {
        float mcBaseline = FontConfig.getInstance().referenceBaseline * 8.0f;
        float scaleFactor = (float) fontRes / 8.0f;

        if (fontRes == 12) {
            scaleFactor *= 2.0f;
        }

        mcBaseline = (float) ModLib.round(mcBaseline * scaleFactor) / scaleFactor;
        return mcBaseline;
    }

    class CreateFontImageThread extends Thread {
        int page;

        private CreateFontImageThread(int page) {
            this.page = page;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public void run() {
            FontRasterizer.this.generateGlyphImage(this.page);
        }
    }
}
 