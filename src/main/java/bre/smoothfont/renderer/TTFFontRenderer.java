package bre.smoothfont.renderer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;

import org.apache.commons.io.IOUtils;

import bre.smoothfont.FontUtils;

@SuppressWarnings("unused")
public class TTFFontRenderer {
    private final String charData = "\u00c0\u00c1\u00c2\u00c8\u00ca\u00cb\u00cd\u00d3\u00d4\u00d5\u00da\u00df\u00e3\u00f5\u011f\u0130\u0131\u0152\u0153\u015e\u015f\u0174\u0175\u017e\u0207\u0000\u0000\u0000\u0000\u0000\u0000\u0000 !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u0000\u00c7\u00fc\u00e9\u00e2\u00e4\u00e0\u00e5\u00e7\u00ea\u00eb\u00e8\u00ef\u00ee\u00ec\u00c4\u00c5\u00c9\u00e6\u00c6\u00f4\u00f6\u00f2\u00fb\u00f9\u00ff\u00d6\u00dc\u00f8\u00a3\u00d8\u00d7\u0192\u00e1\u00ed\u00f3\u00fa\u00f1\u00d1\u00aa\u00ba\u00bf\u00ae\u00ac\u00bd\u00bc\u00a1\u00ab\u00bb\u2591\u2592\u2593\u2502\u2524\u2561\u2562\u2556\u2555\u2563\u2551\u2557\u255d\u255c\u255b\u2510\u2514\u2534\u252c\u251c\u2500\u253c\u255e\u255f\u255a\u2554\u2569\u2566\u2560\u2550\u256c\u2567\u2568\u2564\u2565\u2559\u2558\u2552\u2553\u256b\u256a\u2518\u250c\u2588\u2584\u258c\u2590\u2580\u03b1\u03b2\u0393\u03c0\u03a3\u03c3\u03bc\u03c4\u03a6\u0398\u03a9\u03b4\u221e\u2205\u2208\u2229\u2261\u00b1\u2265\u2264\u2320\u2321\u00f7\u2248\u00b0\u2219\u00b7\u221a\u207f\u00b2\u25a0\u0000";

    public final int[] charWidth = new int[256];

    public int FONT_HEIGHT = 9;
    private final Random fontRandom = new Random();

    public final byte[] glyphWidth = new byte[65536];
    private final int[] colorCode = new int[32];

    public final ResourceLocation locationFontTexture;
    private final TextureManager renderEngine;

    private float posX;
    private float posY;

    private boolean unicodeFlag;

    private float red;
    private float blue;
    private float green;
    private float alpha;

    private int textColor;

    private boolean randomStyle;
    private boolean boldStyle;
    private boolean italicStyle;
    public boolean underlineStyle;
    public boolean strikethroughStyle;

    private final FontRendererImpl fontRendererImpl;

    public TTFFontRenderer(ResourceLocation location, TextureManager textureManagerIn, boolean unicode) {
        this.fontRendererImpl = new FontRendererImpl(this);

        this.locationFontTexture = location;
        this.renderEngine = textureManagerIn;
        this.unicodeFlag = unicode;

        this.bindTexture(this.locationFontTexture);

        for (int i = 0; i < 32; ++i) {
            int j = (i >> 3 & 1) * 85;
            int k = (i >> 2 & 1) * 170 + j;
            int l = (i >> 1 & 1) * 170 + j;
            int i1 = (i & 1) * 170 + j;

            if (i == 6) {
                k += 85;
            }

            if (i >= 16) {
                k /= 4;
                l /= 4;
                i1 /= 4;
            }

            this.colorCode[i] = (k & 255) << 16 | (l & 255) << 8 | i1 & 255;
        }

        this.reloadResources();
        this.fontRendererImpl.fontRendererExitHook();
    }

    public void reloadResources() {
        this.readFontTexture();
        this.readGlyphSizes();
    }

