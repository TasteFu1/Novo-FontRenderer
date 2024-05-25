// =============================================== //
// Recompile disabled. Please run Recaf with a JDK //
// =============================================== //

// Decompiled with: CFR 0.152
// Class Version: 8
package bre.smoothfont;

import org.apache.commons.lang3.CharUtils;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphMetrics;
import java.awt.font.GlyphVector;
import java.awt.geom.RectangularShape;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import bre.smoothfont.config.FontConfig;
import bre.smoothfont.util.ModLib;

public class GlyphImage {
    private final int page;
    private byte[] glyphImageData;
    private BufferedImage glyphImage;
    public int fontRes;
    private final boolean antiAlias;
    private final boolean fractionalMetrics;
    private boolean compression;
    private boolean grayScale;
    public float lsbAdjWidth;
    public float baselineGap;
    public float fontOriginPosX;
    public float maxHeight;
    public float maxWidth;
    public float minLSB;
    public int chImageBoxOriginX;
    public int chImageBoxSize;
    public int borderSize;
    public float baseline;
    public float[] drawingChWidth = new float[256];
    public char minLSBChar;
    public char maxWidthChar;
    public char maxHeightChar;
    public int dataSize;
    public int numOfGlyphs;
    public long processingTime;
    private final FontRasterizer rasterizer;
    public static BufferedImage blankImage;

    public GlyphImage(FontRasterizer rasterizer, FontProperty[] fontProps, int page, int fontRes, boolean antiAlias, boolean fractionalMetrics, int fontDouble) {
        String genGlyphImage = "genGlyphImage-" + page;
        ModLib.startCounter(genGlyphImage);

        this.rasterizer = rasterizer;
        this.page = page;
        this.fontRes = fontRes;
        this.antiAlias = antiAlias;
        this.fractionalMetrics = fractionalMetrics;
        this.grayScale = grayScale;
        this.compression = compression;
        this.borderSize = FontUtils.getBorderWidth(fontRes);

        this.setGlyphImageParams(fontProps, page);

        if (blankImage == null) {
            int imageWidth = 8;
            blankImage = grayScale ? new BufferedImage(imageWidth, imageWidth, 10) : new BufferedImage(imageWidth, imageWidth, 6);
        }

        this.generateGlyphImage(this.page, fontProps, fontDouble);
        this.processingTime = ModLib.stopCounterMs(genGlyphImage, false);
    }

    public void setGlyphImage(BufferedImage glyphImage, boolean compressImage) {
        this.compression = compressImage;
        this.glyphImageData = null;
        this.glyphImage = null;
        this.grayScale = glyphImage.getType() == 10;

        if (compressImage) {
            byte[] byteData = FontUtils.getBytesFromImage(glyphImage);
            this.glyphImageData = FontUtils.gzipBytes(byteData);
            this.dataSize = this.glyphImageData.length;
        } else {
            this.glyphImage = glyphImage;
            this.dataSize = FontUtils.getBytesFromImage(glyphImage).length;
        }
    }

    public BufferedImage getGlyphImage() {
        if (this.compression) {
            if (this.glyphImageData != null) {
                byte[] gunzipBytes = FontUtils.gunzipBytes(this.glyphImageData);
                return this.grayScale ? FontUtils.getGrayImageFromGrayBytes(gunzipBytes) : FontUtils.getABGRImageFromABGRBytes(gunzipBytes);
            }
        } else if (this.glyphImage != null) {
            return this.glyphImage;
        }
        return null;
    }

