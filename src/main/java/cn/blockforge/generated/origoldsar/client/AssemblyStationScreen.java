package cn.blockforge.generated.origoldsar.client;

import cn.blockforge.generated.origoldsar.AssemblyStationMenu;
import cn.blockforge.generated.origoldsar.GeneratedMod;
import cn.blockforge.generated.origoldsar.TraitLibrary;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public final class AssemblyStationScreen extends AbstractContainerScreen<AssemblyStationMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            GeneratedMod.MOD_ID, "textures/gui/assembly_station.png");

    private static final int WIDTH = 200;
    private static final int HEIGHT = 206;
    private static final int INPUT_X = 46;
    private static final int INPUT_Y = 56;
    private static final int CARD_LEFT = 108;
    private static final int CARD_RIGHT = 190;
    private static final int CARD_TOP = 54;
    private static final int CARD_HEIGHT = 20;
    private static final int CARD_GAP = 3;
    private static final int INV_X0 = 19;
    private static final int INV_LABEL_Y = 121;

    private static final int GOLD = 0xFFE0B855;
    private static final int GOLD_BRIGHT = 0xFFFFE9A8;
    private static final int GOLD_SOFT = 0xFFC9A227;
    private static final int LIGHT = 0xFFD9D9E0;
    private static final int DISABLED = 0xFF6E6E7A;
    private static final int HOVER_OVERLAY = 0x40E0B855;

    private static final Component INPUT_LABEL = Component.translatable("gui.origolds_ar.station_input_label");
    private static final Component OPTIONS_LABEL = Component.translatable("gui.origolds_ar.station_options_label");

    public AssemblyStationScreen(AssemblyStationMenu menu, Inventory inventory, Component title) {
        // 26.1.2：AbstractContainerScreen 的 imageWidth/imageHeight 是 final，
        // 尺寸改为通过 4/5 参数的父类构造器传入，不能再在构造体里赋值。
        super(menu, inventory, title, WIDTH, HEIGHT);
        titleLabelX = 8;
        titleLabelY = 8;
        inventoryLabelX = INV_X0;
        inventoryLabelY = INV_LABEL_Y;
    }

    /**
     * 26.1.2：{@code renderBg}/{@code renderLabels}/{@code render} 已不存在，
     * 背景改在 {@code extractBackground} 里用 {@link GuiGraphicsExtractor} 绘制。
     * 自定义卡片与文字画在这里，一样位于槽位内容之下。
     */
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos;
        int y = topPos;
        int localX = mouseX - x;
        int localY = mouseY - y;
        boolean input = menu.hasInput();

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0f, 0f, WIDTH, HEIGHT, 256, 256);

        graphics.centeredText(font, title, x + WIDTH / 2, y + 15, GOLD);
        graphics.text(font, INPUT_LABEL, x + 16, y + 44, LIGHT, false);

        if (input) {
            graphics.outline(x + INPUT_X - 2, y + INPUT_Y - 2, 20, 20, GOLD);
        }

        graphics.text(font, OPTIONS_LABEL, x + CARD_LEFT, y + 42, GOLD_SOFT, false);

        for (int index = 0; index < AssemblyStationMenu.TRAIT_BUTTONS; index++) {
            int rowTop = cardRowTop(index);
            boolean hovered = isInsideCard(localX, localY, rowTop);
            int textColor;
            if (!input) {
                textColor = DISABLED;
            } else if (hovered) {
                textColor = GOLD_BRIGHT;
            } else {
                textColor = GOLD;
            }
            if (input && hovered) {
                graphics.fill(x + CARD_LEFT, y + rowTop, x + CARD_RIGHT, y + rowTop + CARD_HEIGHT, HOVER_OVERLAY);
            }
            graphics.centeredText(font, TraitLibrary.optionComponent(menu.getCandidate(index)),
                    x + (CARD_LEFT + CARD_RIGHT) / 2, y + rowTop + 6, textColor);
        }

        graphics.text(font, playerInventoryTitle, x + INV_X0, y + INV_LABEL_Y, LIGHT, false);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // All labels are drawn inside extractBackground so they stay behind the slot contents.
    }

    private static int cardRowTop(int index) {
        return CARD_TOP + index * (CARD_HEIGHT + CARD_GAP);
    }

    private static boolean isInsideCard(int localX, int localY, int rowTop) {
        return localX >= CARD_LEFT && localX < CARD_RIGHT
                && localY >= rowTop && localY < rowTop + CARD_HEIGHT;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && menu.hasInput()) {
            int localX = (int) event.x() - leftPos;
            int localY = (int) event.y() - topPos;
            for (int index = 0; index < AssemblyStationMenu.TRAIT_BUTTONS; index++) {
                if (isInsideCard(localX, localY, cardRowTop(index))) {
                    if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, index);
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    /** 原版槽位提示由父类绘制；这里在卡片悬停时补上词条说明。 */
    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (menu.hasInput()) {
            int localX = mouseX - leftPos;
            int localY = mouseY - topPos;
            for (int index = 0; index < AssemblyStationMenu.TRAIT_BUTTONS; index++) {
                if (isInsideCard(localX, localY, cardRowTop(index))) {
                    graphics.setTooltipForNextFrame(font,
                            TraitLibrary.optionComponent(menu.getCandidate(index)), mouseX, mouseY);
                    return;
                }
            }
        }
        super.extractTooltip(graphics, mouseX, mouseY);
    }
}