    public void readFontTexture() {
        IResource iresource = null;
        BufferedImage bufferedimage;

        try {
            iresource = Minecraft.getMinecraft().getResourceManager().getResource(this.locationFontTexture);
            bufferedimage = TextureUtil.readBufferedImage(iresource.getInputStream());
        } catch (IOException ioexception) {
            throw new RuntimeException(ioexception);
        } finally {
            IOUtils.closeQuietly(iresource);
        }

        int lvt_3_2_ = bufferedimage.getWidth();
        int lvt_4_1_ = bufferedimage.getHeight();
        int[] lvt_5_1_ = new int[lvt_3_2_ * lvt_4_1_];
        bufferedimage.getRGB(0, 0, lvt_3_2_, lvt_4_1_, lvt_5_1_, 0, lvt_3_2_);
        int lvt_6_1_ = lvt_4_1_ / 16;
        int lvt_7_1_ = lvt_3_2_ / 16;
        float lvt_9_1_ = 8.0F / (float) lvt_7_1_;

        for (int lvt_10_1_ = 0; lvt_10_1_ < 256; ++lvt_10_1_) {
            int j1 = lvt_10_1_ % 16;
            int k1 = lvt_10_1_ / 16;

            if (lvt_10_1_ == 32) {
                this.charWidth[lvt_10_1_] = 4;
            }

            int l1;

            for (l1 = lvt_7_1_ - 1; l1 >= 0; --l1) {
                int i2 = j1 * lvt_7_1_ + l1;
                boolean flag1 = true;

                for (int j2 = 0; j2 < lvt_6_1_ && flag1; ++j2) {
                    int k2 = (k1 * lvt_7_1_ + j2) * lvt_3_2_;

                    if ((lvt_5_1_[i2 + k2] >> 24 & 255) != 0) {
                        flag1 = false;
                        break;
                    }
                }

                if (!flag1) {
                    break;
                }
            }

            ++l1;
            this.charWidth[lvt_10_1_] = (int) (0.5D + (double) ((float) l1 * lvt_9_1_)) + 1;
        }

        this.fontRendererImpl.readFontTextureExitHook();
    }

    public void readGlyphSizes() {
        IResource iresource = null;

        try {
            iresource = Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation("font/glyph_sizes.bin"));
            iresource.getInputStream().read(this.glyphWidth);
        } catch (IOException ioexception) {
            throw new RuntimeException(ioexception);
        } finally {
            IOUtils.closeQuietly(iresource);
        }

