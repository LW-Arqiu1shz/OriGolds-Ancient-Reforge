package cn.blockforge.generated.origoldsar.client;

import cn.blockforge.generated.origoldsar.GeneratedMod;
import cn.blockforge.generated.origoldsar.PlayerInfoPayload;
import cn.blockforge.generated.origoldsar.RemoteBindPayload;
import cn.blockforge.generated.origoldsar.RemoteUnbindPayload;
import cn.blockforge.generated.origoldsar.WirelessConnectorItem;
import cn.blockforge.generated.origoldsar.WirelessDeletePayload;
import cn.blockforge.generated.origoldsar.WirelessLinkSyncPayload;
import cn.blockforge.generated.origoldsar.WirelessRedstoneData;
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
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
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
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.systems.RenderSystem;

import java.util.List;
import java.util.UUID;

/**
 * 金源无线红石连接装置 / 遥控开关的客户端表现：
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
 *   <li>显示模式：穿墙渲染“输入→输出”渐变粒子线（源 A 蓝、终点 B 橙），
 *       两端分别为蓝色（输入端口 A）与橙色（输出端口 B）的方块盒；
 *       准星悬停某条线路时，线体与两个端点盒的<b>描边</b>变白（填充保持原色，
 *       仍能分清输入/输出）；</li>
 *   <li>显示模式下准星悬停某条线路时按 Delete：删除该线路；按 Backspace：反转该线路方向
 *       （交换输入端/输出端）。连电线/删电线/反转电线统一 32 格射程；</li>
 *   <li>已选了一个坐标点、还差第二个点时，在该点画一个呼吸灯式（2 秒周期明暗循环）
 *       的高亮框，提醒下一步该在哪补点；</li>
 *   <li>手持连接装置/遥控开关时，准星命中的方块用<b>白色描边</b>高亮（统一 32 格射程），
 *       提示用户正在选哪个方块；</li>
 *   <li>遥控开关的“悬停选中”与线路悬停同源：<b>穿墙几何检测</b>——准星射线（32 格内）
 *       命中已绑定的方块即选中它（无视中间遮挡），选中的方块<b>直接变成白色</b>
 *       （半透明白色填充；不用“提高亮度”的加法叠加，那会与方块表面深度冲突而鬼畜闪烁），
 *       此时按 <b>Delete</b> 解除绑定；</li>
 *   <li><b>Ctrl+Z / Ctrl+Y</b>：撤销 / 重做。手持任意连接装置且<b>无线红石显示模式开启</b>时，
 *       作用于该玩家的无线线路操作历史（新增/删除连线、反转方向）；模式关着（装置处于
 *       附属模组加入的其它模式）时不上报，既不撤回别的模式的操作，也不会弹"没有可撤销的操作"；
 *       手持某一遥控开关则只作用于这一把开关自己的
 *       绑定/解绑历史（每一把独立保存，最多 16 条，退出世界即清除）。</li>
 *   <li><b>三模式互斥</b>：见 {@link #connectorDisplay} —— 附属模组《OriGolds: Industrialized
 *       Age》给连接装置加入"金源能量电线 / 无线物流管道"两个新模式，任一模式开启时通过本类
 *       的公共静态 boolean 字段 {@code connectorDisplay} 关闭/查询本模组的无线红石显示模式，
 *       实现三模式互斥；本模组的选点、删除、反转、撤销、重做、悬停、渲染与呼吸提示框
 *       全部以该字段为门控，只有本模式开启时才对无线红石线路生效。
 *       切换提示也随之区分：因附属开启其它模式而被动关闭时<b>不弹任何提示</b>（关掉本模式
 *       是附属模式开启的附带结果，不是玩家的操作）；只有玩家主动按快捷键关闭时才弹
 *       “无线红石显示模式：关”；从附属的其它模式切回本模式时弹
 *       “已切换至无线红石显示模式”（由 {@link AddonModeBridge} 只读探测）。</li>
 * </ul>
 *
 * <p>渲染直接在 {@code AFTER_WEATHER} 阶段以 {@code POSITION_COLOR} 格式绘制，
 * 并临时关闭深度测试实现“可穿墙”。输入回调的写法与 {@link GrapnelClient} 一致。
 */
