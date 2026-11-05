package cn.autoforged.module_shop.client.ui.render;

import com.atom.lib.client.render.MinecraftRenderBridge;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.humbleui.skija.Canvas;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.opengl.GL11;

/**
 * 一帧 Skija 绘制的编排（参考 AtomChat {@code SkiaGraphics} 的精简版，MIT）。
 *
 * <p>顺序：
 * <ol>
 *     <li>{@code graphics.flush()}：先把已排队的原版 GUI 绘制提交掉，保证层序；</li>
 *     <li>{@code bridge.vanillaPass(preUi)}：可选，需要在 Skija 之前做的原版 GL 工作（以后的物品图标准备放这里）；</li>
 *     <li>{@code bridge.drawFrame(...)}：Skija 绘制。画布统一缩放到 <b>Minecraft GUI 坐标</b>
 *     （1 单位 = 1 个 GUI 像素），所以鼠标坐标、原版物品/提示框与 Skija 内容共用同一套坐标，无需换算。</li>
 * </ol>
 * 之后调用方可以继续用 {@link GuiGraphics} 在 Skija 之上叠加原版内容（物品、tooltip、隐藏输入框）。
 */
public final class SkiaFrame {
    private SkiaFrame() {
    }

    public static void draw(GuiGraphics graphics, Consumer<Canvas> painter) {
        draw(graphics, null, painter);
    }

    public static void draw(GuiGraphics graphics, Runnable preUi, Consumer<Canvas> painter) {
        RenderSystem.assertOnRenderThread();
        graphics.flush();
        MinecraftRenderBridge bridge = MinecraftRenderBridge.get();
        if (preUi != null) {
            bridge.vanillaPass(preUi);
        }
        float scale = (float) Minecraft.getInstance().getWindow().getGuiScale();
        bridge.drawFrame((canvas, frame) -> {
            int base = canvas.getSaveCount();
            canvas.save();
            try {
                canvas.scale(scale, scale);
                painter.accept(canvas);
            } finally {
                // 无论绘制代码有没有配平 save/restore，都回到进入时的状态。
                canvas.restoreToCount(base);
            }
        });
        restoreVanillaState();
    }

    /**
     * Skija 画完后把原版渲染依赖的全局状态设回标准值（1.20.1 图标闪烁修复）。
     *
     * <p>Skija 之后原版还会叠画物品：页面物品层、容器界面的物品格（出货箱 / 放入物品 / 发放…）、次级弹窗里的物品。
     * AtomLib 会完整恢复 Skija 改动的 GL 状态，但同一帧里其它渲染（别的模组的界面事件等）可能留下
     * 非白的着色颜色 / 物品光照 / 深度缓冲内容，原版画物品会直接沿用，表现为图标偶尔一两帧不显示。
     * 这里统一复位着色颜色、混合、深度测试与写入、物品光照，并清深度缓冲
     * （之后画的东西都在已画内容之上，GUI 本来就是按绘制顺序叠的）。
     */
    public static void restoreVanillaState() {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        Lighting.setupFor3DItems();
    }
}