        this.fontRendererImpl.readGlyphSizesExitHook();
    }

    private float renderChar(char ch, boolean italic) {
        if (ch == ' ') {
            return 4.0F;
        }

        float hookedWidth = this.fontRendererImpl.renderCharHook(ch);

        if (hookedWidth != -1.0F) {
            return hookedWidth;
        }

        int i = charData.indexOf(ch);
        return i != -1 && !this.unicodeFlag ? this.renderDefaultChar(i, italic) : this.renderUnicodeChar(ch, italic);
    }

    private float renderDefaultChar(int ch, boolean italic) {
        return this.fontRendererImpl.renderDefaultCharHook(ch, italic, posX, posY);
    }

    private float renderUnicodeChar(char ch, boolean italic) {
        return this.fontRendererImpl.renderUnicodeCharHook(ch, italic, glyphWidth, posX, posY);
    }

    public int drawStringWithShadow(String text, float x, float y, int color) {
        return this.drawString(text, x, y, color, true);
    }

    public int drawString(String text, int x, int y, int color) {
        return this.drawString(text, (float) x, (float) y, color, false);
    }

    public int drawString(String text, float x, float y, int color, boolean dropShadow) {
        return this.fontRendererImpl.drawStringHook(text, x, y, color, dropShadow, unicodeFlag);
    }

    public void resetStyles() {
        this.randomStyle = false;
        this.boldStyle = false;
        this.italicStyle = false;
        this.underlineStyle = false;
        this.strikethroughStyle = false;
    }

    private void renderStringAtPos(String text, boolean shadow) {
        this.fontRendererImpl.renderStringAtPosEnterHook(text, this.unicodeFlag, shadow);

        for (int i = 0; i < text.length(); ++i) {
            char c0 = text.charAt(i);

            if (c0 == 167 && i + 1 < text.length()) {
                int i1 = "0123456789abcdefklmnor".indexOf(String.valueOf(text.charAt(i + 1)).toLowerCase(Locale.ROOT).charAt(0));

                if (i1 < 16) {
                    this.randomStyle = false;
                    this.boldStyle = false;
                    this.strikethroughStyle = false;
                    this.underlineStyle = false;
                    this.italicStyle = false;

                    if (i1 < 0) {
                        i1 = 15;
                    }

                    if (shadow) {
                        i1 += 16;
                    }

                    int j1 = this.colorCode[i1];
                    this.textColor = j1;
                    GlStateManager.color((float) (j1 >> 16) / 255.0F, (float) (j1 >> 8 & 255) / 255.0F, (float) (j1 & 255) / 255.0F, this.alpha);
                } else if (i1 == 16) {
                    this.randomStyle = true;
                } else if (i1 == 17) {
                    this.boldStyle = true;
                } else if (i1 == 18) {
                    this.strikethroughStyle = true;
                } else if (i1 == 19) {
                    this.underlineStyle = true;
                } else if (i1 == 20) {
                    this.italicStyle = true;
                } else {
                    this.randomStyle = false;
                    this.boldStyle = false;
                    this.strikethroughStyle = false;
                    this.underlineStyle = false;
                    this.italicStyle = false;
                    GlStateManager.color(this.red, this.blue, this.green, this.alpha);
                }

                ++i;
            } else {
                int j = this.fontRendererImpl.renderStringAtPosGetCharIndexHook(c0);

                if (this.randomStyle && j != -1) {
                    int k = this.getCharWidth(c0);
                    char c1;

                    do {
                        j = this.fontRandom.nextInt(charData.length());
                        c1 = charData.charAt(j);

                    } while (k != this.getCharWidth(c1));

                    c0 = c1;
                }

                boolean unicodeFlag = this.fontRendererImpl.thinFontFlag;

                float f1 = unicodeFlag ? 0.5F : 1.0F;
                boolean flag = (c0 == 0 || j == -1 || unicodeFlag) && shadow;

                if (flag) {
                    this.posX -= f1;
                    this.posY -= f1;
                }

                float f = this.renderChar(c0, this.italicStyle);

                if (flag) {
                    this.posX += f1;
                    this.posY += f1;
                }

                if (this.boldStyle) {
                    this.posX += f1;

                    if (flag) {
                        this.posX -= f1;
                        this.posY -= f1;
                    }

                    this.renderChar(c0, this.italicStyle);
                    this.posX -= f1;

                    if (flag) {
                        this.posX += f1;
                        this.posY += f1;
                    }

                    ++f;
                }

                this.doDraw(FontUtils.toNormalWidth(f));
            }
        }

        this.fontRendererImpl.renderStringAtPosExitHook();
    }


    protected void doDraw(float f) {
        this.fontRendererImpl.doDrawEnterHook();

        if (this.strikethroughStyle) {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuffer();
            GlStateManager.disableTexture2D();
            bufferbuilder.begin(7, DefaultVertexFormats.POSITION);
            bufferbuilder.pos(this.posX, this.posY + (float) (this.FONT_HEIGHT / 2), 0.0D).endVertex();
            bufferbuilder.pos(this.posX + f, this.posY + (float) (this.FONT_HEIGHT / 2), 0.0D).endVertex();
            bufferbuilder.pos(this.posX + f, this.posY + (float) (this.FONT_HEIGHT / 2) - 1.0F, 0.0D).endVertex();
            bufferbuilder.pos(this.posX, this.posY + (float) (this.FONT_HEIGHT / 2) - 1.0F, 0.0D).endVertex();
            tessellator.draw();
            GlStateManager.enableTexture2D();
        }

        if (this.underlineStyle) {
            Tessellator tessellator1 = Tessellator.getInstance();
            BufferBuilder bufferbuilder1 = tessellator1.getBuffer();
            GlStateManager.disableTexture2D();
            bufferbuilder1.begin(7, DefaultVertexFormats.POSITION);
            int l = this.underlineStyle ? -1 : 0;
            bufferbuilder1.pos(this.posX + (float) l, this.posY + (float) this.FONT_HEIGHT, 0.0D).endVertex();
            bufferbuilder1.pos(this.posX + f, this.posY + (float) this.FONT_HEIGHT, 0.0D).endVertex();
            bufferbuilder1.pos(this.posX + f, this.posY + (float) this.FONT_HEIGHT - 1.0F, 0.0D).endVertex();
            bufferbuilder1.pos(this.posX + (float) l, this.posY + (float) this.FONT_HEIGHT - 1.0F, 0.0D).endVertex();
            tessellator1.draw();
            GlStateManager.enableTexture2D();
        }

        this.posX += (float) (int) f;
    }

    private int renderStringAligned(String text, int x, int y, int color, boolean dropShadow) {
        return this.fontRendererImpl.renderStringAlignedHook(text, x, y, color, dropShadow, unicodeFlag);
    }

    public int renderString(String text, float x, float y, int color, boolean dropShadow) {
        if ((color & -67108864) == 0) {
            color |= -16777216;
        }

        if (dropShadow) {
            color = (color & 16579836) >> 2 | color & -16777216;
        }

        color = this.fontRendererImpl.renderStringHook(text, color, dropShadow, unicodeFlag);

        this.red = (float) (color >> 16 & 255) / 255.0F;
        this.blue = (float) (color >> 8 & 255) / 255.0F;
        this.green = (float) (color & 255) / 255.0F;
        this.alpha = (float) (color >> 24 & 255) / 255.0F;

        GlStateManager.color(this.red, this.blue, this.green, this.alpha);

        this.posX = x;
        this.posY = y;

        this.renderStringAtPos(text, dropShadow);
        this.fontRendererImpl.renderStringExitHook(text);

        return (int) this.posX;
    }

    public int getStringWidth(String text) {
        return this.fontRendererImpl.getStringWidthFloatHook(text);
    }

    public int getCharWidth(char character) {
        return this.fontRendererImpl.getCharWidthHook(character);
    }

    public String trimStringToWidth(String text, int width) {
        return this.trimStringToWidth(text, width, false);
    }

    public String trimStringToWidth(String text, int width, boolean reverse) {
        return this.fontRendererImpl.trimStringToWidthFloatHook(text, width, reverse);
    }

    private String trimStringNewline(String text) {
        while (text.endsWith("\n")) {
            text = text.substring(0, text.length() - 1);
        }

        return text;
    }

    public void drawSplitString(String str, int x, int y, int wrapWidth, int textColor) {
        this.resetStyles();
        this.textColor = textColor;
        str = this.trimStringNewline(str);
        this.renderSplitString(str, x, y, wrapWidth, false);
    }

    private void renderSplitString(String str, int x, int y, int wrapWidth, boolean addShadow) {
        for (String s : this.listFormattedStringToWidth(str, wrapWidth)) {
            this.renderStringAligned(s, x, y, this.textColor, addShadow);
            y += this.FONT_HEIGHT;
        }
    }

    public int splitStringWidth(String str, int maxLength) {
        return this.FONT_HEIGHT * this.listFormattedStringToWidth(str, maxLength).size();
    }

    public void setUnicodeFlag(boolean unicodeFlagIn) {
        this.unicodeFlag = this.fontRendererImpl.setUnicodeFlagHook(unicodeFlagIn);
    }

    public boolean getUnicodeFlag() {
        return this.unicodeFlag;
    }

    public List<String> listFormattedStringToWidth(String str, int wrapWidth) {
        return Arrays.asList(this.wrapFormattedStringToWidth(str, wrapWidth).split("\n"));
    }

    String wrapFormattedStringToWidth(String str, int wrapWidth) {
        int i = this.sizeStringToWidth(str, wrapWidth);

        if (str.length() <= i) {
            return str;
        } else {
            String s = str.substring(0, i);
            char c0 = str.charAt(i);
            boolean flag = c0 == ' ' || c0 == '\n';
            String s1 = getFormatFromString(s) + str.substring(i + (flag ? 1 : 0));
            return s + "\n" + this.wrapFormattedStringToWidth(s1, wrapWidth);
        }
    }

    private int sizeStringToWidth(String str, int wrapWidth) {
        return this.fontRendererImpl.sizeStringToWidthFloatHook(str, wrapWidth);
    }

    private static boolean isFormatColor(char colorChar) {
        return colorChar >= '0' && colorChar <= '9' || colorChar >= 'a' && colorChar <= 'f' || colorChar >= 'A' && colorChar <= 'F';
    }

    private static boolean isFormatSpecial(char formatChar) {
        return formatChar >= 'k' && formatChar <= 'o' || formatChar >= 'K' && formatChar <= 'O' || formatChar == 'r' || formatChar == 'R';
    }

    public static String getFormatFromString(String text) {
        StringBuilder s = new StringBuilder();
        int i = -1;
        int j = text.length();

        while ((i = text.indexOf(167, i + 1)) != -1) {
            if (i < j - 1) {
                char c0 = text.charAt(i + 1);

                if (isFormatColor(c0)) {
                    s = new StringBuilder("\u00a7" + c0);
                } else if (isFormatSpecial(c0)) {
                    s.append("\u00a7").append(c0);
                }
            }
        }

        return s.toString();
    }

    private void bindTexture(ResourceLocation p_bindTexture_1_) {
        this.fontRendererImpl.bindTextureEnterHook();
        this.renderEngine.bindTexture(p_bindTexture_1_);
    }

    public FontRendererImpl getFontRendererImpl() {
        return fontRendererImpl;
    }
}