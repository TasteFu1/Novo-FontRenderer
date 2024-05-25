// =============================================== //
// Recompile disabled. Please run Recaf with a JDK //
// =============================================== //

// Decompiled with: CFR 0.152
// Class Version: 8
package bre.smoothfont.util;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureUtil;

import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Hashtable;

public class GLUtils {
    public static boolean anisotropicFilterSupported = false;
    public static boolean glGenerateMipmapSupported = false;
    public static boolean shaderSupported = false;

    private static void setTextureBlur(boolean blur, boolean mipmap) {
        if (blur) {
            GlStateManager.glTexParameteri(3553, 10241, mipmap ? 9987 : 9729);
            GlStateManager.glTexParameteri(3553, 10240, 9729);
        } else {
            GlStateManager.glTexParameteri(3553, 10241, mipmap ? 9986 : 9728);
            GlStateManager.glTexParameteri(3553, 10240, 9728);
        }
    }

    private static void setTextureClamp(boolean clamp) {
        if (clamp) {
            GlStateManager.glTexParameteri(3553, 10242, 10496);
            GlStateManager.glTexParameteri(3553, 10243, 10496);
        } else {
            GlStateManager.glTexParameteri(3553, 10242, 10497);
            GlStateManager.glTexParameteri(3553, 10243, 10497);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void allocateTexture(int glTextureId, int mipmapLevels, int width, int height, boolean grayscale) {
        TextureUtil.deleteTexture(glTextureId);
        GlStateManager.bindTexture(glTextureId);
        // ** MonitorExit[var5_5] (shouldn't be in output)
        if (mipmapLevels >= 0) {
            GlStateManager.glTexParameteri(3553, 33085, mipmapLevels);
            GlStateManager.glTexParameteri(3553, 33082, 0);
            GlStateManager.glTexParameteri(3553, 33083, mipmapLevels);
            GlStateManager.glTexParameterf(3553, 34049, 0.0f);
        }

        for (int i = 0; i <= mipmapLevels; ++i) {
            if (grayscale) {
                GL11.glTexImage2D(3553, i, 6406, width >> i, height >> i, 0, 6406, 5121, (ByteBuffer) null);
                continue;
            }
            GL11.glTexImage2D(3553, i, 32993, width >> i, height >> i, 0, 32993, 5121, (ByteBuffer) null);
        }
    }

    public static int uploadTextureImage(int textureId, BufferedImage image, int xOffset, int yOffset, boolean blur, boolean clamp) {
        GlStateManager.bindTexture(textureId);
        GLUtils.uploadTextureImage(image, xOffset, yOffset, blur, clamp);
        return textureId;
    }

    private static void uploadTextureImage(BufferedImage image, int xOffset, int yOffset, boolean blur, boolean clamp) {
        int width = image.getWidth();
        int height = image.getHeight();
        int bands = image.getType() == 10 ? 1 : 4;
        WritableRaster raster = Raster.createInterleavedRaster(0, width, height, bands, null);
        ColorModel colorModel = image.getColorModel();
        BufferedImage texImage = new BufferedImage(colorModel, raster, false, new Hashtable<>());
        DataBufferByte imageBuffer = (DataBufferByte) texImage.getRaster().getDataBuffer();
        ByteBuffer byteBuf = ByteBuffer.allocateDirect(imageBuffer.getSize());
        int[] bytes = new int[bands];
        byteBuf.order(ByteOrder.nativeOrder());
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                image.getRaster().getPixel(x, y, bytes);
                if (image.getType() == 10) {
                    byteBuf.put((byte) bytes[0]);
                    continue;
                }
                byteBuf.put((byte) bytes[2]);
                byteBuf.put((byte) bytes[1]);
                byteBuf.put((byte) bytes[0]);
                byteBuf.put((byte) bytes[3]);
            }
        }
        byteBuf.flip();
        GLUtils.setTextureBlur(blur, false);
        GLUtils.setTextureClamp(clamp);
        if (image.getType() == 10) {
            GL11.glTexSubImage2D(3553, 0, xOffset, yOffset, width, height, 6406, 5121, byteBuf);
        } else {
            GL11.glTexSubImage2D(3553, 0, xOffset, yOffset, width, height, 32993, 5121, byteBuf);
        }
    }

    private static boolean checkAnisotropicFilterSupported() throws RuntimeException {
        ContextCapabilities capabilities = GLContext.getCapabilities();
        return capabilities.GL_EXT_texture_filter_anisotropic;
    }

    private static boolean checkShaderSupported() throws RuntimeException {
        ContextCapabilities capabilities = GLContext.getCapabilities();
        return capabilities.OpenGL20;
    }

    private static boolean checkGlGenerateMipmapSupported() throws RuntimeException {
        ContextCapabilities capabilities = GLContext.getCapabilities();
        return capabilities.OpenGL30;
    }

    public static void checkGlFuncSupported() {
        try {
            anisotropicFilterSupported = GLUtils.checkAnisotropicFilterSupported();
            if (!anisotropicFilterSupported) {
                Logger.info("GL_EXT_texture_filter_anisotropic not supported.");
            }
            if (!(glGenerateMipmapSupported = GLUtils.checkGlGenerateMipmapSupported())) {
                Logger.info("GL30.glGenerateMipmap not supported.");
            }
            if (!(shaderSupported = GLUtils.checkShaderSupported())) {
                Logger.info("GL20 shader not supported.");
            }
        } catch (RuntimeException re) {
            re.printStackTrace();
        }
    }
}
 