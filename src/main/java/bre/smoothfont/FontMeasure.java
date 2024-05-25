// =============================================== //
// Recompile disabled. Please run Recaf with a JDK //
// =============================================== //

// Decompiled with: CFR 0.152
// Class Version: 8
package bre.smoothfont;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphMetrics;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import bre.smoothfont.config.FontConfig;
import bre.smoothfont.util.Logger;
import bre.smoothfont.util.ModLib;

public class FontMeasure {
    private final Font font;
    private final boolean antiAlias;
    private final boolean fractionalMetrics;
    private final FontRenderContext frc;
    private final double boundsHeight;
    private final double boundsAscent;
    private final int pixelBoundsHeight;
    private final int pixelBoundsAscent;
    private final int pixelBoundsDescent;
    private final double maxWidthWithLsb;
    private char maxWidthWithLsbChar;
    private final char maxCharHeightChar;
    private final double minLSB;
    private char minLSBChar;
    private final String testString;

    public FontMeasure(Font font, boolean antiAlias, boolean fractionalMetrics) {
        this(font, antiAlias, fractionalMetrics, null);
    }

    public FontMeasure(Font font, boolean antiAlias, boolean fractionalMetrics, String testChars) {
        this.font = font;
        this.antiAlias = antiAlias;
        this.fractionalMetrics = fractionalMetrics;
        font.getTransform();
        this.frc = new FontRenderContext(null, this.antiAlias, this.fractionalMetrics);
        if (testChars == null) {
            String referenceChars = FontMeasure.getStdSizingRefChars();
            this.testString = this.getDisplayableChars(referenceChars);
        } else {
            this.testString = this.getDisplayableChars(testChars);
        }
        GlyphVector gv = font.createGlyphVector(this.frc, this.testString);
        Rectangle2D rectangle = gv.getVisualBounds();
        this.boundsHeight = rectangle.getHeight();
        this.boundsAscent = Math.abs(rectangle.getY());
        Rectangle pixelRect = gv.getPixelBounds(null, 0.0f, 0.0f);
        this.pixelBoundsHeight = ModLib.round(pixelRect.getHeight());
        this.pixelBoundsAscent = ModLib.round(Math.abs(pixelRect.getY()));
        this.pixelBoundsDescent = this.pixelBoundsHeight - this.pixelBoundsAscent;
        this.getMaxWidth(this.testString);
        this.maxWidthWithLsb = this.getMaxWidthWithLsb(this.testString);
        this.maxCharHeightChar = this.getMaxHeightChar(this.testString);
        this.getCharBoundsHeight(this.maxCharHeightChar);
        this.minLSB = this.getMinLSB(this.testString);
        FontMetrics fontMetrics = this.getFontMetrics(font, antiAlias, fractionalMetrics);
        fontMetrics.getMaxAscent();
        fontMetrics.getMaxDescent();
        fontMetrics.getLeading();
        fontMetrics.getAscent();
        fontMetrics.getDescent();
    }

    public float getLeadingProxy() {
        return 0.0f;
    }

    public float getHeightProxy() {
        return this.getBoundsHeight();
    }

    public float getAscentProxy() {
        return this.getBoundsAscent();
    }

    private float getBoundsHeight() {
        return (float) this.boundsHeight;
    }

    private float getBoundsAscent() {
        return (float) this.boundsAscent;
    }

    public int getPixelBoundsHeight() {
        return this.pixelBoundsHeight;
    }

    public int getPixelBoundsAscent() {
        return this.pixelBoundsAscent;
    }

    public int getPixelBoundsDescent() {
        return this.pixelBoundsDescent;
    }

    public float getMaxWidthWithLsb() {
        return (float) this.maxWidthWithLsb;
    }

    public char getMaxWidthWithLsbChar() {
        return this.maxWidthWithLsbChar;
    }

    public float getMinLSB() {
        return (float) this.minLSB;
    }

    public char getMinLSBChar() {
        return this.minLSBChar;
    }

    public char getMaxCharHeightChar() {
        return this.maxCharHeightChar;
    }

    public float getFittedSize(int pixelSize, float startSize, float step) {
        float size = startSize;
        float targetAscent = (float) pixelSize * FontConfig.getInstance().autosizingTgtAscent;

        while (true) {
            Font workFont = this.font.deriveFont(size + step);
            FontMeasure fm = new FontMeasure(workFont, this.antiAlias, this.fractionalMetrics);

            float maxHeight = fm.getBoundsHeight();
            float maxAscent = fm.getBoundsAscent();

            if (size > 10.0f && maxHeight == 0.0f) {
                Logger.warn("Invalid font height.");
                size = 0.0f;
                break;
            }

            if (!(maxAscent < targetAscent)) {
                break;
            }

            size += step;
        }

        return size;
    }