    private void setGlyphImageParams(FontProperty[] fontProps, int page) {
        int chImgBoxWidthCandidate;
        ArrayList<Character> charList = new ArrayList<Character>();
        String unmeasuredChars = "\u2031\uF0CC";
        if (page < 256) {
            int startChar;
            for (int ch = startChar = page * 256; ch < startChar + 256; ++ch) {
                if (unmeasuredChars.contains(String.valueOf((char) ch))) {
                    continue;
                }
                charList.add((char) ch);
            }
        } else {
            for (char ch : FontUtils.asciiSheetChars) {
                if (unmeasuredChars.contains(String.valueOf(ch))) {
                    continue;
                }
                charList.add(ch);
            }
        }
        this.maxHeight = 0.0f;
        this.maxWidth = 0.0f;
        this.minLSB = 0.0f;
        int margin = FontConfig.getInstance().glyphImageMargin;
        float maxAscent = 0.0f;
        float maxDescent = 0.0f;
        float maxLeading = 0.0f;
        for (FontProperty fontProp : fontProps) {
            float minLSBWork;
            float maxWidthWork;
            StringBuilder testChars = new StringBuilder();
            ArrayList<Character> removeList = new ArrayList<Character>();
            for (char ch : charList) {
                if (!fontProp.font.canDisplay(ch)) {
                    continue;
                }
                testChars.append(ch);
                removeList.add(ch);
            }
            charList.removeAll(removeList);
            if (testChars.isEmpty()) {
                continue;
            }
            FontMeasure fm = new FontMeasure(fontProp.font, this.antiAlias, this.fractionalMetrics, testChars.toString());
            float maxHeightWork = fm.getPixelBoundsHeight();
            if (maxHeightWork > this.maxHeight) {
                this.maxHeight = maxHeightWork;
                this.maxHeightChar = fm.getMaxCharHeightChar();
            }
            if ((maxWidthWork = fm.getMaxWidthWithLsb()) > this.maxWidth) {
                this.maxWidth = maxWidthWork;
                this.maxWidthChar = fm.getMaxWidthWithLsbChar();
            }
            if ((minLSBWork = fm.getMinLSB()) < this.minLSB) {
                this.minLSB = minLSBWork;
                this.minLSBChar = fm.getMinLSBChar();
            }
            maxAscent = Math.max(maxAscent, (float) fm.getPixelBoundsAscent());
            maxDescent = Math.max(maxDescent, (float) fm.getPixelBoundsDescent());
            maxLeading = Math.max(maxLeading, 0.0f);
        }
        this.maxHeight = maxAscent + maxDescent + maxLeading;
        this.baseline = ModLib.round(maxAscent + maxLeading) + margin;
        this.baseline -= this.baseline % 2.0f;
        this.baselineGap = this.rasterizer.getMcFontBaseline(this.fontRes) - this.baseline * 8.0f / (float) this.fontRes;
        this.chImageBoxOriginX = ModLib.round(Math.abs(this.minLSB)) + margin;
        this.chImageBoxOriginX += this.chImageBoxOriginX % 2;
        this.lsbAdjWidth = (float) this.chImageBoxOriginX * 16.0f / (float) this.fontRes;
        this.fontOriginPosX = this.lsbAdjWidth / 2.0f;
        int chImgBoxHeightCandidate = ModLib.round(this.maxHeight) + margin * 2;
        this.chImageBoxSize = chImgBoxHeightCandidate > (chImgBoxWidthCandidate = ModLib.round(this.maxWidth) + this.chImageBoxOriginX + margin) ? chImgBoxHeightCandidate : chImgBoxWidthCandidate;
        this.chImageBoxSize += this.chImageBoxSize % 2;
        this.chImageBoxSize = Math.max(this.chImageBoxSize, 4);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private BufferedImage generateGlyphImage(int page, FontProperty[] fontProp, int fontDouble) {
        float scaleFactor = (float) this.fontRes / 16.0f;
        int imageWidth = (this.chImageBoxSize + this.borderSize * 2) * 16;
        BufferedImage fontImage = this.getEmptyFontImage(imageWidth, imageWidth);
        Graphics2D g2 = fontImage.createGraphics();
        if (this.grayScale) {
            FontUtils.clearGraphics2DGray(g2, 0, 0, imageWidth, imageWidth);
        } else {
            FontUtils.clearGraphics2D(g2, 0, 0, imageWidth, imageWidth);
        }
        BufferedImage chImage = this.getEmptyFontImage(this.chImageBoxSize, this.chImageBoxSize);
        Graphics2D g2ch = chImage.createGraphics();
        FontUtils.setAntiAlias(g2, this.antiAlias, this.fractionalMetrics);
        FontUtils.setAntiAlias(g2ch, this.antiAlias, this.fractionalMetrics);
        int startColumn;
        int endColumn;
        int glyphGap = this.borderSize * 2;
        int noFontCnt = 0;
        int chBase = page == 256 ? 0 : page << 8;

        FontConfig config = FontConfig.getInstance();

        for (int y = 0; y < 16; ++y) {
            for (int x = 0; x < 16; ++x) {
                int leftPos = 0;
                int id = y * 16 + x;
                boolean canDisplay = true;
                g2ch.setFont(fontProp[0].font);
                char ch = page == 256 ? FontUtils.asciiSheetChars[id] : (char) (chBase + id);
                this.rasterizer.fontId[ch] = 0;
                Font fontWork = fontProp[0].font;
                FontMeasure fm = fontProp[0].fm;

                if (!fontProp[0].available || !fontProp[0].font.canDisplay(ch)) {
                    if (fontProp[1].available && fontProp[1].font.canDisplay(ch)) {
                        this.rasterizer.fontId[ch] = 1;
                        g2ch.setFont(fontProp[1].font);
                        fontWork = fontProp[1].font;
                        fm = fontProp[1].fm;

                    } else {
                        this.rasterizer.fontId[ch] = 2;
                        g2ch.setFont(fontProp[2].font);
                        fontWork = fontProp[2].font;
                        fm = fontProp[2].fm;

                        if (!fontProp[2].font.canDisplay(ch)) {
                            ++noFontCnt;
                            canDisplay = false;
                        }
                    }
                }
                FontRenderContext frc = new FontRenderContext(null, this.antiAlias, this.fractionalMetrics);
                GlyphMetrics gm = fontWork.createGlyphVector(frc, String.valueOf(ch)).getGlyphMetrics(0);
                float chWidth = gm.getAdvanceX();
                int type = Character.getType(ch);
                boolean combiningMark = false;
                int combiningMarkXOffset = 0;

                switch (type) {
                    case 8, 6, 7: {
                        combiningMark = true;
                        break;
                    }
                    default: {
                        g2ch.setColor(Color.white);
                    }
                }

                if (combiningMark) {
                    float combiningMarkXOffsetF;
                    chWidth = (float) fm.getCharWidthByStringBounds(ch);
                    if (chWidth != 0.0f) {
                        combiningMarkXOffsetF = (float) fm.getXOffsetByStringBounds(ch);
                    } else {
                        GlyphVector gv = fontWork.createGlyphVector(frc, String.valueOf(ch));
                        Rectangle rectangle = gv.getPixelBounds(null, 0.0f, 0.0f);
                        chWidth = (float) ((RectangularShape) rectangle).getWidth();
                        combiningMarkXOffsetF = (float) ((RectangularShape) rectangle).getX();
                    }
                    combiningMarkXOffsetF = Math.min(combiningMarkXOffsetF, 0.0f);
                    combiningMarkXOffset = (int) combiningMarkXOffsetF;
                }
                if (canDisplay && chWidth != 0.0f) {
                    int opacity;
                    this.drawingChWidth[id] = (float) this.chImageBoxOriginX + chWidth + Math.max(-fm.getRSB(ch), 0.0f);
                    this.drawingChWidth[id] = Math.min(this.drawingChWidth[id], (float) (this.chImageBoxSize - leftPos));
                    if ((chWidth -= this.rasterizer.fontGapAdjWidth) < 0.01f) {
                        chWidth = 0.01f;
                    }
                    if (this.grayScale) {
                        FontUtils.clearGraphics2DGray(g2ch, 0, 0, this.chImageBoxSize, this.chImageBoxSize);
                    } else {
                        FontUtils.clearGraphics2D(g2ch, 0, 0, this.chImageBoxSize, this.chImageBoxSize);
                    }

                    g2ch.drawString(String.valueOf(ch), (float) (this.chImageBoxOriginX + leftPos - combiningMarkXOffset), this.baseline);

                    if (fontDouble == 1 && CharUtils.isAscii(ch) || fontDouble == 2 && !CharUtils.isAscii(ch) || fontDouble == 3) {
                        g2ch.drawString(String.valueOf(ch), (float) (this.chImageBoxOriginX + leftPos - combiningMarkXOffset), this.baseline);
                    }

                    g2.drawImage(chImage, (this.chImageBoxSize + glyphGap) * x + this.borderSize, (this.chImageBoxSize + glyphGap) * y + this.borderSize, null);
                    float chWidthFloat = chWidth / scaleFactor;

                    if (page == 256) {
                        this.rasterizer.charWidthFloat[id] = chWidthFloat / 2.0f;
                        this.rasterizer.charWidthInt[id] = (int) ((chWidthFloat + 1.0f) / 2.0f);

                        ErrorCorrector.setCharWidthError(id, chWidthFloat / 2.0f);
                    } else {
                        endColumn = (int) (((float) leftPos + chWidth) / scaleFactor);
                        startColumn = (int) ((float) leftPos / scaleFactor);

                        startColumn = Math.max(startColumn, 0);
                        endColumn = Math.min(endColumn, 15);

                        this.rasterizer.glyphWidthByte[ch] = (byte) (startColumn << 4 | endColumn & 0xF);
                        this.rasterizer.glyphWidthFloat[ch] = chWidthFloat;
                        this.rasterizer.glyphWidthFloat8[ch] = chWidthFloat / 2.0f;

                        ErrorCorrector.setGlyphWidthError(ch, chWidthFloat / 2.0f);
                    }

                    if (ch != '1' && ch != 'I' || (opacity = this.grayScale ? FontUtils.getTotalOpacityPosYGray(chImage, (int) (this.baseline * 0.75f)) : FontUtils.getTotalOpacityPosY(chImage, (int) (this.baseline * 0.75f))) == 0) {
                        continue;
                    }

                    int estimatedBrightnessValue = FontUtils.getEstimatedBrightness(this.fontRes, opacity);

                    synchronized (this.rasterizer) {
                        this.rasterizer.brightnessBoundaryScaleFactor = Math.max(this.rasterizer.brightnessBoundaryScaleFactor, FontUtils.getEstimatedBrightnessBoundaryScaleFactor(this.fontRes, opacity));
                        this.rasterizer.autoBrightnessValue = Math.min(this.rasterizer.autoBrightnessValue, estimatedBrightnessValue);

                        if (config.autoBrightness) {
                            config.brightness = this.rasterizer.autoBrightnessValue;
                        }
                    }

                    continue;
                }

                if (page == 256) {
                    this.rasterizer.charWidthInt[id] = 0;
                    this.rasterizer.charWidthFloat[id] = 0.0f;
                    ErrorCorrector.setCharWidthError(id, 0.0f);
                    continue;
                }

                this.rasterizer.glyphWidthByte[ch] = 0;
                this.rasterizer.glyphWidthFloat[ch] = 0.0f;
                this.rasterizer.glyphWidthFloat8[ch] = 0.0f;
                ErrorCorrector.setGlyphWidthError(ch, 0.0f);
            }
        }

        g2ch.dispose();
        g2.dispose();

        if (config.enablePremultipliedAlpha) {
            fontImage = FontUtils.convertToPremultipliedAlpha(fontImage);
        }

        if (noFontCnt == 256) {
            fontImage = blankImage;
        }

        this.setGlyphImage(fontImage, this.compression);
        this.numOfGlyphs = 256 - noFontCnt;

        if (fontImage == blankImage && !this.compression) {
            this.dataSize = 0;
        }

        return fontImage;
    }

    private BufferedImage getEmptyFontImage(int width, int height) {
        return this.grayScale ? new BufferedImage(width, height, 10) : new BufferedImage(width, height, 6);
    }
}
 