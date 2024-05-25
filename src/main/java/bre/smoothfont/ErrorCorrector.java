// =============================================== //
// Recompile disabled. Please run Recaf with a JDK //
// =============================================== //

// Decompiled with: CFR 0.152
// Class Version: 8
package bre.smoothfont;

import net.minecraft.util.math.MathHelper;

import java.util.HashMap;
import java.util.Map;

public class ErrorCorrector {
    private float nextHighPosX = Float.MAX_VALUE;
    private float nextNormPosX = Float.MAX_VALUE;
    private float nextNormPosY;
    private float prevHighPosX = Float.MAX_VALUE;
    private float prevNormPosX = Float.MAX_VALUE;
    private static final float[] charWidthError = new float[256];
    private static final float[] glyphWidthError = new float[65536];
    public static float charWidthErrAverage;
    public static float glyphWidthErrAverage;
    private static float charWidthCorrection;
    private static float charWidthCorrectionHead;
    private static float glyphWidthCorrection;
    private static float glyphWidthCorrectionHead;
    public static FontRasterizer rasterizer;
    private static final Map<Character, Float> letterFreq;

    public float getCorrectedPosX(float posX, float posY, float width, boolean bold, boolean unicode) {
        boolean head = false;
        if (posY == this.nextNormPosY && posX == this.nextNormPosX) {
            posX = this.nextHighPosX;
        } else if (bold) {
            posX = this.prevHighPosX + 0.5f;
            this.nextHighPosX = this.prevHighPosX + 1.0f;
            this.nextNormPosX = this.prevNormPosX + 1.0f;
        } else {
            this.nextHighPosX = posX;
            this.nextNormPosX = posX;
            this.nextNormPosY = posY;
            head = true;
        }
        this.prevHighPosX = this.nextHighPosX;
        this.prevNormPosX = this.nextNormPosX;
        this.nextHighPosX += width;
        this.nextNormPosX += (float) FontUtils.toNormalWidth(width);
        float adjustVal = unicode ? head ? glyphWidthCorrectionHead : glyphWidthCorrection : head ? charWidthCorrectionHead : charWidthCorrection;
        float correction = this.nextHighPosX > this.nextNormPosX ? -adjustVal : 0.0f;
        this.nextHighPosX += correction;
        return posX;
    }

    public static void calcErrorAverage(int fontRes) {
        String sampleChars = "abcdefghijklmnopqrstuvwxyz";

        float totalCharWidthError = 0.0f;
        float totalGlyphWidthError = 0.0f;

        for (char ch : sampleChars.toCharArray()) {
            int id = FontUtils.getDefaultGlyphIndex(ch);
            totalCharWidthError += charWidthError[id] * letterFreq.get(ch);
            totalGlyphWidthError += glyphWidthError[ch] * letterFreq.get(ch);
        }

        charWidthErrAverage = totalCharWidthError;
        glyphWidthErrAverage = totalGlyphWidthError;

        if (fontRes == 12 || fontRes == 24 || fontRes == 48) {
            if (charWidthErrAverage < -0.3333333f) {
                charWidthCorrection = 0.5f;
                charWidthCorrectionHead = 0.3333333f;
            } else {
                charWidthCorrection = 0.3333333f;
                charWidthCorrectionHead = 0.0f;
            }
            if (glyphWidthErrAverage < -0.3333333f) {
                glyphWidthCorrection = 0.5f;
                glyphWidthCorrectionHead = 0.3333333f;
            } else {
                glyphWidthCorrection = 0.3333333f;
                glyphWidthCorrectionHead = 0.0f;
            }
        } else {
            if (charWidthErrAverage < -0.25f) {
                charWidthCorrection = 0.5f;
                charWidthCorrectionHead = 0.25f;
            } else {
                charWidthCorrection = 0.25f;
                charWidthCorrectionHead = 0.0f;
            }
            if (glyphWidthErrAverage < -0.25f) {
                glyphWidthCorrection = 0.5f;
                glyphWidthCorrectionHead = 0.25f;
            } else {
                glyphWidthCorrection = 0.25f;
                glyphWidthCorrectionHead = 0.0f;
            }
        }
    }

    public static float getCorrectedSpaceWidth(boolean unicodeFlag) {
        return unicodeFlag ? glyphWidthErrAverage < 0.0f ? (float) (int) ErrorCorrector.rasterizer.glyphWidthFloat8[32] + 1.0f : (float) MathHelper.ceil(ErrorCorrector.rasterizer.glyphWidthFloat8[32]) : charWidthErrAverage < 0.0f ? (float) (int) ErrorCorrector.rasterizer.charWidthFloat[32] + 1.0f : (float) MathHelper.ceil(ErrorCorrector.rasterizer.charWidthFloat[32]);
    }

    public static void setCharWidthError(int id, float width) {
        ErrorCorrector.charWidthError[id] = width == 0.0f ? 0.0f : (float) FontUtils.toNormalWidth(width) - width;
    }

    public static void setGlyphWidthError(int ch, float width) {
        ErrorCorrector.glyphWidthError[ch] = width == 0.0f ? 0.0f : (float) FontUtils.toNormalWidth(width) - width;
    }

    static {
        rasterizer = FontRasterizer.getInstance();
        letterFreq = new HashMap<Character, Float>() {
            {
                this.put('a', 0.08167f);
                this.put('b', 0.01492f);
                this.put('c', 0.02782f);
                this.put('d', 0.04253f);
                this.put('e', 0.12702f);
                this.put('f', 0.02228f);
                this.put('g', 0.02015f);
                this.put('h', 0.06094f);
                this.put('i', 0.06966f);
                this.put('j', 0.00153f);
                this.put('k', 0.00772f);
                this.put('l', 0.04025f);
                this.put('m', 0.02406f);
                this.put('n', 0.06749f);
                this.put('o', 0.07507f);
                this.put('p', 0.01929f);
                this.put('q', 9.5E-4f);
                this.put('r', 0.05987f);
                this.put('s', 0.06327f);
                this.put('t', 0.09056f);
                this.put('u', 0.02758f);
                this.put('v', 0.00978f);
                this.put('w', 0.0236f);
                this.put('x', 0.0015f);
                this.put('y', 0.01972f);
                this.put('z', 7.4E-4f);
            }
        };
    }
}
