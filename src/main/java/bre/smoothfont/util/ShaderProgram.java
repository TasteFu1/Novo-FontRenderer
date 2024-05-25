// =============================================== //
// Recompile disabled. Please run Recaf with a JDK //
// =============================================== //

// Decompiled with: CFR 0.152
// Class Version: 8
package bre.smoothfont.util;

import org.lwjgl.opengl.GL20;

public class ShaderProgram {
    private int program = this.createProgram();
    private int vertShader;
    private int fragShader;
    private String lerr;

    public ShaderProgram(String vertShaderSrc, String fragShaderSrc) {
        if (this.program == 0) {
            return;
        }

        this.vertShader = this.compileShader(35633, vertShaderSrc);
        this.fragShader = this.compileShader(35632, fragShaderSrc);

        if (this.vertShader != 0 && this.fragShader != 0) {
            if (this.linkProgram(this.program, this.fragShader)) {
                return;
            }

            this.detachShaders();
        }

        this.dispose();
    }

    public int getProgram() {
        return this.program;
    }

    private int createProgram() {
        int program = GL20.glCreateProgram();
        if (program == 0) {
            Logger.error("Shader program creation failed.");
            return 0;
        }
        return program;
    }

    private int compileShader(int type, String shaderSource) {
        int shader = GL20.glCreateShader(type);
        if (shader == 0) {
            Logger.error("Shader creation failed. Type=" + type);
            return 0;
        }
        GL20.glShaderSource(shader, shaderSource);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, 35713) == 0) {
            int len = GL20.glGetShaderi(shader, 35716);
            String err = GL20.glGetShaderInfoLog(shader, len);
            Logger.error("Shader compilation failed. (Type=" + type + "): " + err);
            if (type == 35633) {
            } else if (type == 35632) {
            }
            GL20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    private boolean linkProgram(int program, int fragShader) {
        GL20.glAttachShader(program, fragShader);
        GL20.glLinkProgram(program);
        if (GL20.glGetProgrami(program, 35714) == 0) {
            int len = GL20.glGetProgrami(program, 35716);
            this.lerr = GL20.glGetProgramInfoLog(program, len);
            Logger.error("Linking shader program failed: " + this.lerr);
            return false;
        }
        return true;
    }

    private void detachShaders() {
        if (this.vertShader != 0) {
            GL20.glDetachShader(this.program, this.vertShader);
        }
        if (this.fragShader != 0) {
            GL20.glDetachShader(this.program, this.fragShader);
        }
    }

    private void dispose() {
        if (this.vertShader != 0) {
            GL20.glDeleteShader(this.vertShader);
            this.vertShader = 0;
        }

        if (this.fragShader != 0) {
            GL20.glDeleteShader(this.fragShader);
            this.fragShader = 0;
        }

        if (this.program != 0) {
            GL20.glDeleteProgram(this.program);
            this.program = 0;
        }
    }

    public void disposeShader() {
        this.detachShaders();
        this.dispose();
    }
}
 