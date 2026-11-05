package cn.autoforged.module_shop.client.screen;

import cn.autoforged.module_shop.client.atom.AtomUi;
import com.atom.lib.client.render.BorrowedFramePainter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * AtomLib 屏幕的输入转发壳。
 * <p>
 * 按下、拖拽、松开、滚轮和 {@code removed} 都交给 {@link com.atom.lib.input.ScreenInputAdapter}。
 * 屏幕关掉时只取消手势，不关闭共享渲染桥。
 * <p>
 * 1.20.1：原版 {@code Screen#render} 不画背景，子类在 {@code render} 开头调用 {@link #renderBackground(GuiGraphics)}
 * 压暗世界，再画自己的 AtomLib 内容。滚轮只有竖直一个量。
 */
abstract class AtomScreen extends Screen {
    protected final AtomUi.Pointers pointers = new AtomUi.Pointers();

    protected AtomScreen(Component title) {
        super(title);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.pointers.input.press(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return this.pointers.input.drag(mouseX, mouseY, button, dragX, dragY)
                || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return this.pointers.input.release(mouseX, mouseY, button) || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // 1.20.1 只有竖直滚轮一个量；横向传 0。
        return this.pointers.input.scroll(mouseX, mouseY, 0.0D, delta)
                || super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void removed() {
        this.pointers.removed();
        super.removed();
    }

    /** 先结束原版批次，再进入 Skija 通道。原版绘制不要放进这个回调。 */
    protected final void paintAtom(GuiGraphics graphics, BorrowedFramePainter painter) {
        graphics.flush();
        AtomUi.draw(painter);
    }
}
