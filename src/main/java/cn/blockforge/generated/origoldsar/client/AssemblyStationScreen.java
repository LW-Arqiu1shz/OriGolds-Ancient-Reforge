package cn.blockforge.generated.origoldsar.client;

import cn.blockforge.generated.origoldsar.AssemblyStationMenu;
import cn.blockforge.generated.origoldsar.GeneratedMod;
import cn.blockforge.generated.origoldsar.TraitLibrary;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class AssemblyStationScreen extends AbstractContainerScreen<AssemblyStationMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
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
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        titleLabelX = 8;
        titleLabelY = 8;
        inventoryLabelX = INV_X0;
        inventoryLabelY = INV_LABEL_Y;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        int localX = mouseX - x;
        int localY = mouseY - y;
        boolean input = menu.hasInput();

        graphics.blit(TEXTURE, x, y, 0f, 0f, WIDTH, HEIGHT, 256, 256);

        graphics.drawCenteredString(font, title, x + WIDTH / 2, y + 15, GOLD);
        graphics.drawString(font, INPUT_LABEL, x + 16, y + 44, LIGHT, false);

        if (input) {
            graphics.renderOutline(x + INPUT_X - 2, y + INPUT_Y - 2, 20, 20, GOLD);
        }

        graphics.drawString(font, OPTIONS_LABEL, x + CARD_LEFT, y + 42, GOLD_SOFT, false);

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
            graphics.drawCenteredString(font, TraitLibrary.optionComponent(menu.getCandidate(index)),
                    x + (CARD_LEFT + CARD_RIGHT) / 2, y + rowTop + 6, textColor);
        }

        graphics.drawString(font, playerInventoryTitle, x + INV_X0, y + INV_LABEL_Y, LIGHT, false);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // All labels are drawn inside renderBg so they stay behind the slot contents.
    }

    private static int cardRowTop(int index) {
        return CARD_TOP + index * (CARD_HEIGHT + CARD_GAP);
    }

    private static boolean isInsideCard(int localX, int localY, int rowTop) {
        return localX >= CARD_LEFT && localX < CARD_RIGHT
                && localY >= rowTop && localY < rowTop + CARD_HEIGHT;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && menu.hasInput()) {
            int localX = (int) mouseX - leftPos;
            int localY = (int) mouseY - topPos;
            for (int index = 0; index < AssemblyStationMenu.TRAIT_BUTTONS; index++) {
                if (isInsideCard(localX, localY, cardRowTop(index))) {
                    if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, index);
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        int localX = mouseX - leftPos;
        int localY = mouseY - topPos;
        for (int index = 0; index < AssemblyStationMenu.TRAIT_BUTTONS; index++) {
            if (menu.hasInput() && isInsideCard(localX, localY, cardRowTop(index))) {
                graphics.renderTooltip(font, TraitLibrary.optionComponent(menu.getCandidate(index)), mouseX, mouseY);
                return;
            }
        }
    }
}
