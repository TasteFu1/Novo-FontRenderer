package net.minecraft.client.renderer.tileentity;

import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.client.gui.font.FontRenderer;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.client.model.ModelSign;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

public class TileEntitySignRenderer extends TileEntitySpecialRenderer<TileEntitySign>
{
    private static final ResourceLocation SIGN_TEXTURE = new ResourceLocation("textures/entity/sign.png");

    /** The ModelSign instance for use in this renderer */
    private final ModelSign model = new ModelSign();

    public void func_192841_a(TileEntitySign p_192841_1_, double p_192841_2_, double p_192841_4_, double p_192841_6_, float p_192841_8_, int p_192841_9_, float p_192841_10_)
    {
        Block block = p_192841_1_.getBlockType();
        GlStateManager.pushMatrix();
        float f = 0.6666667F;

        if (block == Blocks.STANDING_SIGN)
        {
            GlStateManager.translate((float)p_192841_2_ + 0.5F, (float)p_192841_4_ + 0.5F, (float)p_192841_6_ + 0.5F);
            float f1 = (float)(p_192841_1_.getBlockMetadata() * 360) / 16.0F;
            GlStateManager.rotate(-f1, 0.0F, 1.0F, 0.0F);
            this.model.signStick.showModel = true;
        }
        else
        {
            int k = p_192841_1_.getBlockMetadata();
            float f2 = 0.0F;

            if (k == 2)
            {
                f2 = 180.0F;
            }

            if (k == 4)
            {
                f2 = 90.0F;
            }

            if (k == 5)
            {
                f2 = -90.0F;
            }

            GlStateManager.translate((float)p_192841_2_ + 0.5F, (float)p_192841_4_ + 0.5F, (float)p_192841_6_ + 0.5F);
            GlStateManager.rotate(-f2, 0.0F, 1.0F, 0.0F);
            GlStateManager.translate(0.0F, -0.3125F, -0.4375F);
            this.model.signStick.showModel = false;
        }

        if (p_192841_9_ >= 0)
        {
            this.bindTexture(DESTROY_STAGES[p_192841_9_]);
            GlStateManager.matrixMode(5890);
            GlStateManager.pushMatrix();
            GlStateManager.scale(4.0F, 2.0F, 1.0F);
            GlStateManager.translate(0.0625F, 0.0625F, 0.0625F);
            GlStateManager.matrixMode(5888);
        }
        else
        {
            this.bindTexture(SIGN_TEXTURE);
        }

        GlStateManager.enableRescaleNormal();
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.6666667F, -0.6666667F, -0.6666667F);
        this.model.renderSign();
        GlStateManager.popMatrix();
        FontRenderer fontrenderer = this.getFontRenderer();
        float f3 = 0.010416667F;
        GlStateManager.translate(0.0F, 0.33333334F, 0.046666667F);
        GlStateManager.scale(0.010416667F, -0.010416667F, 0.010416667F);
        GlStateManager.glNormal3f(0.0F, 0.0F, -0.010416667F);
        GlStateManager.depthMask(false);
        int i = 0;

        if (p_192841_9_ < 0)
        {
            for (int j = 0; j < p_192841_1_.signText.length; ++j)
            {
                if (p_192841_1_.signText[j] != null)
                {
                    ITextComponent itextcomponent = p_192841_1_.signText[j];
                    List<ITextComponent> list = GuiUtilRenderComponents.splitText(itextcomponent, 90, fontrenderer, false, true);
                    String s = list != null && !list.isEmpty() ? ((ITextComponent)list.get(0)).getFormattedText() : "";

                    if (j == p_192841_1_.lineBeingEdited)
                    {
                        s = "> " + s + " <";
                        fontrenderer.drawString(s, -fontrenderer.getStringWidth(s) / 2, j * 10 - p_192841_1_.signText.length * 5, 0);
                    }
                    else
                    {
                        fontrenderer.drawString(s, -fontrenderer.getStringWidth(s) / 2, j * 10 - p_192841_1_.signText.length * 5, 0);
                    }
                }
            }
        }

        GlStateManager.depthMask(true);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();

        if (p_192841_9_ >= 0)
        {
            GlStateManager.matrixMode(5890);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(5888);
        }
    }
}
