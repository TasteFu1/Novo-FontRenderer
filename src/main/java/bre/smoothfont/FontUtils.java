// =============================================== //
// Recompile disabled. Please run Recaf with a JDK //
// =============================================== //

// Decompiled with: CFR 0.152
// Class Version: 8
package bre.smoothfont;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

import org.apache.commons.io.IOUtils;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.imageio.ImageIO;

import bre.smoothfont.config.FontConfig;
import bre.smoothfont.util.Logger;
import bre.smoothfont.util.ModLib;

public class FontUtils {
    public static final String defaultCharList = "\u00C0\u00C1\u00C2\u00C8\u00CA\u00CB\u00CD\u00D3\u00D4\u00D5\u00DA\u00DF\u00E3\u00F5\u011F\u0130\u0131\u0152\u0153\u015E\u015F\u0174\u0175\u017E\u0207\u0000\u0000\u0000\u0000\u0000\u0000\u0000 !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u0000\u00C7\u00FC\u00E9\u00E2\u00E4\u00E0\u00E5\u00E7\u00EA\u00EB\u00E8\u00EF\u00EE\u00EC\u00C4\u00C5\u00C9\u00E6\u00C6\u00F4\u00F6\u00F2\u00FB\u00F9\u00FF\u00D6\u00DC\u00F8\u00A3\u00D8\u00D7\u0192\u00E1\u00ED\u00F3\u00FA\u00F1\u00D1\u00AA\u00BA\u00BF\u00AE\u00AC\u00BD\u00BC\u00A1\u00AB\u00BB\u2591\u2592\u2593\u2502\u2524\u2561\u2562\u2556\u2555\u2563\u2551\u2557\u255D\u255C\u255B\u2510\u2514\u2534\u252C\u251C\u2500\u253C\u255E\u255F\u255A\u2554\u2569\u2566\u2560\u2550\u256C\u2567\u2568\u2564\u2565\u2559\u2558\u2552\u2553\u256B\u256A\u2518\u250C\u2588\u2584\u258C\u2590\u2580\u03B1\u03B2\u0393\u03C0\u03A3\u03C3\u03BC\u03C4\u03A6\u0398\u03A9\u03B4\u221E\u2205\u2208\u2229\u2261\u00B1\u2265\u2264\u2320\u2321\u00F7\u2248\u00B0\u2219\u00B7\u221A\u207F\u00B2\u25A0\u0000";
    public static char[] asciiSheetChars = defaultCharList.toCharArray();
    private static final Map<Character, Integer> defaultGlyphIdMap = new HashMap<>();

    static {
        StringBuilder sb = new StringBuilder(defaultCharList);
        String chListReversed = sb.reverse().toString();

        int id = chListReversed.length() - 1;

        for (char ch : chListReversed.toCharArray()) {
            defaultGlyphIdMap.put(ch, id);
            --id;
        }
    }

    public static int getDefaultGlyphIndex(char character) {
        Integer id = defaultGlyphIdMap.get(character);
        return id == null ? -1 : id;
    }

    public static int getFontRes(int val) {
        return switch (val) {
            case 0 -> 8;
            case 1 -> 10;
            case 2 -> 12;
            case 4 -> 24;
            case 5 -> 32;
            case 6 -> 48;
            case 7 -> 64;
            default -> 16;
        };
    }

    public static int getBorderWidth(int fontRes) {
        int border = (int) ((float) fontRes / 16.0f);
        border += border % 2;
        border = Math.max(border, 2);
        return border;
    }

    public static int nearPOT(int value) {
        if (value < 0) {
            return 0;
        }
        int p = MathHelper.ceil(FontUtils.log2(value));
        return (int) Math.pow(2.0, p);
    }

    public static boolean isPOT(int value) {
        return (value & value - 1) == 0;
    }

    private static float log2(int value) {
        return (float) (Math.log(value) / Math.log(2.0));
    }

    public static BufferedImage resizeImage(BufferedImage origImg, float scaleFactor, boolean lerp) {
        Graphics2D g2;
        BufferedImage newImage;
        int width = origImg.getWidth();
        int height = origImg.getHeight();
        int newWidth = (int) ((float) width * scaleFactor);
        int newHeight = (int) ((float) height * scaleFactor);
        if (origImg.getType() == 10) {
            newImage = new BufferedImage(newWidth, newHeight, 10);
            g2 = newImage.createGraphics();
            FontUtils.clearGraphics2DGray(g2, 0, 0, newWidth, newHeight);
        } else {
            newImage = new BufferedImage(newWidth, newHeight, 6);
            g2 = newImage.createGraphics();
            FontUtils.clearGraphics2D(g2, 0, 0, newWidth, newHeight);
        }
        if (lerp) {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        } else {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        }
        g2.drawImage(origImg, 0, 0, newWidth, newHeight, 0, 0, width, height, null);
        g2.dispose();
        return newImage;
    }

