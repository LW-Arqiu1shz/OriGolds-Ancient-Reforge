package cn.blockforge.generated.origoldsar.client;

import cn.blockforge.generated.origoldsar.purse.OtherworldPurseMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** 异界行囊的 3×3 界面（借用原版发射器的背景），界面正上方一个“装备”按钮。 */
public final class OtherworldPurseScreen extends AbstractContainerScreen<OtherworldPurseMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/dispenser.png");

    public OtherworldPurseScreen(OtherworldPurseMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
        if (menu.isPlaced()) {
            // 从地上的行囊打开的：没有装备按钮，要装备就先挖起来。
            return;
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.origolds_ar.purse.equip"), button -> {
                    if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, OtherworldPurseMenu.BUTTON_EQUIP);
                    }
                })
                .bounds(leftPos + (imageWidth - 46) / 2, topPos - 27, 46, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.origolds_ar.purse.equip_hint")))
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }
}
