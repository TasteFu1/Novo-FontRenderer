// =============================================== //
// Recompile disabled. Please run Recaf with a JDK //
// =============================================== //

// Decompiled with: CFR 0.152
// Class Version: 8
package bre.smoothfont;

import net.minecraft.client.renderer.OpenGlHelper;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import bre.smoothfont.config.FontConfig;
import bre.smoothfont.renderer.FontRendererImpl;
import bre.smoothfont.util.GLUtils;
import bre.smoothfont.util.Logger;
import bre.smoothfont.util.ShaderProgram;

public class FontShader {
    private static final FontShader INSTANCE = new FontShader();
    private int shaderProg;
    private int shaderProgWithLight;
    private boolean lightmap;
    ShaderProgram shader1;
    ShaderProgram shader2;
    private int prevShaderProg;
    private boolean shaderEnabled = false;
    private boolean shaderParamDefaultStatus = false;
    private boolean shaderParamUnicodeStatus = false;
    private float rgbDefault;
    private float rgbUnicode;
    private float alphaDefault;
    private float alphaUnicode;
    private boolean shaderAvailable = false;
    private boolean setupDone = false;
    private int uniformLocTex;
    private int uniformLocTexWithLight1;
    private int uniformLocTexWithLight2;
    private int uniformLocColorWithLight;
    private int uniformLocColor;

    public static FontShader getInstance() {
        return INSTANCE;
    }

    public boolean setupShader() {
        if (this.setupDone) {
            return true;
        }

        this.setupDone = true;
        this.shaderAvailable = GLUtils.shaderSupported;

        if (!this.shaderAvailable) {
            return true;
        }

        String vShaderSrc = "#version 110\n" + //
                            "void main(void) {\n" + //
                            "  gl_Position = ftransform();\n" + //
                            "  gl_TexCoord[0] = gl_TextureMatrix[0] * gl_MultiTexCoord0;\n" + //
                            "  gl_FrontColor = gl_Color;\n" + //
                            "}";
        String fShaderSrc = "#version 110\n" + //
                            "uniform sampler2D texture;\n" + //
                            "uniform vec4 colorBias;\n" + //
                            "void main(void) {\n" + //
                            "  vec4 color = texture2DProj(texture, gl_TexCoord[0]);\n" + //
                            "  color.r = clamp(color.r + colorBias.r , 0.0, 1.0);\n" + //
                            "  color.g = clamp(color.g + colorBias.g , 0.0, 1.0);\n" + //
                            "  color.b = clamp(color.b + colorBias.b , 0.0, 1.0);\n" + //
                            "  color.a = clamp(color.a * colorBias.a , 0.0, 1.0);\n" + //
                            "  gl_FragColor = color * gl_Color;\n" + //
                            "}";
        String vShaderSrc2 = "#version 110\n" + //
                             "void main(void) {\n" + //
                             "  gl_Position = ftransform();\n" + //
                             "  gl_TexCoord[0] = gl_TextureMatrix[0] * gl_MultiTexCoord0;\n" + //
                             "  gl_TexCoord[1] = gl_TextureMatrix[1] * gl_MultiTexCoord1;\n" + //
                             "  gl_FrontColor = gl_Color;\n" + //
                             "}";
        String fShaderSrc2 = "#version 110\n" + //
                             "uniform sampler2D texture;\n" + //
                             "uniform sampler2D texture2;\n" + //
                             "uniform vec4 colorBias;\n" + //
                             "void main(void) {\n" + //
                             "  vec4 color = texture2DProj(texture, gl_TexCoord[0]);\n" + //
                             "  vec4 color2 = texture2DProj(texture2, gl_TexCoord[1]);\n" + //
                             "  color.r = clamp(color.r + colorBias.r , 0.0, 1.0);\n" + //
                             "  color.g = clamp(color.g + colorBias.g , 0.0, 1.0);\n" + //
                             "  color.b = clamp(color.b + colorBias.b , 0.0, 1.0);\n" + //
                             "  color.a = clamp(color.a * colorBias.a , 0.0, 1.0);\n" + //
                             "  gl_FragColor = color * color2 * gl_Color;\n" + //
                             "}";

        this.shader1 = new ShaderProgram(vShaderSrc, fShaderSrc);
        this.shader2 = new ShaderProgram(vShaderSrc2, fShaderSrc2);
        this.shaderProg = this.shader1.getProgram();
        this.shaderProgWithLight = this.shader2.getProgram();
        if (this.shaderProg == 0 || this.shaderProgWithLight == 0) {
            Logger.error("Could not create shader. Disabled shader.");
            this.shaderAvailable = false;
            this.shader1.disposeShader();
            this.shader2.disposeShader();
            this.shaderProg = 0;
            this.shaderProgWithLight = 0;
            return true;
        }
        this.uniformLocTex = GL20.glGetUniformLocation(this.shaderProg, "texture");
        this.uniformLocTexWithLight1 = GL20.glGetUniformLocation(this.shaderProgWithLight, "texture");
        this.uniformLocTexWithLight2 = GL20.glGetUniformLocation(this.shaderProgWithLight, "texture2");
        this.uniformLocColor = GL20.glGetUniformLocation(this.shaderProg, "colorBias");
        this.uniformLocColorWithLight = GL20.glGetUniformLocation(this.shaderProgWithLight, "colorBias");
        return true;
    }

    private void setColorBias(boolean lightmap, float r, float g, float b, float a) {
        if (lightmap) {
            GL20.glUniform4f(this.uniformLocColorWithLight, r, g, b, a);
        } else {
            GL20.glUniform4f(this.uniformLocColor, r, g, b, a);
        }
    }