    public static BufferedImage expandFrame(BufferedImage origImg, int newWidth, int newHeight) {
        Graphics2D g2;
        BufferedImage newImg;
        if (origImg.getWidth() >= newWidth || origImg.getHeight() >= newHeight) {
            return origImg;
        }
        if (origImg.getType() == 10) {
            newImg = new BufferedImage(newWidth, newHeight, 10);
            g2 = newImg.createGraphics();
            FontUtils.clearGraphics2DGray(g2, 0, 0, newImg.getWidth(), newImg.getHeight());
        } else {
            newImg = FontConfig.getInstance().enablePremultipliedAlpha ? new BufferedImage(newWidth, newHeight, 7) : new BufferedImage(newWidth, newHeight, 6);
            g2 = newImg.createGraphics();
            FontUtils.clearGraphics2D(g2, 0, 0, newImg.getWidth(), newImg.getHeight());
        }
        g2.drawImage(origImg, 0, 0, null);
        g2.dispose();
        return newImg;
    }

    public static BufferedImage convertToPremultipliedAlpha(BufferedImage origImg) {
        BufferedImage newImg = new BufferedImage(origImg.getWidth(), origImg.getHeight(), 7);
        for (int x = 0; x < origImg.getWidth(); ++x) {
            for (int y = 0; y < origImg.getHeight(); ++y) {
                int argb = origImg.getRGB(x, y);
                int alpha = argb >>> 24;
                int red = ((argb & 0xFF0000) >>> 16) * alpha / 255;
                int green = ((argb & 0xFF00) >>> 8) * alpha / 255;
                int blue = (argb & 0xFF) * alpha / 255;
                int premultifiedAlpha = (alpha << 24) + (red << 16) + (green << 8) + blue;
                newImg.setRGB(x, y, premultifiedAlpha);
            }
        }
        return newImg;
    }

    public static void clearGraphics2D(Graphics2D g2, int x1, int y1, int x2, int y2) {
        if (FontConfig.getInstance().enablePremultipliedAlpha) {
            g2.setBackground(new Color(0, 0, 0, 0));
        } else {
            g2.setBackground(new Color(255, 255, 255, 0));
        }
        g2.clearRect(x1, y1, x2, y2);
    }

    public static void clearGraphics2DGray(Graphics2D g2, int x1, int y1, int x2, int y2) {
        g2.setBackground(Color.BLACK);
        g2.clearRect(x1, y1, x2, y2);
    }

    public static int getTotalOpacityPosY(BufferedImage image, int posY) {
        int opacity = 0;
        int width = image.getWidth();
        for (int x = 0; x < width; ++x) {
            int alpha = image.getRGB(x, posY) >>> 24;
            opacity += alpha;
        }
        return opacity;
    }

    public static int getTotalOpacityPosYGray(BufferedImage image, int posY) {
        int opacity = 0;
        int width = image.getWidth();
        for (int x = 0; x < width; ++x) {
            int alpha = image.getRGB(x, posY) & 0xFF;
            opacity += alpha;
        }
        return opacity;
    }

    public static int getEstimatedBrightness(int fontRes, int opacity) {
        int normalizedOpacity = opacity / fontRes;
        normalizedOpacity = Math.max(1, normalizedOpacity);
        return 60 / normalizedOpacity;
    }

    public static float getEstimatedBrightnessBoundaryScaleFactor(int fontRes, int opacity) {
        return 255.0f / (float) opacity * (float) fontRes / 8.0f;
    }

    public static float getShadowAdjustVal(int shadowLength) {
        return (float) (shadowLength - 5) / 10.0f;
    }

