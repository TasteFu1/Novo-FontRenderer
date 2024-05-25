// =============================================== //
// Recompile disabled. Please run Recaf with a JDK //
// =============================================== //

// Decompiled with: CFR 0.152
// Class Version: 8
package bre.smoothfont;
import bre.smoothfont.renderer.TTFFontRenderer;

public class RenderCharReplacedChecker {
    public boolean renderDefaultCharWorked = false;
    public boolean renderUnicodeCharWorked = false;
    private int prevTextLength = 0;

    public boolean isReplaced(TTFFontRenderer renderer, String text) {
        if (this.prevTextLength > 0) {
            return !this.renderDefaultCharWorked && !this.renderUnicodeCharWorked;
        }

        String chars = text.replace("\u00A0", "").replace(" ", "");
        int textLen = renderer.getStringWidth(chars);

        if (textLen > 0) {
            this.prevTextLength = textLen;
        }

        return false;
    }

    public boolean needToCheck() {
        return !this.renderDefaultCharWorked && !this.renderUnicodeCharWorked;
    }
}
 