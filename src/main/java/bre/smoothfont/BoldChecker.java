// =============================================== //
// Recompile disabled. Please run Recaf with a JDK //
// =============================================== //

// Decompiled with: CFR 0.152
// Class Version: 8
package bre.smoothfont;

public class BoldChecker {
    int[] prevChar = new int[2];
    float[] prevPosX = new float[2];

    public BoldChecker() {
        this.prevPosX[0] = Float.MAX_VALUE;
        this.prevPosX[1] = Float.MAX_VALUE;
    }

    public boolean isBold(float posX, int ch, boolean shadow, boolean unicode) {
        boolean bold = false;
        int idx = shadow ? 1 : 0;
        float gap = unicode ? 0.5f : 1.0f;

        if (ch == this.prevChar[idx] && posX == this.prevPosX[idx] + gap) {
            bold = true;
        }
        this.prevChar[idx] = ch;
        this.prevPosX[idx] = posX;
        return bold;
    }
}
 