    public static int getTexFilterSettingId() {
        FontConfig config = FontConfig.getInstance();
        return (config.enableInterpolation ? 2 : 0) + (config.enableMipmap ? 1 : 0);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static BufferedImage getMCFontImage(ResourceLocation resourceLoc) {
        IResourceManager iRegMgr = Minecraft.getMinecraft().getResourceManager();
        IResource iresource = null;
        BufferedImage bufImage = null;
        try {
            iresource = iRegMgr.getResource(resourceLoc);
            bufImage = TextureUtil.readBufferedImage(iresource.getInputStream());
        } catch (Exception e) {
            Logger.error("Failed to get font resource.");
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(iresource);
        }
        return bufImage;
    }

    public static boolean isFontAvailable(Font font) {
        String familyName = font.getFamily(Locale.ENGLISH);
        String settingName = font.getName();
        String[] settingNameSplitted = settingName.split("\\.");
        return !familyName.equals("Dialog") || settingNameSplitted[0].equals("Dialog");
    }

    @Deprecated
    public static BufferedImage convertGrayToABGR(BufferedImage origImg) {
        int width = origImg.getWidth();
        int height = origImg.getHeight();
        BufferedImage newImg = new BufferedImage(width, height, 6);
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                int argb = origImg.getRGB(x, y) << 24 | 0xFFFFFF;
                newImg.setRGB(x, y, argb);
            }
        }
        return newImg;
    }

    @Deprecated
    public static BufferedImage convertIntArrayGrayToABGR(BufferedImage origImg) {
        int width = origImg.getWidth();
        int height = origImg.getHeight();
        BufferedImage newImg = new BufferedImage(width, height, 6);
        int[] pixels = origImg.getRGB(0, 0, width, height, null, 0, width);
        for (int offset = 0; offset < height * width; offset += width) {
            for (int x = 0; x < width; ++x) {
                int i = offset + x;
                pixels[i] = pixels[i] << 24 | 0xFFFFFF;
            }
        }
        newImg.setRGB(0, 0, width, height, pixels, 0, width);
        return newImg;
    }

    public static BufferedImage convertDataBufferGrayToABGR(BufferedImage origImg) {
        int width = origImg.getWidth();
        int height = origImg.getHeight();
        BufferedImage newImg = new BufferedImage(width, height, 6);
        DataBuffer intBuf = origImg.getRaster().getDataBuffer();
        int[] pixels = new int[intBuf.getSize()];
        for (int i = 0; i < intBuf.getSize(); ++i) {
            pixels[i] = intBuf.getElem(i) << 24 | 0xFFFFFF;
        }
        int[] bandMasks = new int[]{0xFF0000, 65280, 255, -16777216};
        WritableRaster writableRaster = Raster.createPackedRaster(new DataBufferInt(pixels, width * height), width, height, width, bandMasks, null);
        newImg.setData(writableRaster);
        return newImg;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Deprecated
    public static byte[] getPngBytesFromImage(BufferedImage image) {
        byte[] bytes = null;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BufferedOutputStream bufout = new BufferedOutputStream(out);

        image.flush();

        try {
            ImageIO.write(image, "png", bufout);
            bufout.flush();
            bufout.close();
            bytes = out.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(out);
        }

        return bytes;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Deprecated
    public static BufferedImage getImageFromBytes(byte[] bytes) {
        BufferedImage image = null;
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        try {
            image = ImageIO.read(in);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(in);
        }
        return image;
    }

    public static byte[] getBytesFromImage(BufferedImage image) {
        return ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
    }

    public static BufferedImage getABGRImageFromABGRBytes(byte[] bytes) {
        int width = (int) MathHelper.sqrt((float) bytes.length / 4);
        BufferedImage image = new BufferedImage(width, width, 6);
        byte[] byteArray = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(bytes, 0, byteArray, 0, byteArray.length);
        return image;
    }

    public static BufferedImage getGrayImageFromGrayBytes(byte[] bytes) {
        int width = (int) MathHelper.sqrt(bytes.length);
        BufferedImage image = new BufferedImage(width, width, 10);
        byte[] byteArray = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(bytes, 0, byteArray, 0, byteArray.length);
        return image;
    }

    public static int getImageSize(BufferedImage image) {
        return image.getRaster().getDataBuffer().getSize();
    }

    @Deprecated
    public static byte[] getBytesFromGrayImage(BufferedImage image) {
        DataBuffer dataBuf = image.getRaster().getDataBuffer();
        byte[] result = new byte[dataBuf.getSize()];
        for (int i = 0; i < dataBuf.getSize(); ++i) {
            result[i] = (byte) dataBuf.getElem(i);
        }
        return result;
    }

    @Deprecated
    public static BufferedImage getABGRImageFromGrayBytes2(byte[] bytes) {
        int width = (int) MathHelper.sqrt(bytes.length);
        int[] pixels = new int[bytes.length];
        for (int i = 0; i < bytes.length; ++i) {
            pixels[i] = bytes[i] << 24 | 0xFFFFFF;
        }
        int[] bandMasks = new int[]{0xFF0000, 65280, 255, -16777216};
        WritableRaster writableRaster = Raster.createPackedRaster(new DataBufferInt(pixels, width * width), width, width, width, bandMasks, null);
        BufferedImage newImg = new BufferedImage(width, width, 6);
        newImg.setData(writableRaster);
        return newImg;
    }

    public static byte[] gzipBytes(byte[] bytes) {
        byte[] result;
        try {
            ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
            GZIPOutputStream gzipOut = new GZIPOutputStream(byteArrayOut);
            gzipOut.write(bytes);
            gzipOut.close();
            byteArrayOut.close();
            result = byteArrayOut.toByteArray();
        } catch (Exception e) {
            Logger.error("Error during compressing byte array.");
            e.printStackTrace();
            result = bytes;
        }
        return result;
    }

    public static byte[] gunzipBytes(byte[] gzipBytes) {
        byte[] result;
        try {
            int length;
            ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
            ByteArrayInputStream byteArrayIn = new ByteArrayInputStream(gzipBytes);
            GZIPInputStream gzipIn = new GZIPInputStream(byteArrayIn);
            byte[] buf = new byte[4096];
            while ((length = gzipIn.read(buf)) > 0) {
                byteArrayOut.write(buf, 0, length);
            }
            gzipIn.close();
            byteArrayIn.close();
            byteArrayOut.close();
            result = byteArrayOut.toByteArray();
        } catch (Exception e) {
            Logger.error("Error during uncompressing byte array.");
            e.printStackTrace();
            result = gzipBytes;
        }
        return result;
    }

    public static void setAntiAlias(Graphics2D g2, boolean antiAlias, boolean fractionalMetrics) {
        if (antiAlias) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (fractionalMetrics) {
                g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            } else {
                g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            }
        } else {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        }
    }

    public static int toNormalWidth(float width) {
        return ModLib.roundHalfEven(width);
    }
}
 