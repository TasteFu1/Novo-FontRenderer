// =============================================== //
// Recompile disabled. Please run Recaf with a JDK //
// =============================================== //

// Decompiled with: CFR 0.152
// Class Version: 8
package bre.smoothfont;

import com.google.common.collect.Maps;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Map;

import bre.smoothfont.config.FontConfig;
import bre.smoothfont.renderer.TTFFontRenderer;
import bre.smoothfont.util.GLUtils;
import bre.smoothfont.util.Logger;

public class FontTextureManager implements IResourceManagerReloadListener {
    private final Minecraft mc = Minecraft.getMinecraft();
    private static final FontTextureManager INSTANCE = new FontTextureManager();
    private final Map<ResourceLocation, ITextureObject> mapTextureObjects = Maps.newHashMap();
    private final IResourceManager theResourceManager = mc.getResourceManager();

    private FontTextureManager() {
        ((IReloadableResourceManager) mc.getResourceManager()).registerReloadListener(this);
    }

    public static FontTextureManager getInstance() {
        return INSTANCE;
    }

    public FontTexture bindTexture(ResourceLocation resource) {
        ITextureObject itextureobject = this.mapTextureObjects.get(resource);
        if (itextureobject == null) {
            itextureobject = new FontTexture(resource);
            this.loadTexture(resource, itextureobject);
        } else if (((FontTexture) itextureobject).needReload) {
            this.loadTexture(resource, itextureobject);
        }
        GlStateManager.bindTexture(itextureobject.getGlTextureId());
        return (FontTexture) itextureobject;
    }

    public boolean loadTexture(ResourceLocation textureLocation, ITextureObject textureObj) {
        boolean flag = true;
        try {
            textureObj.loadTexture(this.theResourceManager);
        } catch (Exception e) {
            Logger.error("Failed to load texture: " + textureLocation);
            e.printStackTrace();
            this.mapTextureObjects.put(textureLocation, textureObj);
            flag = false;
        }
        this.mapTextureObjects.put(textureLocation, textureObj);
        return flag;
    }

    public void onResourceManagerReload(IResourceManager iresourceManager) {
        this.clearMapTextureObjects();
        TTFFontRenderer ttfFontRenderer = mc.ttfFontRenderer;

        if (ttfFontRenderer != null) {
            ttfFontRenderer.getFontRendererImpl().reloadResources();
        }
    }

    public void clearMapTextureObjects() {
        ArrayList<ITextureObject> texObjList = new ArrayList<ITextureObject>(this.mapTextureObjects.values());
        try {
            for (ITextureObject texObj : texObjList) {
                ((AbstractTexture) texObj).deleteGlTexture();
            }
        } catch (RuntimeException re) {
            Logger.warn("Cannot delete Textures in the current thread.");
            re.printStackTrace();
            for (ITextureObject texObj : texObjList) {
                ((FontTexture) texObj).needReload = true;
            }
            return;
        }
        this.mapTextureObjects.clear();
    }

    public int getUnicodeFontRes(boolean osFont) {
        return osFont ? FontUtils.getFontRes(FontConfig.getInstance().fontResIndex) : 16;
    }

    public int getDefaultFontRes(ResourceLocation resourceLoc, boolean osFont) {
        if (osFont) {
            return FontUtils.getFontRes(FontConfig.getInstance().fontResIndex);
        }
        int texSize = FontTextureManager.getInstance().getFontRes(resourceLoc);
        return texSize == 0 ? 8 : texSize;
    }

    public int getFontRes(ResourceLocation resLoc) {
        FontTexture texObj = (FontTexture) this.mapTextureObjects.get(resLoc);
        return texObj == null ? 0 : texObj.fontRes;
    }

    private int getTexFilterSetting(ResourceLocation resLoc) {
        FontTexture texObj = (FontTexture) this.mapTextureObjects.get(resLoc);
        return texObj == null ? 0 : texObj.texFilterSetting;
    }

    private void setTexFilterSetting(ResourceLocation resLoc, int id) {
        FontTexture texObj = (FontTexture) this.mapTextureObjects.get(resLoc);
        if (texObj != null) {
            texObj.texFilterSetting = id;
        } else {
            Logger.error("setTexFilterSetting: textureObject is null.");
        }
    }

    private boolean getAnisotropicFilterSetting(ResourceLocation resLoc) {
        FontTexture texObj = (FontTexture) this.mapTextureObjects.get(resLoc);
        return texObj != null && texObj.anisotropicFilterSetting;
    }

    private void setAnisotropicFilterSetting(ResourceLocation resLoc, boolean flag) {
        FontTexture texObj = (FontTexture) this.mapTextureObjects.get(resLoc);
        if (texObj != null) {
            texObj.anisotropicFilterSetting = flag;
        } else {
            Logger.error("setTexFilterSetting: textureObject is null.");
        }
    }

    public void setTexParams(ResourceLocation resLoc, int filterId) {
        if (this.getTexFilterSetting(resLoc) != filterId) {
            this.setTexFilterSetting(resLoc, filterId);

            FontConfig config = FontConfig.getInstance();

            if (config.allowNPOTTexture) {
                GlStateManager.glTexParameteri(3553, 10242, 33071);
                GlStateManager.glTexParameteri(3553, 10243, 33071);
            }
            if (config.enableInterpolation) {
                if (config.enableMipmap) {
                    GlStateManager.glTexParameteri(3553, 10241, 9987);
                } else {
                    GlStateManager.glTexParameteri(3553, 10241, 9729);
                }
                GlStateManager.glTexParameteri(3553, 10240, 9729);
            } else {
                if (config.enableMipmap) {
                    GlStateManager.glTexParameteri(3553, 10241, 9986);
                } else {
                    GlStateManager.glTexParameteri(3553, 10241, 9728);
                }
                GlStateManager.glTexParameteri(3553, 10240, 9728);
            }
        }
    }

    public void setTexParamsNearest(ResourceLocation resLoc) {
        if (this.getTexFilterSetting(resLoc) != 0) {
            this.setTexFilterSetting(resLoc, 0);
            if (FontConfig.getInstance().allowNPOTTexture) {
                GlStateManager.glTexParameteri(3553, 10242, 33071);
                GlStateManager.glTexParameteri(3553, 10243, 33071);
            }
            GlStateManager.glTexParameteri(3553, 10241, 9728);
            GlStateManager.glTexParameteri(3553, 10240, 9728);
        }
    }

    public void setAnisotropicFilter(ResourceLocation resLoc, boolean enable) {
        if (GLUtils.anisotropicFilterSupported) {
            if (enable) {
                if (!this.getAnisotropicFilterSetting(resLoc)) {
                    float maxAnisotropic = GL11.glGetFloat(34047);
                    GlStateManager.glTexParameterf(3553, 34046, maxAnisotropic);
                    this.setAnisotropicFilterSetting(resLoc, true);
                }
            } else if (this.getAnisotropicFilterSetting(resLoc)) {
                GlStateManager.glTexParameterf(3553, 34046, 1.0f);
                this.setAnisotropicFilterSetting(resLoc, false);
            }
        }
    }
}
 