    private void useProgram(boolean lightmap) {
        if (lightmap) {
            this.useProgram(this.shaderProgWithLight);
            GL20.glUniform1i(this.uniformLocTexWithLight1, OpenGlHelper.defaultTexUnit - 33984);
            GL20.glUniform1i(this.uniformLocTexWithLight2, OpenGlHelper.lightmapTexUnit - 33984);
        } else {
            this.useProgram(this.shaderProg);
            GL20.glUniform1i(this.uniformLocTex, OpenGlHelper.defaultTexUnit - 33984);
        }
    }

    private void useProgram(int prog) {
        if (!this.shaderAvailable) {
            return;
        }
        while (GL11.glGetError() != 0) {
        }
        GL20.glUseProgram(prog);
        int errno = GL11.glGetError();
        if (errno != 0) {
            Logger.error("glUseProgram Error: " + errno);
            Logger.error("Disabled shader.");
            this.shaderAvailable = false;
        }
    }

    private int getCurProgram() {
        if (!this.shaderAvailable) {
            return 0;
        }
        return GL11.glGetInteger(35725);
    }

    private boolean checkLightmapTexUnit() {
        GL13.glActiveTexture(OpenGlHelper.lightmapTexUnit);
        boolean ret = GL11.glGetBoolean(3553);
        GL13.glActiveTexture(OpenGlHelper.defaultTexUnit);
        return ret;
    }

    public void setShaderParams(FontRendererImpl frh, boolean unicodeFlag) {
        if (this.shaderEnabled) {
            if (unicodeFlag) {
                if (!this.shaderParamUnicodeStatus) {
                    this.setColorBias(this.lightmap & !frh.orthographic, this.rgbUnicode, this.rgbUnicode, this.rgbUnicode, this.alphaUnicode);
                    this.shaderParamDefaultStatus = false;
                    this.shaderParamUnicodeStatus = true;
                }
            } else if (!this.shaderParamDefaultStatus) {
                this.setColorBias(this.lightmap & !frh.orthographic, this.rgbDefault, this.rgbDefault, this.rgbDefault, this.alphaDefault);
                this.shaderParamDefaultStatus = true;
                this.shaderParamUnicodeStatus = false;
            }
        }
    }

    public void restoreShaderTemporarily() {
        if (this.shaderEnabled) {
            this.useProgram(this.prevShaderProg);
        }
    }

    public void resetShader(FontRendererImpl frh) {
        if (this.shaderEnabled) {
            this.useProgram(this.lightmap & !frh.orthographic);
        }
    }

    public void prepareShader(FontRendererImpl frh) {
        if (!this.shaderAvailable) {
            return;
        }

        this.prevShaderProg = this.getCurProgram();
        if (this.prevShaderProg == 0) {
            FontConfig config = FontConfig.getInstance();

            if (config.brightness != 0) {
                float brightnessFactorUnicode;
                float brightnessFactorDefault;
                float boundaryScaleFactorUnicode;
                float boundaryScaleFactorDefault;

                int brightnessUnicode;
                int brightnessDefault;

                this.shaderEnabled = true;
                this.rgbDefault = 0.0f;
                this.rgbUnicode = 0.0f;

                if (config.autoBrightness && !frh.changeFont) {
                    brightnessDefault = frh.autoBrightnessDefault;
                    brightnessUnicode = frh.autoBrightnessUnicode;
                    boundaryScaleFactorDefault = frh.brightnessBoundaryScaleFactorDefault;
                    boundaryScaleFactorUnicode = frh.brightnessBoundaryScaleFactorUnicode;
                } else {
                    brightnessDefault = config.brightness;
                    brightnessUnicode = config.brightness;
                    boundaryScaleFactorUnicode = boundaryScaleFactorDefault = FontRasterizer.getInstance().brightnessBoundaryScaleFactor;
                }

                if (frh.roundedFontScale < 1.5f) {
                    brightnessFactorDefault = 7.0f;
                    brightnessFactorUnicode = 7.0f;
                } else {
                    brightnessFactorDefault = frh.roundedFontScale < boundaryScaleFactorDefault ? 20.0f - 13.0f / (boundaryScaleFactorDefault - 1.5f) * (boundaryScaleFactorDefault - frh.roundedFontScale) : 20.0f;
                    brightnessFactorUnicode = frh.roundedFontScale < boundaryScaleFactorUnicode ? 20.0f - 13.0f / (boundaryScaleFactorUnicode - 1.5f) * (boundaryScaleFactorUnicode - frh.roundedFontScale) : 20.0f;
                }

                this.alphaDefault = 1.0f + (float) brightnessDefault / brightnessFactorDefault;
                this.alphaUnicode = 1.0f + (float) brightnessUnicode / brightnessFactorUnicode;

                if (config.enablePremultipliedAlpha) {
                    this.rgbDefault = (this.alphaDefault - 1.0f) / 2.0f;
                    this.rgbUnicode = (this.alphaUnicode - 1.0f) / 2.0f;
                }
            }

        } else {
            if (FontRendererImpl.extShaderWorking) {
                return;
            }

            Logger.info("Other shader is detected. Use ABGR texture.");
            FontTextureManager.getInstance().clearMapTextureObjects();
            FontRendererImpl.extShaderWorking = true;
        }
    }

    public void useShader(FontRendererImpl frh) {
        if (this.shaderEnabled) {
            this.lightmap = this.checkLightmapTexUnit();
            this.useProgram(this.lightmap & !frh.orthographic);
            this.setColorBias(this.lightmap & !frh.orthographic, 0.0f, 0.0f, 0.0f, 1.0f);
            this.shaderParamDefaultStatus = false;
            this.shaderParamUnicodeStatus = false;
        }
    }

    public void restoreShader() {
        if (this.shaderEnabled) {
            this.useProgram(this.prevShaderProg);
            this.shaderEnabled = false;
        }
    }
}
 