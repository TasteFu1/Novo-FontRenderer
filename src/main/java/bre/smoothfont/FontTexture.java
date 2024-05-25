// =============================================== //
// Recompile disabled. Please run Recaf with a JDK //
// =============================================== //

// Decompiled with: CFR 0.152
// Class Version: 8
package bre.smoothfont;

import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.data.TextureMetadataSection;
import net.minecraft.util.ResourceLocation;

import org.apache.commons.io.IOUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import bre.smoothfont.config.FontConfig;
import bre.smoothfont.renderer.FontRendererImpl;
import bre.smoothfont.util.GLUtils;
import bre.smoothfont.util.Logger;

public class FontTexture extends AbstractTexture {
    public int fontRes;
    public float actualFontRes;
    public float chImageSizePx;
    public float borderWidthPx;
    public int blankSpacePx;
    public int texSizePx;
    public int texFilterSetting;
    public boolean anisotropicFilterSetting;
    public int textureWidth;
    public int textureSize;
    public boolean needReload;
    public float scaleFactor;
    protected final ResourceLocation textureLocation;
    protected final int page;

    public FontTexture(ResourceLocation resourceLoc) {
        this.textureLocation = resourceLoc;
        if ("smoothfont".equals(resourceLoc.getResourceDomain())) {
            if (resourceLoc.getResourcePath().equals("osFontDefaultPage".toLowerCase())) {
                this.page = 256;
            } else if (resourceLoc.getResourcePath().startsWith("osFontUnicodePage_".toLowerCase())) {
                String hex = "0x" + resourceLoc.getResourcePath().substring("osFontUnicodePage_".length());
                this.page = Integer.decode(hex);
            } else {
                this.page = -1;
            }
        } else {
            this.page = -1;
        }
        this.fontRes = 0;
        this.chImageSizePx = 0.0f;
        this.borderWidthPx = 0.0f;
        this.blankSpacePx = 0;
        this.texSizePx = 0;
        this.texFilterSetting = 0;
        this.anisotropicFilterSetting = false;
        this.needReload = false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    @Override
    public void loadTexture(IResourceManager iResMgr) throws IOException {
        this.texFilterSetting = 0;
        this.anisotropicFilterSetting = false;
        this.deleteGlTexture();
        IResource iresource = null;
        try {
            BufferedImage borderedImage;
            boolean flagBlur = false;
            boolean flagClamp = false;
            if ("smoothfont".equals(this.textureLocation.getResourceDomain())) {
                GlyphImage glyphCache = FontRasterizer.getInstance().getGlyphImage(this.page);
                assert glyphCache != null : "glyphImage-" + this.page + " is null";
                borderedImage = glyphCache.getGlyphImage();
                if (FontRendererImpl.extShaderWorking && borderedImage.getType() == 10) {
                    borderedImage = FontUtils.convertDataBufferGrayToABGR(borderedImage);
                }
                this.fontRes = FontTextureManager.getInstance().getUnicodeFontRes(true);
                this.borderWidthPx = glyphCache.borderSize;
                this.chImageSizePx = glyphCache.chImageBoxSize;
            } else {
                iresource = iResMgr.getResource(this.textureLocation);
                BufferedImage bufferedimage = TextureUtil.readBufferedImage(iresource.getInputStream());
                borderedImage = this.addTextureBorder(bufferedimage);
                if (iresource.hasMetadata()) {
                    try {
                        TextureMetadataSection texturemetadatasection = iresource.getMetadata("texture");
                        if (texturemetadatasection != null) {
                            flagBlur = texturemetadatasection.getTextureBlur();
                            flagClamp = texturemetadatasection.getTextureClamp();
                        }
                    } catch (RuntimeException runtimeexception) {
                        Logger.warn("Failed reading metadata of: " + this.textureLocation);
                    }
                }
            }

            this.scaleFactor = 1.0f;
            this.actualFontRes = this.fontRes;

            FontConfig config = FontConfig.getInstance();

            if (this.fontRes <= config.blurReduction) {
                this.scaleFactor = 2.0f;
                borderedImage = FontUtils.resizeImage(borderedImage, this.scaleFactor, false);
                this.actualFontRes = (float) this.fontRes * this.scaleFactor;
            }

            this.blankSpacePx = 0;

            if (!config.allowNPOTTexture && !FontUtils.isPOT(borderedImage.getWidth())) {
                int width = borderedImage.getWidth();
                int potWidth = FontUtils.nearPOT(width);
                this.blankSpacePx = potWidth - width;
                borderedImage = FontUtils.expandFrame(borderedImage, potWidth, potWidth);
            }

            this.texSizePx = borderedImage.getWidth();

            if (this.scaleFactor != 1.0f) {
                this.chImageSizePx *= this.scaleFactor;
                this.borderWidthPx *= this.scaleFactor;
            }

            this.textureWidth = borderedImage.getWidth();
            this.textureSize = FontUtils.getImageSize(borderedImage);

            if (config.enableMipmap) {
                this.textureSize += this.textureSize / 3;
            }

            this.uploadTextureImageAllocate(this.getGlTextureId(), borderedImage, flagBlur, flagClamp);
            this.needReload = false;

            if (iresource == null) {
                return;
            }

        } catch (Throwable throwable) {
            if (iresource == null) {
                throw throwable;
            }

            IOUtils.closeQuietly(iresource);
            throw throwable;
        }

        IOUtils.closeQuietly(iresource);
    }

    private void uploadTextureImageAllocate(int textureId, BufferedImage texture, boolean blur, boolean clamp) {
        FontConfig config = FontConfig.getInstance();
        int mipmapLevel = config.enableMipmap ? config.mipmapLevel : 0;

        if (texture.getType() == 10) {
            GLUtils.allocateTexture(textureId, mipmapLevel, texture.getWidth(), texture.getHeight(), true);
        } else {
            TextureUtil.allocateTextureImpl(textureId, mipmapLevel, texture.getWidth(), texture.getHeight());
        }

        if (config.enableMipmap && !GLUtils.glGenerateMipmapSupported) {
            GL11.glTexParameteri(3553, 33085, config.mipmapLevel);
            GL11.glTexParameteri(3553, 33169, 1);
        }

        if (texture.getType() == 10) {
            GLUtils.uploadTextureImage(textureId, texture, 0, 0, blur, clamp);
        } else {
            TextureUtil.uploadTextureImageSub(textureId, texture, 0, 0, blur, clamp);
        }

        if (config.enableMipmap && GLUtils.glGenerateMipmapSupported) {
            GL30.glGenerateMipmap(3553);
        }
    }

    protected BufferedImage addTextureBorder(BufferedImage origImg) {
        int width = origImg.getWidth();
        int height = origImg.getHeight();
        this.fontRes = width / 16;
        this.borderWidthPx = FontUtils.getBorderWidth(this.fontRes);
        int borderWidthInt = (int) this.borderWidthPx;
        int glyphGap = borderWidthInt * 2;
        this.chImageSizePx = this.fontRes;
        int textureSize = (this.fontRes + borderWidthInt * 2) * 16;
        BufferedImage newImg = new BufferedImage(textureSize, textureSize, 6);
        Graphics2D g2 = newImg.createGraphics();
        FontUtils.clearGraphics2D(g2, 0, 0, newImg.getWidth(), newImg.getHeight());
        if (origImg.getType() == 12) {
            for (int x = 0; x < width; ++x) {
                for (int y = 0; y < height; ++y) {
                    int alpha = origImg.getRGB(x, y) >>> 24;
                    if (alpha != 255) {
                        continue;
                    }
                    newImg.setRGB(x + x / this.fontRes * glyphGap + borderWidthInt, y + y / this.fontRes * glyphGap + borderWidthInt, -1);
                }
            }
        } else {
            for (int i = 0; i < 16; ++i) {
                for (int j = 0; j < 16; ++j) {
                    BufferedImage charImage = origImg.getSubimage(i * this.fontRes, j * this.fontRes, this.fontRes, this.fontRes);
                    g2.drawImage(charImage, i * (this.fontRes + glyphGap) + borderWidthInt, j * (this.fontRes + glyphGap) + borderWidthInt, null);
                }
            }
        }
        g2.dispose();
        return newImg;
    }

    public int page() {
        return this.page;
    }
}
 