package cn.blockforge.generated.origoldsar.client;

import cn.blockforge.generated.origoldsar.GeneratedMod;
import cn.blockforge.generated.origoldsar.purse.OtherworldPurseItem;
import cn.blockforge.generated.origoldsar.purse.PurseData;
import cn.blockforge.generated.origoldsar.purse.PurseNetwork;
import cn.blockforge.generated.origoldsar.purse.PurseSlot;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * 物品栏界面上的异界行囊：玩家模型右边的图标（左键展开/收起，右键拿到鼠标上），
 * 和展开后界面上方那一行 9 格。装了 Traveler's Backpack 时图标往上挪一格，避开它的背包图标。
 */
@EventBusSubscriber(modid = GeneratedMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class PurseInventoryOverlay {
    private static final int ICON_X = 77;
    private static final int ICON_Y = 44;
    private static final int ICON_Y_WITH_TRAVELERS_BACKPACK = 26;
    /** 创造模式物品栏（背包页）：跟副手格以玩家模型为中线对称。 */
    private static final int CREATIVE_ICON_X = 127;
    private static final int CREATIVE_ICON_Y = 20;
    /** 展开的那一行接在物品栏界面顶上：从这里一直到界面原来的顶边。 */
    private static final int PANEL_TOP = PurseSlot.ROW_Y - 8;
    private static final int PANEL_HEIGHT = -PANEL_TOP;
    private static final int PANEL_WIDTH = 176;
    /** 直接借原版物品栏贴图拼，换材质包时也跟物品栏一个样子。 */
    private static final net.minecraft.resources.ResourceLocation INVENTORY_TEXTURE =
            net.minecraft.resources.ResourceLocation.withDefaultNamespace("textures/gui/container/inventory.png");

    private static Boolean travelersBackpack;

    private PurseInventoryOverlay() {
    }

    /** 客户端初始化：告诉行囊格子什么时候该显示。 */
    public static void init() {
        PurseSlot.clientScreenShowsPanel = () -> Minecraft.getInstance().screen instanceof InventoryScreen;
    }

    public static void onSync(PurseNetwork.SyncPayload payload) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.getData(GeneratedMod.PURSE_DATA).setClientPurse(payload.purse(), payload.open());
        }
    }

    private static int iconY() {
        if (travelersBackpack == null) {
            travelersBackpack = ModList.get().isLoaded("travelersbackpack");
        }
        return travelersBackpack ? ICON_Y_WITH_TRAVELERS_BACKPACK : ICON_Y;
    }

    private static PurseData data() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? null : player.getData(GeneratedMod.PURSE_DATA);
    }

    /** 这个界面上行囊图标的位置（相对界面左上角）；不显示图标的界面返回 null。 */
    private static int[] iconPos(Screen screen) {
        if (screen instanceof InventoryScreen) {
            return new int[] {ICON_X, iconY()};
        }
        if (screen instanceof CreativeModeInventoryScreen creative && creative.isInventoryOpen()) {
            return new int[] {CREATIVE_ICON_X, CREATIVE_ICON_Y};
        }
        return null;
    }

    private static boolean overIcon(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        int[] pos = iconPos(screen);
        if (pos == null) {
            return false;
        }
        double x = mouseX - screen.getGuiLeft() - pos[0];
        double y = mouseY - screen.getGuiTop() - pos[1];
        return x >= -1 && x < 17 && y >= -1 && y < 17;
    }

    private static boolean overPanel(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        double x = mouseX - screen.getGuiLeft();
        double y = mouseY - screen.getGuiTop();
        return x >= 0 && x < PANEL_WIDTH && y >= PANEL_TOP && y < PANEL_TOP + PANEL_HEIGHT;
    }

    @SubscribeEvent
    public static void onRenderBackground(ContainerScreenEvent.Render.Background event) {
        if (!(event.getContainerScreen() instanceof AbstractContainerScreen<?> screen) || iconPos(screen) == null) {
            return;
        }
        PurseData data = data();
        if (data == null || !data.equipped()) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        if (data.panelOpen() && screen instanceof InventoryScreen) {
            drawPanel(graphics, left, top);
        }
        // 图标底下不画格子（跟 Traveler's Backpack 一样），它不是真的格子，放不进别的东西。
        int[] pos = iconPos(screen);
        int iconX = left + pos[0];
        int iconY = top + pos[1];
        graphics.renderItem(data.purse(), iconX, iconY);
    }

    @SubscribeEvent
    public static void onRenderForeground(ContainerScreenEvent.Render.Foreground event) {
        if (!(event.getContainerScreen() instanceof AbstractContainerScreen<?> screen) || iconPos(screen) == null) {
            return;
        }
        PurseData data = data();
        if (data == null || !data.equipped() || !overIcon(screen, event.getMouseX(), event.getMouseY())) {
            return;
        }
        // 这里的坐标已经平移到界面左上角。
        int[] pos = iconPos(screen);
        event.getGuiGraphics().fillGradient(pos[0], pos[1], pos[0] + 16, pos[1] + 16, 0x80FFFFFF, 0x80FFFFFF);
    }

    @SubscribeEvent
    public static void onRenderPost(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen) || iconPos(screen) == null) {
            return;
        }
        PurseData data = data();
        Minecraft minecraft = Minecraft.getInstance();
        if (data == null || !data.equipped() || minecraft.player == null
                || !minecraft.player.containerMenu.getCarried().isEmpty()
                || !overIcon(screen, event.getMouseX(), event.getMouseY())) {
            return;
        }
        List<Component> lines = screen instanceof InventoryScreen
                ? List.of(
                        data.purse().getHoverName(),
                        Component.translatable(data.panelOpen()
                                        ? "gui.origolds_ar.purse.close" : "gui.origolds_ar.purse.open")
                                .withStyle(ChatFormatting.GRAY),
                        Component.translatable("gui.origolds_ar.purse.take").withStyle(ChatFormatting.GRAY))
                : List.of(
                        data.purse().getHoverName(),
                        Component.translatable("gui.origolds_ar.purse.take_creative").withStyle(ChatFormatting.GRAY));
        event.getGuiGraphics().renderComponentTooltip(minecraft.font, lines, event.getMouseX(), event.getMouseY());
    }

    @SubscribeEvent
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen) || iconPos(screen) == null) {
            return;
        }
        PurseData data = data();
        LocalPlayer player = Minecraft.getInstance().player;
        if (data == null || player == null) {
            return;
        }
        ItemStack carried = player.containerMenu.getCarried();
        if (overIcon(screen, event.getMouseX(), event.getMouseY())) {
            if (!(screen instanceof InventoryScreen)) {
                // 创造模式物品栏：没有展开的那一行；鼠标上的物品归客户端管，所以右键直接把行囊放回物品栏。
                if (data.equipped()) {
                    if (event.getButton() == InputConstants.MOUSE_BUTTON_RIGHT && carried.isEmpty()) {
                        send(PurseNetwork.ACTION_TAKE_TO_INVENTORY);
                    }
                    event.setCanceled(true);
                }
                return;
            }
            if (event.getButton() == InputConstants.MOUSE_BUTTON_LEFT && OtherworldPurseItem.isPurse(carried)) {
                send(PurseNetwork.ACTION_EQUIP_CARRIED);
                event.setCanceled(true);
            } else if (data.equipped() && event.getButton() == InputConstants.MOUSE_BUTTON_LEFT) {
                boolean open = !data.panelOpen();
                data.setPanelOpen(open);
                send(open ? PurseNetwork.ACTION_OPEN : PurseNetwork.ACTION_CLOSE);
                event.setCanceled(true);
            } else if (data.equipped() && event.getButton() == InputConstants.MOUSE_BUTTON_RIGHT) {
                if (carried.isEmpty()) {
                    data.setPanelOpen(false);
                    send(PurseNetwork.ACTION_TAKE);
                }
                event.setCanceled(true);
            }
            return;
        }
        // 展开的那一行在界面外面：点到格子之间的空隙时别当成“点到界面外”把手上的东西扔出去。
        if (data.panelOpen() && screen instanceof InventoryScreen && screen.getSlotUnderMouse() == null
                && overPanel(screen, event.getMouseX(), event.getMouseY())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen) || iconPos(screen) == null) {
            return;
        }
        PurseData data = data();
        if (data != null && data.panelOpen() && screen instanceof InventoryScreen && screen.getSlotUnderMouse() == null
                && overPanel(screen, event.getMouseX(), event.getMouseY())) {
            event.setCanceled(true);
        }
    }

    private static void send(int action) {
        PacketDistributor.sendToServer(new PurseNetwork.ActionPayload(action));
    }

    /**
     * 用原版物品栏贴图拼出接在界面顶上的一行：贴图最上面 7 行（顶边框）、快捷栏那一行格子、
     * 再用纯灰底（带左右边框）的一行一直铺到界面原来顶边的下面，把原来的顶边盖掉，两块连成一个整体。
     */
    private static void drawPanel(GuiGraphics graphics, int left, int top) {
        int y = top + PANEL_TOP;
        strip(graphics, left, y, 0, 7);
        y += 7;
        strip(graphics, left, y, 141, 18);
        y += 18;
        for (; y < top + 4; y++) {
            strip(graphics, left, y, 80, 1);
        }
    }

    /** 把物品栏贴图第 v 行起、整宽、height 行高的一条画到 (x, y)。 */
    private static void strip(GuiGraphics graphics, int x, int y, int v, int height) {
        graphics.blit(INVENTORY_TEXTURE, x, y, 0, v, PANEL_WIDTH, height);
    }
}