@EventBusSubscriber(modid = GeneratedMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class WirelessConnectorClient {
    /** 服务端同步下来的全部无线红石线路。 */
    private static List<WirelessLinkSyncPayload.LinkView> links = List.of();
    /** 当前准星悬停的线路 id（显示模式下有效），没有则为 null。 */
    private static UUID hoveredLink = null;
    /** 当前“待选中转点”：已选了一个坐标点、还差第二个点（由服务端同步），没有则为 null。 */
    private static SelectedPoint selectedPoint = null;
    /** 上一次 tick 反转键（G）是否按下，用于上升沿检测（一次按键只反转一次）。 */
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

    /** 输入端口（线路源 A，被采样红石信号的那端）高亮盒颜色：蓝色。 */
    private static final float INPUT_R = 0.25F, INPUT_G = 0.55F, INPUT_B = 1.0F;
    /** 输出端口（线路终点 B，被无线供电的那端）高亮盒颜色：橙色。 */
    private static final float OUTPUT_R = 1.0F, OUTPUT_G = 0.55F, OUTPUT_B = 0.1F;

    // ---------------------------------------------------------------- NEO 样式（与 26.x 版一致）
    // 客户端配置 wireStyle = NEO（默认）时，本类的经典渲染整段跳过，改由 api/visual 模块
    // （WireVisuals + WireVisualCanvas）画：渐变 + 流光 + 辉光的面向相机线带、四角括号、呼吸框。

    private static final int INPUT_COLOR = 0xFF408CFF;
    private static final int OUTPUT_COLOR = 0xFFFF8C1A;
    private static final int SWITCH_COLOR = 0xFF80CCFF;
    private static final int WHITE = 0xFFFFFFFF;

    private static final WireVisualStyle WIRE_LINE = WireVisualStyle.gradient(INPUT_COLOR, OUTPUT_COLOR)
            .widthScale(1.0F).flow(1.6F).glow(1.2F)
            .layer(WireVisualLayer.OVERLAY);
    private static final WireVisualStyle PENDING_START =
            WireVisualStyle.of(INPUT_COLOR).widthScale(1.0F).glow(0.9F);
    private static final WireVisualStyle PENDING_END =
            WireVisualStyle.of(OUTPUT_COLOR).widthScale(1.0F).glow(0.9F);
    private static final WireVisualStyle AIM_BRACKETS = WireVisualStyle.hairline(WHITE).widthScale(1.0F);
    private static final WireVisualStyle SWITCH_BOX =
            WireVisualStyle.of(SWITCH_COLOR).widthScale(1.0F).glow(0.8F);
    private static final WireVisualStyle SWITCH_SELECTED = WireVisualStyle.hairline(WHITE).widthScale(1.0F);

    private static final ResourceLocation CHANNEL_WIRELESS =
            ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID, "wireless_redstone");
    private static final ResourceLocation CHANNEL_REMOTE_SWITCH =
            ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID, "remote_switch");

    /** NEO 样式用的线路视图（与 {@link #links} 同步更新）。 */
    private static List<WireVisualLink> neoLinks = List.of();

    private WirelessConnectorClient() {}

    /** 注册 NEO 样式的两路视觉通道（客户端初始化时调用一次；CLASSIC 样式下通道不产生几何）。 */
    public static void registerVisualChannels() {
        WireVisuals.register(WireVisualSource.of(CHANNEL_WIRELESS, 1000,
                context -> WireStyleConfig.isNeo() && wirelessRedstoneVisible(context.player()),
                WirelessConnectorClient::collectWirelessRedstone));
        WireVisuals.register(WireVisualSource.of(CHANNEL_REMOTE_SWITCH, 1010,
                context -> WireStyleConfig.isNeo()
                        && holds(context.player(), GeneratedMod.ORIGOLD_REMOTE_SWITCH.get()),
                WirelessConnectorClient::collectRemoteSwitch));
    }

    /** 由服务端同步包处理器调用（见 {@code WirelessRedstoneNetwork}）。 */
    public static void onLinkSync(WirelessLinkSyncPayload payload) {
        links = payload.links();
        List<WireVisualLink> views = new java.util.ArrayList<>(payload.links().size());
        for (WirelessLinkSyncPayload.LinkView link : payload.links()) {
            views.add(WireVisualLink.of(link.id(), link.dimA(), WirelessRedstoneData.fromLong(link.posA()),
                    link.dimB(), WirelessRedstoneData.fromLong(link.posB()), WIRE_LINE));
        }
        neoLinks = List.copyOf(views);
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
            BlockHitResult hit = raycastBlock(minecraft, player,
                    cn.blockforge.generated.origoldsar.WirelessRedstoneManager.INTERACT_RANGE);
            if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                // 连接装置的选点属于无线红石模式：模式关闭（例如附属模组把装置切到了
                // 金源能量电线 / 无线物流管道模式）时不触发本模式的选点，让位给当前模式；
                // 此时若另一只手还拿着遥控开关，则退让为开关绑定（与服务端择栈优先级一致）。
                if (holdsConnector && connectorDisplay) {
                    PacketDistributor.sendToServer(new WirelessSelectPayload(hit.getBlockPos().asLong(), true));
                    event.setCanceled(true);
                } else if (holdsSwitch) {
                    PacketDistributor.sendToServer(new RemoteBindPayload(hit.getBlockPos().asLong()));
                    event.setCanceled(true);
                }
            }
        } else if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT && isSneak(event, player) && holdsConnector) {
            // 潜行 + 右键：连接装置记录终点方块（用 32 格射线，不受普通触及距离限制），并阻止
            // 把它当成一次普通右键使用方块（开箱、按按钮等）。终点选择不再走 useOn，统一走 32 格射线。
            if (!connectorDisplay) {
                return; // 同上：右键选点同样只在无线红石模式开启时生效。
            }
            BlockHitResult hit = raycastBlock(minecraft, player,
                    cn.blockforge.generated.origoldsar.WirelessRedstoneManager.INTERACT_RANGE);
            if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                PacketDistributor.sendToServer(new WirelessSelectPayload(hit.getBlockPos().asLong(), false));
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
        requestInfo(on
                ? "message.origolds_ar.rs_display_on" : "message.origolds_ar.rs_display_off");
    }

    /**
     * 本地触发的信息栏提示：交给服务端走父模组《金源工艺》的玩家信息栏接口显示，
     * 这样多人游戏里同样生效，也不会与其它模组的持续信息互相覆盖。
     */
    private static void requestInfo(String translationKey) {
        PacketDistributor.sendToServer(new PlayerInfoPayload(translationKey));
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
        long window = Minecraft.getInstance().getWindow().getWindow();
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
        PacketDistributor.sendToServer(new WirelessUndoPayload(event.getKey() == GLFW.GLFW_KEY_Z));
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

    /** 手持连接装置/遥控开关时，准星命中的真实方块（统一 32 格射线），没有命中真实方块则返回 null。 */
    private static BlockPos aimedBlock(Minecraft minecraft, Player player) {
        BlockHitResult hit = raycastBlock(minecraft, player,
                cn.blockforge.generated.origoldsar.WirelessRedstoneManager.INTERACT_RANGE);
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        return hit.getBlockPos();
    }

    /**
     * 准星射线与方块包围盒的相交检测（slab 法）：与线路悬停同源的<b>穿墙</b>几何选中——
     * 只要射线在 {@code range} 格内掠过该方块（四周一并放 0.05 容差），无论中间有没有
     * 别的方块挡着都算选中。
     */
    private static boolean rayHitsBlock(Vec3 eye, Vec3 look, BlockPos pos, double range) {
        double[] o = {eye.x, eye.y, eye.z};
        double[] d = {look.x, look.y, look.z};
        double[] loB = {pos.getX() - 0.05, pos.getY() - 0.05, pos.getZ() - 0.05};
        double[] hiB = {pos.getX() + 1.05, pos.getY() + 1.05, pos.getZ() + 1.05};
        double lo = 0.0;
        double hi = range;
        for (int i = 0; i < 3; i++) {
            if (Math.abs(d[i]) < 1.0E-8) {
                if (o[i] < loB[i] || o[i] > hiB[i]) {
                    return false;
                }
            } else {
                double t1 = (loB[i] - o[i]) / d[i];
                double t2 = (hiB[i] - o[i]) / d[i];
                if (t1 > t2) {
                    double t = t1;
                    t1 = t2;
                    t2 = t;
                }
                lo = Math.max(lo, t1);
                hi = Math.min(hi, t2);
                if (lo > hi) {
                    return false;
                }
            }
        }
        return true;
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
        // NEO 样式下悬停线路由视觉通道每帧刷新；本模式不可见时通道不再运行，
        // 立刻丢掉悬停，免得 Delete / Backspace 删掉一条看不见的线路。
        if (WireStyleConfig.isNeo() && !wirelessRedstoneVisible(minecraft.player)) {
            hoveredLink = null;
        }
        if (minecraft.screen != null) {
            return;
        }
        long window = minecraft.getWindow().getWindow();
        boolean deleteDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_DELETE);
        if (connectorDisplay && holds(minecraft.player, GeneratedMod.WIRELESS_CONNECTOR.get())
                && hoveredLink != null && deleteDown) {
            PacketDistributor.sendToServer(new WirelessDeletePayload(hoveredLink));
            hoveredLink = null;
        }
        // 遥控开关：按下 Delete 解除绑定。悬停选中走与线路悬停同源的穿墙几何检测——
        // 准星射线（32 格内）命中已绑定的方块即算选中，无视中间挡着的其它方块。
        if (minecraft.level != null && holds(minecraft.player, GeneratedMod.ORIGOLD_REMOTE_SWITCH.get())) {
            if (deleteDown && !unbindKeyHeld) {
                RemoteSwitchHolder holder = boundSwitch(minecraft.player);
                if (holder != null && holder.binding().dimension().equals(minecraft.level.dimension())
                        && rayHitsBlock(minecraft.player.getEyePosition(), minecraft.player.getLookAngle(),
                                holder.binding().position(),
                                cn.blockforge.generated.origoldsar.WirelessRedstoneManager.INTERACT_RANGE)) {
                    PacketDistributor.sendToServer(new RemoteUnbindPayload(holder.binding().position().asLong()));
                }
            }
        }
        unbindKeyHeld = deleteDown;
        // 上升沿检测：一次按压 Backspace 只反转一次（悬停的线路方向）。
        boolean reverseDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_BACKSPACE);
        if (reverseDown && !reverseKeyHeld
                && connectorDisplay && holds(minecraft.player, GeneratedMod.WIRELESS_CONNECTOR.get())
                && hoveredLink != null) {
            PacketDistributor.sendToServer(new WirelessReversePayload(hoveredLink));
        }
        reverseKeyHeld = reverseDown;
    }

    // ---------------------------------------------------------------- 渲染

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }
        // NEO 样式由 WireVisuals 的视觉通道负责绘制（含悬停判定），这里的经典渲染整段跳过。
        if (WireStyleConfig.isNeo()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        ResourceKey<Level> dim = minecraft.level.dimension();
        boolean holdsConnector = holds(minecraft.player, GeneratedMod.WIRELESS_CONNECTOR.get());
        boolean holdsSwitch = holds(minecraft.player, GeneratedMod.ORIGOLD_REMOTE_SWITCH.get());
        boolean showConnector = connectorDisplay && holdsConnector;
        boolean showSwitch = DisplayModeMemory.isOn(DisplayModeMemory.MODE_REMOTE) && holdsSwitch;
        // 待选中转点与准星白色描边同样只在无线红石模式开启时显示（附属模组的其它模式有自己的提示渲染）。
        boolean connectorActive = connectorDisplay && holdsConnector;
        boolean showPending = connectorActive && selectedPoint != null && selectedPoint.dim().equals(dim);
        if (!showConnector) {
            hoveredLink = null;
        }

        Vec3 cam = event.getCamera().getPosition();
        org.joml.Vector3f camLook = event.getCamera().getLookVector();
        Vec3 look = new Vec3(camLook.x, camLook.y, camLook.z);

        // 遥控开关的“悬停选中”与线路悬停同源：穿墙几何检测——准星射线（32 格内）命中
        // 已绑定的方块即选中它，无视中间挡着的其它方块。选中后该方块直接变白（见下）。
        BlockPos switchTarget = null;
        if (holdsSwitch) {
            RemoteSwitchHolder holder = boundSwitch(minecraft.player);
            if (holder != null && holder.binding().dimension().equals(dim)
                    && rayHitsBlock(cam, look, holder.binding().position(),
                            cn.blockforge.generated.origoldsar.WirelessRedstoneManager.INTERACT_RANGE)) {
                switchTarget = holder.binding().position();
            }
        }
        boolean whitenBound = switchTarget != null;
        // 手持连接装置/遥控开关时，准星物理命中的真实方块（统一 32 格射线）→ 白色描边提示；
        // 穿墙选中已绑定方块时不再描物理命中块（那通常是挡在前面的墙），以变白提示优先。
        BlockPos aimed = (!whitenBound && (connectorActive || holdsSwitch))
                ? aimedBlock(minecraft, minecraft.player) : null;
        boolean showAimed = aimed != null;
        if (!showConnector && !showSwitch && !showPending && !showAimed && !whitenBound) {
            return;
        }

        // 悬停检测：连接装置显示模式下，找离准星视线最近的线路。
        if (showConnector) {
            computeHoveredLink(event, dim);
        }

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();

        // 第一遍：线（粒子线 + 方块盒描边）。
        BufferBuilder lines = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        int lineVerts = 0;
        if (showConnector) {
            for (WirelessLinkSyncPayload.LinkView link : links) {
                boolean aHere = link.dimA().equals(dim);
                boolean bHere = link.dimB().equals(dim);
                Vec3 a = center(link.posA());
                Vec3 b = center(link.posB());
                boolean hovered = link.id().equals(hoveredLink);
                // 渐变粒子线（两端同维度才画得出线）：输入端蓝 → 输出端橙；悬停整体描白。
                if (aHere && bHere) {
                    if (hovered) {
                        lineVerts = segment(matrix, lines, a, b, 1.0F, 1.0F, 1.0F, 0.95F, lineVerts);
                    } else {
                        lineVerts = segment(matrix, lines, a, b,
                                INPUT_R, INPUT_G, INPUT_B, OUTPUT_R, OUTPUT_G, OUTPUT_B, 0.85F, lineVerts);
                    }
                }
                if (aHere) {
                    lineVerts = boxEdges(matrix, lines, a,
                            hovered ? 1.0F : INPUT_R, hovered ? 1.0F : INPUT_G, hovered ? 1.0F : INPUT_B,
                            hovered ? 0.95F : 0.75F, lineVerts);
                }
                if (bHere) {
                    lineVerts = boxEdges(matrix, lines, b,
                            hovered ? 1.0F : OUTPUT_R, hovered ? 1.0F : OUTPUT_G, hovered ? 1.0F : OUTPUT_B,
                            hovered ? 0.95F : 0.75F, lineVerts);
                }
            }
        }
        if (showSwitch) {
            RemoteSwitchHolder holder = boundSwitch(minecraft.player);
            if (holder != null && holder.binding().dimension().equals(dim)) {
                Vec3 c = center(holder.binding().position());
                boolean on = holder.binding().active();
                lineVerts = boxEdges(matrix, lines, c, 0.5F, 0.8F, 1.0F, on ? 0.9F : 0.6F, lineVerts);
            }
        }
        // 待选中转点：呼吸灯式高亮框（已选一个点、还差第二个点时）。
        if (showPending) {
            SelectedPoint sp = selectedPoint;
            float breathe = breatheAlpha();
            float ar = sp.start() ? INPUT_R : OUTPUT_R;
            float ag = sp.start() ? INPUT_G : OUTPUT_G;
            float ab = sp.start() ? INPUT_B : OUTPUT_B;
            lineVerts = boxEdges(matrix, lines, center(sp.pos()), ar, ag, ab, 0.35F + 0.55F * breathe, lineVerts);
        }
        // 准星物理命中的方块：白色描边（只描边，不填充）。
        if (showAimed) {
            lineVerts = boxEdges(matrix, lines, center(aimed), 1.0F, 1.0F, 1.0F, 0.9F, lineVerts);
        }
        // 遥控开关穿墙选中的已绑定方块：白色描边（填充在第二遍，整体变白）。
        if (whitenBound) {
            lineVerts = boxEdges(matrix, lines, center(switchTarget), 1.0F, 1.0F, 1.0F, 0.95F, lineVerts);
        }
        if (lineVerts > 0) {
            BufferUploader.drawWithShader(lines.buildOrThrow());
        }

        // 第二遍：半透明填充盒（输入端蓝 / 输出端橙，悬停时保持原色；遥控开关淡蓝；待选中转点呼吸）。
        BufferBuilder quads = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int quadVerts = 0;
        if (showConnector) {
            for (WirelessLinkSyncPayload.LinkView link : links) {
                if (link.dimA().equals(dim)) {
                    quadVerts = boxFill(matrix, quads, center(link.posA()), INPUT_R, INPUT_G, INPUT_B, 0.25F, quadVerts);
                }
                if (link.dimB().equals(dim)) {
                    quadVerts = boxFill(matrix, quads, center(link.posB()), OUTPUT_R, OUTPUT_G, OUTPUT_B, 0.25F, quadVerts);
                }
            }
        }
        if (showSwitch) {
            RemoteSwitchHolder holder = boundSwitch(minecraft.player);
            if (holder != null && holder.binding().dimension().equals(dim)) {
                boolean on = holder.binding().active();
                quadVerts = boxFill(matrix, quads, center(holder.binding().position()),
                        0.5F, 0.8F, 1.0F, on ? 0.3F : 0.18F, quadVerts);
            }
        }
        // 待选中转点的呼吸式填充盒。
        if (showPending) {
            SelectedPoint sp = selectedPoint;
            float breathe = breatheAlpha();
            float ar = sp.start() ? INPUT_R : OUTPUT_R;
            float ag = sp.start() ? INPUT_G : OUTPUT_G;
            float ab = sp.start() ? INPUT_B : OUTPUT_B;
            quadVerts = boxFill(matrix, quads, center(sp.pos()), ar, ag, ab, 0.10F + 0.35F * breathe, quadVerts);
        }
        // 遥控开关穿墙选中的已绑定方块：直接变成白色——半透明白色填充（覆盖原色），
        // 与描边同在穿墙层绘制，不开深度测试，因此不会再有深度冲突导致的鬼畜闪烁。
        if (whitenBound) {
            quadVerts = boxFill(matrix, quads, center(switchTarget), 1.0F, 1.0F, 1.0F, 0.65F, quadVerts);
        }
        if (quadVerts > 0) {
            BufferUploader.drawWithShader(quads.buildOrThrow());
        }

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    /** 准星射线与各条线路（线段）的最短距离，阈值内取最近的一条作为悬停。 */
    private static void computeHoveredLink(RenderLevelStageEvent event, ResourceKey<Level> dim) {
        Minecraft minecraft = Minecraft.getInstance();
        Vec3 eye = event.getCamera().getPosition();
        org.joml.Vector3f lv = event.getCamera().getLookVector();
        Vec3 look = new Vec3(lv.x, lv.y, lv.z);
        UUID best = null;
        double bestDistSq = 0.6 * 0.6;
        for (WirelessLinkSyncPayload.LinkView link : links) {
            if (!link.dimA().equals(dim) || !link.dimB().equals(dim)) {
                continue;
            }
            Vec3 a = center(link.posA());
            Vec3 b = center(link.posB());
            Vec3 ab = b.subtract(a);
            double lenSq = ab.lengthSqr();
            if (lenSq < 1.0E-4) {
                continue;
            }
            // 在线段上采样找离射线（eye + t*look）最近的点。
            int steps = (int) Math.min(64, Math.max(8, ab.length() * 2));
            for (int i = 0; i <= steps; i++) {
                double t = (double) i / steps;
                Vec3 p = a.add(ab.x * t, ab.y * t, ab.z * t);
                Vec3 v = p.subtract(eye);
                double along = v.dot(look);
                double rangeSq = cn.blockforge.generated.origoldsar.WirelessRedstoneManager.INTERACT_RANGE
                        * cn.blockforge.generated.origoldsar.WirelessRedstoneManager.INTERACT_RANGE;
                if (along <= 0.5 || v.lengthSqr() > rangeSq) {
                    continue;
                }
                double distSq = v.lengthSqr() - along * along;
                if (distSq < bestDistSq) {
                    bestDistSq = distSq;
                    best = link.id();
                }
            }
        }
        hoveredLink = best;
    }

    // ---------------------------------------------------------------- 绘制小工具

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

    private static Vec3 center(long packedPos) {
        return Vec3.atCenterOf(WirelessRedstoneData.fromLong(packedPos));
    }

    private static Vec3 center(net.minecraft.core.BlockPos pos) {
        return Vec3.atCenterOf(pos);
    }

    private static int segment(Matrix4f m, BufferBuilder buffer, Vec3 a, Vec3 b,
                               float r, float g, float bl, float alpha, int verts) {
        buffer.addVertex(m, (float) a.x, (float) a.y, (float) a.z).setColor(r, g, bl, alpha);
        buffer.addVertex(m, (float) b.x, (float) b.y, (float) b.z).setColor(r, g, bl, alpha);
        return verts + 2;
    }

    /** 渐变线段：起点用 (r1,g1,b1)、终点用 (r2,g2,b2)，中间由 GPU 插值，表示输入→输出的流向。 */
    private static int segment(Matrix4f m, BufferBuilder buffer, Vec3 a, Vec3 b,
                               float r1, float g1, float b1, float r2, float g2, float b2,
                               float alpha, int verts) {
        buffer.addVertex(m, (float) a.x, (float) a.y, (float) a.z).setColor(r1, g1, b1, alpha);
        buffer.addVertex(m, (float) b.x, (float) b.y, (float) b.z).setColor(r2, g2, b2, alpha);
        return verts + 2;
    }

    /** 呼吸灯亮度（0..1）：以 2 秒为周期，先由亮到灭再到亮（正弦曲线）。 */
    private static float breatheAlpha() {
        long now = System.currentTimeMillis();
        double phase = (now % 2000L) / 2000.0;
        return (float) (1.0 - Math.sin(phase * Math.PI));
    }

    private static final int[][] BOX_EDGES = {
            {0, 1}, {1, 3}, {3, 2}, {2, 0}, {4, 5}, {5, 7}, {7, 6}, {6, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };
    private static final double[][] BOX_CORNERS = {
            {0, 0, 0}, {1, 0, 0}, {0, 1, 0}, {1, 1, 0}, {0, 0, 1}, {1, 0, 1}, {0, 1, 1}, {1, 1, 1}
    };

    private static int boxEdges(Matrix4f m, BufferBuilder buffer, Vec3 center,
                                float r, float g, float b, float alpha, int verts) {
        double x = center.x - 0.5;
        double y = center.y - 0.5;
        double z = center.z - 0.5;
        for (int[] edge : BOX_EDGES) {
            double[] p1 = BOX_CORNERS[edge[0]];
            double[] p2 = BOX_CORNERS[edge[1]];
            buffer.addVertex(m, (float) (x + p1[0]), (float) (y + p1[1]), (float) (z + p1[2])).setColor(r, g, b, alpha);
            buffer.addVertex(m, (float) (x + p2[0]), (float) (y + p2[1]), (float) (z + p2[2])).setColor(r, g, b, alpha);
            verts += 2;
        }
        return verts;
    }

    /** 6 个面（每面两个三角形用 QUADS 的 4 顶点），共 24 顶点。 */
    private static final int[][] BOX_FACES = {
            {0, 1, 3, 2}, {4, 6, 7, 5}, {0, 2, 6, 4}, {1, 5, 7, 3}, {0, 4, 5, 1}, {2, 3, 7, 6}
    };

    private static int boxFill(Matrix4f m, BufferBuilder buffer, Vec3 center,
                               float r, float g, float b, float alpha, int verts) {
        double x = center.x - 0.5;
        double y = center.y - 0.5;
        double z = center.z - 0.5;
        for (int[] face : BOX_FACES) {
            for (int idx : face) {
                double[] p = BOX_CORNERS[idx];
                buffer.addVertex(m, (float) (x + p[0]), (float) (y + p[1]), (float) (z + p[2])).setColor(r, g, b, alpha);
                verts++;
            }
        }
        return verts;
    }

    // ---------------------------------------------------------------- NEO 样式的视觉通道回调

    private static boolean wirelessRedstoneVisible(Player player) {
        return connectorDisplay && holds(player, GeneratedMod.WIRELESS_CONNECTOR.get());
    }

    /** 无线红石通道：线路本体 + 两端端口 + 待选中转点 + 准星命中方块的四角括号。 */
    private static void collectWirelessRedstone(WireVisualContext context) {
        WireVisualCanvas canvas = context.canvas();
        // 悬停判定与画线共用本帧相机，"看着亮了"与"按下 Delete 删掉的"必是同一条线。
        hoveredLink = WireVisualLinks.hover(context, neoLinks,
                cn.blockforge.generated.origoldsar.WirelessRedstoneManager.INTERACT_RANGE,
                WireVisualMath.DEFAULT_HOVER_THRESHOLD);
        WireVisualLinks.render(context, neoLinks, WIRE_LINE, null, hoveredLink);

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

    /** 遥控开关通道：绑定方块淡蓝框；准星穿墙指到它时整块变白（此时按 Delete 解绑）。 */
    private static void collectRemoteSwitch(WireVisualContext context) {
        RemoteSwitchHolder holder = boundSwitch(context.player());
        if (holder == null) {
            return;
        }
        cn.blockforge.generated.origoldsar.RemoteSwitchBinding binding = holder.binding();
        if (!binding.dimension().equals(context.dimension())) {
            return;
        }
        WireVisualCanvas canvas = context.canvas();
        BlockPos pos = binding.position();
        boolean selected = DisplayModeMemory.isOn(DisplayModeMemory.MODE_REMOTE)
                && WireVisualMath.rayHitsBlock(context.camera(), context.look(), pos,
                        cn.blockforge.generated.origoldsar.WirelessRedstoneManager.INTERACT_RANGE);
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
                context.eye().add(context.player().getLookAngle()
                        .scale(cn.blockforge.generated.origoldsar.WirelessRedstoneManager.INTERACT_RANGE)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, context.player()));
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            canvas.cornerBrackets(hit.getBlockPos(), AIM_BRACKETS, 0.28F);
        }
    }

    private static boolean holds(Player player, Item item) {
        return player.getMainHandItem().is(item) || player.getOffhandItem().is(item);
    }

    private static boolean holds(ItemStack stack, Item item) {
        return stack.is(item);
    }
}
