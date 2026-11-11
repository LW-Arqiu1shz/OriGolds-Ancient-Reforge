package cn.blockforge.generated.origoldsar.client;

import cn.blockforge.generated.origoldsar.GeneratedMod;
import cn.blockforge.generated.origoldsar.PlayerInfoPayload;
import cn.blockforge.generated.origoldsar.RemoteBindPayload;
import cn.blockforge.generated.origoldsar.RemoteUnbindPayload;
import cn.blockforge.generated.origoldsar.WirelessDeletePayload;
import cn.blockforge.generated.origoldsar.WirelessLinkSyncPayload;
import cn.blockforge.generated.origoldsar.WirelessRedstoneData;
import cn.blockforge.generated.origoldsar.WirelessRedstoneManager;
import cn.blockforge.generated.origoldsar.WirelessReversePayload;
import cn.blockforge.generated.origoldsar.WirelessSelectPayload;
import cn.blockforge.generated.origoldsar.WirelessSelectSyncPayload;
import cn.blockforge.generated.origoldsar.WirelessUndoPayload;
import cn.blockforge.generated.origoldsar.api.visual.WireVisualCanvas;
import cn.blockforge.generated.origoldsar.api.visual.WireVisualColor;
import cn.blockforge.generated.origoldsar.api.visual.WireVisualContext;
import cn.blockforge.generated.origoldsar.api.visual.WireVisualLayer;
import cn.blockforge.generated.origoldsar.api.visual.WireVisualLink;
import cn.blockforge.generated.origoldsar.api.visual.WireVisualLinks;
import cn.blockforge.generated.origoldsar.api.visual.WireVisualMath;
import cn.blockforge.generated.origoldsar.api.visual.WireVisualSource;
import cn.blockforge.generated.origoldsar.api.visual.WireVisualStyle;
import cn.blockforge.generated.origoldsar.api.visual.WireVisuals;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 金源无线红石连接装置 / 遥控开关的客户端<b>输入与状态</b>：
 *
 * <ul>
 *   <li>潜行 + 左键：向服务端上报准星命中的方块（连接装置选起点、遥控开关绑定；
 *       连接装置的选点只在无线红石显示模式开启时生效）；
 *       若只选了一个点、再对同一方块按同一条边的键，服务端会取消该标记；</li>
 *   <li>潜行 + 中键：切换“无线红石显示模式”/“遥控显示模式”（本地状态，不占网络；
 *       开/关与所处模式由 {@link DisplayModeMemory} 持久化，重启游戏后原样恢复，
 *       附属模组加入的新模式用同一套 API 即自动适配）。<b>按住 Ctrl 或 Alt 时不响应</b>——
 *       那是附属模组连接装置自己两个模式的快捷键，本模组要让路，否则玩家一边潜行一边按
 *       附属快捷键时，本模式会被连带切换并误弹“无线红石显示模式：关”；</li>
 *   <li>显示模式：渲染“输入→输出”渐变线路（源 A 蓝、终点 B 橙），两端分别为蓝色（输入端口 A）
 *       与橙色（输出端口 B）的端点盒；准星悬停某条线路时，线体与两个端点<b>描白</b>
 *       （填充仍保留原色，玩家才分得清输入 / 输出）；</li>
 *   <li>显示模式下准星悬停某条线路时按 Delete：删除该线路；按 Backspace：反转该线路方向
 *       （交换输入端 / 输出端）。连电线 / 删电线 / 反转电线统一 32 格射程；</li>
 *   <li>已选了一个坐标点、还差第二个点时，在该点画一个呼吸灯式（2 秒周期明暗循环）
 *       的高亮框，提醒下一步该在哪补点；</li>
 *   <li>手持连接装置 / 遥控开关时，准星命中的方块用<b>白色四角括号</b>提示（统一 32 格射程）；</li>
 *   <li>遥控开关的“悬停选中”与线路悬停同源：<b>穿墙几何检测</b>——准星射线（32 格内）
 *       命中已绑定的方块即选中它（无视中间遮挡），选中的方块<b>直接变成白色</b>
 *       （半透明白色填充），此时按 <b>Delete</b> 解除绑定；</li>
 *   <li><b>Ctrl+Z / Ctrl+Y</b>：撤销 / 重做。手持任意连接装置且<b>无线红石显示模式开启</b>时，
 *       作用于该玩家的无线线路操作历史（新增/删除连线、反转方向）；模式关着（装置处于
 *       附属模组加入的其它模式）时不上报，既不撤回别的模式的操作，也不会弹"没有可撤销的操作"；
 *       手持某一遥控开关则只作用于这一把开关自己的绑定/解绑历史
 *       （每一把独立保存，最多 16 条，退出世界即清除）。</li>
 *   <li><b>三模式互斥</b>：见 {@link #connectorDisplay} —— 附属模组《OriGolds: Industrialized
 *       Age》给连接装置加入"金源能量电线 / 无线物流管道"两个新模式，任一模式开启时通过本类
 *       的公共静态 boolean 字段 {@code connectorDisplay} 关闭/查询本模组的无线红石显示模式，
 *       实现三模式互斥；本模组的选点、删除、反转、撤销、重做、悬停、渲染与呼吸提示框
 *       全部以该字段为门控，只有本模式开启时才对无线红石线路生效。
 *       切换提示也随之区分：因附属开启其它模式而被动关闭时<b>不弹任何提示</b>；只有玩家主动
 *       按快捷键关闭时才弹“无线红石显示模式：关”；从附属的其它模式切回本模式时弹
 *       “已切换至无线红石显示模式”（由 {@link AddonModeBridge} 只读探测）。</li>
 * </ul>
 *
 * <p><b>本类不再自己画任何东西。</b>26.1.2 的渲染改成"渲染状态 + 提交节点"模型后，
 * 能画线的 {@code RenderTypes.lines()} 输出到的是实体/物品那张离屏缓冲，线路因此在主画面里
 * 消失。所有线路、端点、外框与高亮都改由通用模块 {@code api/visual}（{@link WireVisuals} +
 * {@link WireVisualCanvas}）自建的管线绘制：本类只注册两路通道、并在通道回调里交出本帧要画的
 * 几何。附属模组要给自己的电线/管道加显示，同样只需
 * {@link WireVisuals#register} 一路通道即可，与本模组共用同一套管线与准星交互手感。
 */
@EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT)
public final class WirelessConnectorClient {
    /** 服务端同步下来的全部无线红石线路（模块视图，一次交给 {@link WireVisualLinks} 画完）。 */
    private static List<WireVisualLink> links = List.of();
    /** 当前准星悬停的线路 id（显示模式下有效），没有则为 null。 */
    private static UUID hoveredLink = null;
    /** 当前“待选中转点”：已选了一个坐标点、还差第二个点（由服务端同步），没有则为 null。 */
    private static SelectedPoint selectedPoint = null;
    /** 上一次 tick 反转键（Backspace）是否按下，用于上升沿检测（一次按键只反转一次）。 */
    private static boolean reverseKeyHeld = false;
    /** 上一次 tick Delete 是否按下，用于遥控开关解除绑定的上升沿检测（一次按键只解除一次）。 */
    private static boolean unbindKeyHeld = false;

    /**
     * 无线红石显示模式的<b>实时开关</b>（本模式开 / 关的唯一运行时真相）。
     *
     * <p><b>对外接口契约</b>：附属模组（如《OriGolds: Industrialized Age》的三模式互斥桥
     * {@code OrigoldsArBridge}）通过反射按名字在本类上查找这个<b>公共静态 boolean 字段</b>并
     * 直接读写——用户切到金源能量电线 / 无线物流管道模式时把本字段写为 {@code false} 以关闭
     * 本模式，也读取本字段判断本模式是否开启（开启时附属自动关闭它自己的两个模式）。
     * 因此<b>类名 {@code WirelessConnectorClient}、字段名 {@code connectorDisplay}、
     * {@code public static boolean} 类型三者都是契约的一部分，不得更名、改型或降权</b>。
     *
     * <p>本模组内部请一律经由 {@link DisplayModeMemory#setOn} / {@link DisplayModeMemory#toggle}
     * 修改（其内部会同步本字段）。附属绕过 API 直写本字段产生的不一致，会在客户端 tick 开头
     * 对账并写回 {@link DisplayModeMemory}（含落盘），保证退出游戏后记住最后生效的状态。
     */
    public static volatile boolean connectorDisplay = false;

    /** 本字段与持久化记忆是否已对过一次账（游戏启动后首个客户端 tick 完成初次恢复）。 */
    private static boolean connectorDisplayRestored = false;

    /** 一个“待选中转点”的客户端视图。 */
    private record SelectedPoint(ResourceKey<Level> dim, long pos, boolean start) {}

    // ---------------------------------------------------------------- 视觉样式（外观真相）

    /** 输入端口（线路源 A，被采样红石信号的那端）配色：蓝。 */
    private static final int INPUT_COLOR = 0xFF408CFF;
    /** 输出端口（线路终点 B，被无线供电的那端）配色：橙。 */
    private static final int OUTPUT_COLOR = 0xFFFF8C1A;
    /** 遥控开关绑定方块：淡蓝（0.5, 0.8, 1.0）。 */
    private static final int SWITCH_COLOR = 0xFF80CCFF;
    /** 悬停 / 选中提示：纯白。 */
    private static final int WHITE = 0xFFFFFFFF;

    // ---- 线宽：一律用「基准宽度的倍数」表达，不再写死格数 ------------------------------------
    // 基准宽度 = WireVisualStyle.baseWidth()（出厂默认 0.025 格）。以后加配置选项时只要把用户
    // 设置的值写进 WireVisuals.setBaseWidth(...)，下面所有线就一起等比变粗/变细，而且彼此的
    // 粗细关系保持不变。当前所有倍数统一取 1.0（线宽 == 基准宽度），历史写死值记在注释里备查。
    // 确实要脱离全局缩放单独钉死某一条线，才把 widthScale(...) 换成 width(格数)。
    // 注意：.glow(...) 是核心线之外额外绘制的一层加色带。Iris 会让这类透明/加色几何更贵，
    // 因此本轮把主线与辅助框的辉光宽度收窄；光晕仍保留，不影响线色、流光与穿墙显示。

    /** 无线连线主线路：1.0 × 基准（历史写死值 0.055 格 = 2.2 倍）。 */
    private static final float WIRE_LINE_WIDTH_SCALE = 1.0F;
    /** 待选中转点括号框：1.0 × 基准（历史 0.07 格 = 2.8 倍）。 */
    private static final float PENDING_WIDTH_SCALE = 1.0F;
    /** 准星四角括号：1.0 × 基准（历史 0.045 格 = 1.8 倍）。 */
    private static final float AIM_WIDTH_SCALE = 1.0F;
    /** 遥控开关绑定外框：1.0 × 基准（历史 0.05 格 = 2.0 倍）。 */
    private static final float SWITCH_BOX_WIDTH_SCALE = 1.0F;
    /** 遥控开关穿墙选中框：1.0 × 基准（历史 0.06 格 = 2.4 倍）。 */
    private static final float SWITCH_SELECTED_WIDTH_SCALE = 1.0F;

    /** 无线红石线路本体：输入→输出渐变 + 流光 + 收窄后的辉光，穿墙显示。 */
    private static final WireVisualStyle WIRE_LINE = WireVisualStyle.gradient(INPUT_COLOR, OUTPUT_COLOR)
            .widthScale(WIRE_LINE_WIDTH_SCALE).flow(1.6F).glow(1.2F)
            // 无线链路必须穿过实体方块显示；不要依赖 WireVisualStyle 的默认层级。
            .layer(WireVisualLayer.OVERLAY);
    /** 待选中转点的呼吸括号框（起点蓝、终点橙，提醒玩家另一端该点谁）。 */
    private static final WireVisualStyle PENDING_START =
            WireVisualStyle.of(INPUT_COLOR).widthScale(PENDING_WIDTH_SCALE).glow(0.9F);
    private static final WireVisualStyle PENDING_END =
            WireVisualStyle.of(OUTPUT_COLOR).widthScale(PENDING_WIDTH_SCALE).glow(0.9F);
    /** 准星命中的方块：白色四角括号，不发光——只是"你要点的是它"。 */
    private static final WireVisualStyle AIM_BRACKETS = WireVisualStyle.hairline(WHITE).widthScale(AIM_WIDTH_SCALE);
    /** 遥控开关绑定方块的外框（按开关是否激活调透明度）。 */
    private static final WireVisualStyle SWITCH_BOX =
            WireVisualStyle.of(SWITCH_COLOR).widthScale(SWITCH_BOX_WIDTH_SCALE).glow(0.8F);
    /** 遥控开关穿墙选中：整块变白（半透填充 + 白框）。 */
    private static final WireVisualStyle SWITCH_SELECTED =
            WireVisualStyle.hairline(WHITE).widthScale(SWITCH_SELECTED_WIDTH_SCALE);

    /** 本模组的两路视觉通道 id（附属模组用自己的命名空间另注册即可）。 */
    private static final Identifier CHANNEL_WIRELESS =
            Identifier.fromNamespaceAndPath(GeneratedMod.MOD_ID, "wireless_redstone");
    private static final Identifier CHANNEL_REMOTE_SWITCH =
            Identifier.fromNamespaceAndPath(GeneratedMod.MOD_ID, "remote_switch");

    private WirelessConnectorClient() {}

    /**
     * 注册本模组的无线视觉通道（由 {@code GeneratedMod} 在客户端分支调用一次）。
     *
     * <p>通道 id 固定，重复调用按替换处理——附属模组或重载脚本可以先注册同 id 的通道覆盖表现。
     */
    public static void registerVisualChannels() {
        WireVisuals.register(WireVisualSource.of(CHANNEL_WIRELESS, 1000,
                WirelessConnectorClient::wirelessRedstoneVisible,
                WirelessConnectorClient::collectWirelessRedstone));
        WireVisuals.register(WireVisualSource.of(CHANNEL_REMOTE_SWITCH, 1010,
                context -> holds(context.player(), GeneratedMod.ORIGOLD_REMOTE_SWITCH.get()),
                WirelessConnectorClient::collectRemoteSwitch));
    }

    /** 由服务端同步包处理器调用（见 {@code WirelessRedstoneNetwork}）。 */
    public static void onLinkSync(WirelessLinkSyncPayload payload) {
        List<WireVisualLink> views = new ArrayList<>(payload.links().size());
        for (WirelessLinkSyncPayload.LinkView link : payload.links()) {
            views.add(WireVisualLink.of(link.id(), link.dimA(), WirelessRedstoneData.fromLong(link.posA()),
                    link.dimB(), WirelessRedstoneData.fromLong(link.posB()), WIRE_LINE));
        }
        links = List.copyOf(views);
    }

    /** 由服务端同步包处理器调用：更新“待选中转点”。 */
    public static void onSelectSync(WirelessSelectSyncPayload payload) {
        selectedPoint = payload.present()
                ? new SelectedPoint(payload.dim(), payload.pos(), payload.start())
                : null;
    }

    // ---------------------------------------------------------------- 输入

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.getOverlay() != null) {
            return;
        }
        Player player = minecraft.player;
        if (player == null) {
            return;
        }
        boolean holdsConnector = holds(player, GeneratedMod.WIRELESS_CONNECTOR.get());
        boolean holdsSwitch = holds(player, GeneratedMod.ORIGOLD_REMOTE_SWITCH.get());
        if (!holdsConnector && !holdsSwitch) {
            return;
        }

        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT && isSneak(event, player)) {
            // 潜行 + 左键：把命中的方块上报服务端（连接装置记录起点 / 遥控开关绑定），并阻止挖掘。
            // 非潜行的普通左键不再拦截，恢复原版挖掘手感。
            // 注意：不能用缓存的 minecraft.hitResult——准星前站着一只生物时它是 EntityHitResult、
            // 而且每 tick 才更新一次，会导致"按了没反应"。这里现做一次只命中方块的射线。
            // 统一 32 格射程；且必须命中真实方块（空气不参与选点/绑定）。
            BlockHitResult hit = raycastBlock(minecraft, player, WirelessRedstoneManager.INTERACT_RANGE);
            if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                // 连接装置的选点属于无线红石模式：模式关闭（例如附属模组把装置切到了
                // 金源能量电线 / 无线物流管道模式）时不触发本模式的选点，让位给当前模式；
                // 此时若另一只手还拿着遥控开关，则退让为开关绑定（与服务端择栈优先级一致）。
                if (holdsConnector && connectorDisplay) {
                    ClientPacketDistributor.sendToServer(new WirelessSelectPayload(hit.getBlockPos().asLong(), true));
                    event.setCanceled(true);
                } else if (holdsSwitch) {
                    ClientPacketDistributor.sendToServer(new RemoteBindPayload(hit.getBlockPos().asLong()));
                    event.setCanceled(true);
                }
            }
        } else if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT && isSneak(event, player) && holdsConnector) {
            // 潜行 + 右键：连接装置记录终点方块（用 32 格射线，不受普通触及距离限制），并阻止
            // 把它当成一次普通右键使用方块（开箱、按按钮等）。终点选择不再走 useOn，统一走 32 格射线。
            if (!connectorDisplay) {
                return; // 同上：右键选点同样只在无线红石模式开启时生效。
            }
            BlockHitResult hit = raycastBlock(minecraft, player, WirelessRedstoneManager.INTERACT_RANGE);
            if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                ClientPacketDistributor.sendToServer(new WirelessSelectPayload(hit.getBlockPos().asLong(), false));
                event.setCanceled(true);
            }
        } else if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE
                && isSneak(event, player) && !isCtrlOrAlt(event)) {
            // 潜行 + 中键：切换显示模式。模式开/关与所处模式由 DisplayModeMemory 持久化，
            // 重启游戏后原样恢复；附属给装置加入的新模式走同一套 API 即自动被记住。
            // Ctrl / Alt 同时按下时本分支不响应：那是附属模组“金源能量电线 / 无线物流管道”
            // 两个模式的快捷键（Ctrl+中键 / Alt+中键）。玩家潜行着按附属快捷键时，若不排除
            // 这两个修饰键，本模组会跟着把自己的模式也切掉并误弹“无线红石显示模式：关”。
            if (holds(player.getMainHandItem(), GeneratedMod.WIRELESS_CONNECTOR.get())) {
                toggleConnectorDisplay();
            } else if (holds(player.getMainHandItem(), GeneratedMod.ORIGOLD_REMOTE_SWITCH.get())) {
                toggleSwitchDisplay();
            } else if (holdsConnector) {
                toggleConnectorDisplay();
            } else {
                toggleSwitchDisplay();
            }
            event.setCanceled(true);
        }
    }

    /**
     * 切换连接装置的无线红石显示模式（写入跨会话记忆）。
     *
     * <p>提示分三种情况：
     * <ul>
     *   <li>本次<b>关闭</b>：弹“无线红石显示模式：关”。这条只可能由玩家主动按快捷键触发——
     *       附属模组开启它的其它模式时会直接改写 {@link #connectorDisplay}，走不到这里，
     *       因此不会替附属的模式切换背锅；</li>
     *   <li>本次<b>打开</b>、且切换前附属的“金源能量电线 / 无线物流管道”模式正开着：
     *       弹“已切换至无线红石显示模式”，说明是模式之间的切换（附属会在自己的客户端 tick
     *       里关掉那两个模式，完成三模式互斥）；</li>
     *   <li>本次<b>打开</b>、其它模式都没开：弹“无线红石显示模式：开”。</li>
     * </ul>
     */
    private static void toggleConnectorDisplay() {
        // 先于本次切换探测附属模式状态：切换后附属的 tick 才会把它的模式关掉，
        // 那时再读就分不清“是不是从其它模式切回来的”了。
        boolean fromOtherMode = AddonModeBridge.isOtherModeActive();
        boolean on = DisplayModeMemory.toggle(DisplayModeMemory.MODE_WIRELESS);
        if (!on) {
            hoveredLink = null;
            requestInfo("message.origolds_ar.wc_display_off");
        } else if (fromOtherMode) {
            requestInfo("message.origolds_ar.wc_switched_to_display");
        } else {
            requestInfo("message.origolds_ar.wc_display_on");
        }
    }

    /** 切换遥控开关的遥控显示模式（写入跨会话记忆）。 */
    private static void toggleSwitchDisplay() {
        boolean on = DisplayModeMemory.toggle(DisplayModeMemory.MODE_REMOTE);
        requestInfo(on ? "message.origolds_ar.rs_display_on" : "message.origolds_ar.rs_display_off");
    }

    /**
     * 1.2.0 起显示模式切换的提示改由服务端展示：客户端把翻译键发过去，
     * 服务端经 {@code PlayerInfoBar} 优先投进基模玩家信息栏（不在则回退 action bar）。
     */
    private static void requestInfo(String translationKey) {
        ClientPacketDistributor.sendToServer(new PlayerInfoPayload(translationKey));
    }

    /** Shift 判定：优先用鼠标事件自带的 GLFW 修饰位（与按下同一时刻，绝对准确），
     *  再用本地玩家的实时按键状态兜底。 */
    private static boolean isSneak(InputEvent.MouseButton.Pre event, Player player) {
        return (event.getModifiers() & GLFW.GLFW_MOD_SHIFT) != 0 || player.isShiftKeyDown();
    }

    /**
     * Ctrl / Alt 是否按下：本模组的连接装置快捷键是 Shift+中键，而附属模组《OriGolds:
     * Industrialized Age》用它自己的一套 Ctrl+中键（金源能量电线）、Alt+中键（无线物流管道）。
     * 玩家潜行时 Shift 一直是按下的，若中键分支不排除 Ctrl / Alt，两套快捷键会同时命中，
     * 本模式被连带切换并误弹“关”。判定方式与附属一致：先看鼠标事件的修饰位，
     * 再用实时按键状态兜底。
     */
    private static boolean isCtrlOrAlt(InputEvent.MouseButton.Pre event) {
        if ((event.getModifiers() & (GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_ALT)) != 0) {
            return true;
        }
        // 26.1.2：InputConstants.isKeyDown 直接接收 Window。
        Window window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_ALT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_ALT);
    }

    /**
     * Ctrl+Z：撤销；Ctrl+Y：重做。手持连接装置时仅在<b>无线红石显示模式开启</b>且没有打开
     * 任何界面时上报（装置处于附属模组的其它模式时让位给该模式自己的撤销处理）；
     * 手持遥控开关则照旧（开关的历史独立于装置三模式）。
     * 具体作用于哪支历史栈、执行什么逆操作都由服务端裁定（历史不存在客户端）。
     */
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return; // 按住连发的 REPEAT 不处理：一次按键一次操作
        }
        if (event.getKey() != GLFW.GLFW_KEY_Z && event.getKey() != GLFW.GLFW_KEY_Y) {
            return;
        }
        if ((event.getModifiers() & GLFW.GLFW_MOD_CONTROL) == 0) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null || minecraft.getOverlay() != null) {
            return;
        }
        boolean holdsConnector = holds(minecraft.player, GeneratedMod.WIRELESS_CONNECTOR.get());
        boolean holdsSwitch = holds(minecraft.player, GeneratedMod.ORIGOLD_REMOTE_SWITCH.get());
        // 撤销 / 重做只对“正处于开启状态的模式”生效：无线红石显示模式关着时
        // （例如附属模组已把装置切到金源能量电线 / 无线物流管道模式）不上报本模组的撤销，
        // 让位给那个模式自己的撤销处理，也不会因为无线红石没有可撤销的操作而弹
        // “没有可撤销的操作”。遥控开关不属于装置的三模式之列，维持原行为。
        if (!holdsSwitch && !(holdsConnector && connectorDisplay)) {
            return;
        }
        ClientPacketDistributor.sendToServer(new WirelessUndoPayload(event.getKey() == GLFW.GLFW_KEY_Z));
    }

    /** 现做一条眼睛出发的方块射线（穿过实体、只取方块），超出给定距离返回 null。 */
    private static BlockHitResult raycastBlock(Minecraft minecraft, Player player, double range) {
        if (minecraft.level == null) {
            return null;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        return minecraft.level.clip(new ClipContext(
                eye, eye.add(look.scale(range)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
    }

    /** 手持连接装置 / 遥控开关时，准星命中的真实方块（统一 32 格射线），没有命中真实方块则返回 null。 */
    private static BlockPos aimedBlock(Minecraft minecraft, Player player) {
        BlockHitResult hit = raycastBlock(minecraft, player, WirelessRedstoneManager.INTERACT_RANGE);
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        return hit.getBlockPos();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        // 与持久化记忆对账（放在一切守卫之前，主界面里也照跑）：
        // 首个 tick 把退出游戏前记住的开关恢复到实时字段；之后若字段与记忆不一致，
        // 只能是附属模组绕过 API 反射直写了本字段——以字段（最新生效值）为准写回记忆并落盘。
        if (!connectorDisplayRestored) {
            connectorDisplayRestored = true;
            connectorDisplay = DisplayModeMemory.isOn(DisplayModeMemory.MODE_WIRELESS);
        } else if (connectorDisplay != DisplayModeMemory.isOn(DisplayModeMemory.MODE_WIRELESS)) {
            DisplayModeMemory.setOn(DisplayModeMemory.MODE_WIRELESS, connectorDisplay);
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        // 本模式不再可见时立刻丢掉悬停线路：视觉通道不会再更新它，留着会让 Delete / Backspace
        // 在模式关闭后仍然删掉一条玩家看不见的线路。
        if (!wirelessRedstoneVisible(minecraft.player)) {
            hoveredLink = null;
        }
        if (minecraft.screen != null) {
            return;
        }
        // 26.1.2：InputConstants.isKeyDown 直接接收 Window。
        Window window = minecraft.getWindow();
        boolean deleteDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_DELETE);
        if (connectorDisplay && holds(minecraft.player, GeneratedMod.WIRELESS_CONNECTOR.get())
                && hoveredLink != null && deleteDown) {
            ClientPacketDistributor.sendToServer(new WirelessDeletePayload(hoveredLink));
            hoveredLink = null;
        }
        // 遥控开关：按下 Delete 解除绑定。悬停选中走与线路悬停同源的穿墙几何检测——
        // 准星射线（32 格内）命中已绑定的方块即算选中，无视中间挡着的其它方块。
        if (minecraft.level != null && holds(minecraft.player, GeneratedMod.ORIGOLD_REMOTE_SWITCH.get())) {
            if (deleteDown && !unbindKeyHeld) {
                RemoteSwitchHolder holder = boundSwitch(minecraft.player);
                if (holder != null && holder.binding().dimension().equals(minecraft.level.dimension())
                        && WireVisualMath.rayHitsBlock(minecraft.player.getEyePosition(),
                                minecraft.player.getLookAngle(), holder.binding().position(),
                                WirelessRedstoneManager.INTERACT_RANGE)) {
                    ClientPacketDistributor.sendToServer(
                            new RemoteUnbindPayload(holder.binding().position().asLong()));
                }
            }
        }
        unbindKeyHeld = deleteDown;
        // 上升沿检测：一次按压 Backspace 只反转一次（悬停的线路方向）。
        boolean reverseDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_BACKSPACE);
        if (reverseDown && !reverseKeyHeld
                && connectorDisplay && holds(minecraft.player, GeneratedMod.WIRELESS_CONNECTOR.get())
                && hoveredLink != null) {
            ClientPacketDistributor.sendToServer(new WirelessReversePayload(hoveredLink));
        }
        reverseKeyHeld = reverseDown;
    }

    // ---------------------------------------------------------------- 视觉通道回调

    /** 本模组的无线红石视觉是否可见：开着本模式、且手上确实拿着连接装置。 */
    private static boolean wirelessRedstoneVisible(WireVisualContext context) {
        return wirelessRedstoneVisible(context.player());
    }

    private static boolean wirelessRedstoneVisible(Player player) {
        return connectorDisplay && holds(player, GeneratedMod.WIRELESS_CONNECTOR.get());
    }

    /** 无线红石通道：线路本体 + 两端端口 + 待选中转点 + 准星命中方块的外框提示。 */
    private static void collectWirelessRedstone(WireVisualContext context) {
        WireVisualCanvas canvas = context.canvas();
        // 悬停判定与画线共用本帧相机，"看着亮了"与"按下 Delete 删掉的"必是同一条线。
        hoveredLink = WireVisualLinks.hover(context, links, WirelessRedstoneManager.INTERACT_RANGE,
                WireVisualMath.DEFAULT_HOVER_THRESHOLD);
        WireVisualLinks.render(context, links, WIRE_LINE, null, hoveredLink);

        // 待选中转点：已选了一个点、还差第二个点时，在那个方块上画呼吸括号框与呼吸淡填充。
        SelectedPoint pending = selectedPoint;
        if (pending != null && pending.dim().equals(context.dimension())) {
            float breathe = context.breathe(2.0F, 0.0F);
            BlockPos pos = WirelessRedstoneData.fromLong(pending.pos());
            int color = pending.start() ? INPUT_COLOR : OUTPUT_COLOR;
            canvas.cornerBrackets(pos, (pending.start() ? PENDING_START : PENDING_END)
                    .alpha(0.35F + 0.62F * breathe), 0.3F);
            canvas.boxFill(pos, WireVisualColor.withAlpha(color, 0.10F + 0.34F * breathe),
                    WireVisualLayer.OVERLAY);
        }
        drawAimedBlock(canvas, context);
    }

    /**
     * 遥控开关通道：已绑定方块画淡蓝框（激活时更亮）；准星穿墙指到它时整块变白
     * （此时按 Delete 解绑），否则用白色四角括号提示准星物理命中的方块。
     */
    private static void collectRemoteSwitch(WireVisualContext context) {
        RemoteSwitchHolder holder = boundSwitch(context.player());
        if (holder == null) {
            return;
        }
        cn.blockforge.generated.origoldsar.RemoteSwitchBinding binding = holder.binding();
        if (!binding.dimension().equals(context.dimension())) {
            return; // 跨维度绑定只画这一端看不到的部分留给玩家自己理解：不在本维度就不画。
        }
        WireVisualCanvas canvas = context.canvas();
        BlockPos pos = binding.position();
        // 悬停选中：与线路悬停同源的穿墙几何检测。注意用的是相机射线而非玩家射线——
        // 玩家射线不含渲染帧插值，快速甩镜头时会比高亮慢一帧。
        boolean selected = DisplayModeMemory.isOn(DisplayModeMemory.MODE_REMOTE)
                && WireVisualMath.rayHitsBlock(context.camera(), context.look(), pos,
                        WirelessRedstoneManager.INTERACT_RANGE);
        if (selected) {
            canvas.boxFill(pos, WireVisualColor.withAlpha(WHITE, 0.65F), WireVisualLayer.OVERLAY);
            canvas.boxOutline(WireVisualCanvas.blockBox(pos, 0.006), SWITCH_SELECTED);
            return;
        }
        boolean on = binding.active();
        canvas.boxOutline(pos, SWITCH_BOX.alpha(on ? 0.9F : 0.6F));
        canvas.boxFill(pos, WireVisualColor.withAlpha(SWITCH_COLOR, on ? 0.3F : 0.18F), WireVisualLayer.OVERLAY);
        drawAimedBlock(canvas, context);
    }

    /** 准星物理命中的真实方块：白色四角括号（只提示，不填充）。 */
    private static void drawAimedBlock(WireVisualCanvas canvas, WireVisualContext context) {
        if (context.level() == null) {
            return;
        }
        BlockHitResult hit = context.level().clip(new ClipContext(context.eye(),
                context.eye().add(context.player().getLookAngle().scale(WirelessRedstoneManager.INTERACT_RANGE)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, context.player()));
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            canvas.cornerBrackets(hit.getBlockPos(), AIM_BRACKETS, 0.28F);
        }
    }

    // ---------------------------------------------------------------- 小工具

    private record RemoteSwitchHolder(cn.blockforge.generated.origoldsar.RemoteSwitchBinding binding) {}

    /** 取玩家手上（主手优先）遥控开关的绑定信息。 */
    private static RemoteSwitchHolder boundSwitch(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(GeneratedMod.ORIGOLD_REMOTE_SWITCH.get())
                    && stack.get(GeneratedMod.REMOTE_SWITCH_BINDING.get()) != null) {
                return new RemoteSwitchHolder(stack.get(GeneratedMod.REMOTE_SWITCH_BINDING.get()));
            }
        }
        return null;
    }

    private static boolean holds(Player player, Item item) {
        return player.getMainHandItem().is(item) || player.getOffhandItem().is(item);
    }

    private static boolean holds(ItemStack stack, Item item) {
        return stack.is(item);
    }
}
