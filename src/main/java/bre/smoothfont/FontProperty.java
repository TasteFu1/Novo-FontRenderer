// =============================================== //
// Recompile disabled. Please run Recaf with a JDK //
// =============================================== //

// Decompiled with: CFR 0.152
// Class Version: 8
package bre.smoothfont;

import net.minecraft.util.math.MathHelper;

import java.awt.Font;
import java.awt.geom.AffineTransform;

import bre.smoothfont.config.FontConfig;
import bre.smoothfont.util.Logger;

public class FontProperty {
    public Font font;
    public float size;
    public boolean available;
    public FontMeasure fm;
    public float baseline;
    public float stdBaseline;
    private final int fontRes;
    private final boolean antiAlias;
    private final boolean fractionalMetrics;
    private final double widthFactor;
    private float pointSize;
    public float ascentGap;

    public FontProperty(String fontName, int fontStyle, int fontRes, double scalingFactor, boolean antiAlias, boolean fractionalMetrics, double widthFactor) {
        this.fontRes = fontRes;
        this.antiAlias = antiAlias;
        this.fractionalMetrics = fractionalMetrics;
        this.widthFactor = widthFactor;
        this.font = new Font(fontName, fontStyle, fontRes);

        AffineTransform affineTransform = new AffineTransform();
        affineTransform.setToScale(this.widthFactor, 1.0);

        this.font = this.font.deriveFont(affineTransform);
        this.pointSize = FontConfig.getInstance().fontAutoSizing ? this.getMaxSizePoint(this.font) : (float) fontRes;

        this.font = this.font.deriveFont(this.pointSize);
        this.fm = new FontMeasure(this.font, antiAlias, fractionalMetrics);

        this.stdBaseline = this.fm.getAscentProxy() + this.fm.getLeadingProxy();
        this.pointSize = (float) ((double) this.pointSize * scalingFactor);

        this.font = this.font.deriveFont(this.pointSize);
        this.fm = new FontMeasure(this.font, antiAlias, fractionalMetrics);

        this.baseline = this.fm.getAscentProxy() + this.fm.getLeadingProxy();
        this.baseline = MathHelper.ceil(this.baseline);

        this.available = FontUtils.isFontAvailable(this.font);
    }

    private float getMaxSizePoint(Font font) {
        FontMeasure fm = new FontMeasure(font, this.antiAlias, this.fractionalMetrics);
        float point = fm.getFittedSize(this.fontRes, 0.0f, 1.0f);
        if (point > 0.0f) {
            point = fm.getFittedSize(this.fontRes, point, 0.1f);
        }
        Logger.info("Suitable font size is " + point + " (" + font.getFontName() + ")");
        return point;
    }
}
 