    private double getCharBoundsWidth(char ch) {
        GlyphVector gv = this.font.createGlyphVector(this.frc, String.valueOf(ch));
        Rectangle2D rectangle = gv.getVisualBounds();
        return rectangle.getWidth();
    }

    private double getCharBoundsHeight(char ch) {
        GlyphVector gv = this.font.createGlyphVector(this.frc, String.valueOf(ch));
        Rectangle2D rectangle = gv.getVisualBounds();
        return rectangle.getHeight();
    }

    public double getCharWidthByStringBounds(char ch) {
        FontRenderContext frc = new FontRenderContext(null, this.antiAlias, this.fractionalMetrics);
        Rectangle2D rectangle = this.font.getStringBounds(String.valueOf(ch), frc);
        return rectangle.getWidth();
    }

    public double getXOffsetByStringBounds(char ch) {
        FontRenderContext frc = new FontRenderContext(null, this.antiAlias, this.fractionalMetrics);
        Rectangle2D rectangle = this.font.getStringBounds(String.valueOf(ch), frc);
        return rectangle.getX();
    }

    private double getMaxWidth(String testStr) {
        double maxCharWidth = 0.0;
        for (char ch : testStr.toCharArray()) {
            double chWidth = this.getCharBoundsWidth(ch);
            if (!(chWidth > maxCharWidth)) {
                continue;
            }
            maxCharWidth = chWidth;
        }
        return maxCharWidth;
    }

    private double getMaxWidthWithLsb(String testStr) {
        double maxCharWidth = 0.0;

        for (char ch : testStr.toCharArray()) {
            float lsb = this.getLSB(ch);
            lsb = Math.max(lsb, 0.0f);
            double chWidth = this.getCharBoundsWidth(ch) + (double) lsb;

            if (chWidth > maxCharWidth) {
                maxCharWidth = chWidth;
                this.maxWidthWithLsbChar = ch;
            }
        }

        return maxCharWidth;
    }

    private char getMaxHeightChar(String testStr) {
        double maxCharHeight = 0.0;
        char maxHeightChar = '\u0000';

        for (char ch : testStr.toCharArray()) {
            double chHeight = this.getCharBoundsHeight(ch);

            if (chHeight > maxCharHeight) {
                maxCharHeight = chHeight;
                maxHeightChar = ch;
            }
        }

        return maxHeightChar;
    }

    public float getRSB(char ch) {
        GlyphMetrics gm = this.font.createGlyphVector(this.frc, String.valueOf(ch)).getGlyphMetrics(0);
        return gm.getRSB();
    }

    private float getLSB(char ch) {
        float lsb;
        GlyphMetrics gm = this.font.createGlyphVector(this.frc, String.valueOf(ch)).getGlyphMetrics(0);
        try {
            lsb = this.getLSB(gm);
        } catch (RuntimeException re) {
            Logger.warn(String.format("getLSB(%Xh): ", ch) + re.getMessage());
            lsb = 0.0f;
        }
        return lsb;
    }

    private float getLSB(GlyphMetrics metrics) {
        double width = metrics.getBounds2D().getWidth();
        if (width < 0.0) {
            throw new RuntimeException(String.format("GlyphMetrics.getBounds2D().getWidth() returned the abnormal value %f.", width));
        }
        return metrics.getLSB();
    }

    private double getMinLSB(String testStr) {
        double minLSB = 32767.0;
        for (char ch : testStr.toCharArray()) {
            switch (Character.getType(ch)) {
                case 6:
                case 7:
                case 8:
                case 23:
                case 27: {
                    continue;
                }
                default: {
                    double lsb = this.getLSB(ch);
                    if (!(lsb < minLSB)) {
                        continue;
                    }
                    this.minLSBChar = ch;
                    minLSB = lsb;
                }
            }
        }
        return minLSB;
    }

    private String getDisplayableChars(String str) {
        StringBuilder builder = new StringBuilder();
        int len = str.length();
        for (int i = 0; i < len; ++i) {
            char c = str.charAt(i);
            if (!this.font.canDisplay(c)) {
                continue;
            }
            builder.append(c);
        }
        return new String(builder);
    }

    private FontMetrics getFontMetrics(Font font, boolean antiAlias, boolean fractionalMetrics) {
        BufferedImage workImage = new BufferedImage(8, 8, 6);
        Graphics2D g2 = workImage.createGraphics();
        g2.setFont(font);
        FontUtils.setAntiAlias(g2, antiAlias, fractionalMetrics);
        FontMetrics fontMetrics = g2.getFontMetrics();
        g2.dispose();
        return fontMetrics;
    }

    public static String getStdSizingRefChars() {
        String referenceChars = FontConfig.getInstance().fontSizingRefChars.trim();

        if (referenceChars.isEmpty()) {
            referenceChars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        }

        return referenceChars;
    }
}
 