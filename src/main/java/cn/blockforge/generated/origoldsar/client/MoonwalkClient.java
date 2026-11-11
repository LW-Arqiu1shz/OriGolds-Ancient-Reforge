package cn.blockforge.generated.origoldsar.client;

import cn.blockforge.generated.origoldsar.AbilityItems;
import cn.blockforge.generated.origoldsar.GeneratedMod;
import cn.blockforge.generated.origoldsar.MoonwalkLunarPayload;
import cn.blockforge.generated.origoldsar.MoonwalkTogglePayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * 月亮行者勋章输入检测，两个互不干扰的触发：
 * <ul>
 *   <li>按住“月行”键（默认空格）时双击潜行键：反转自身的重力方向。</li>
 *   <li>按住潜行键时双击“月行”键：触发月球重力（重力降为 25%、终端下落速度降为 40%）。</li>
 * </ul>
 * 月球重力的重力修饰符与空气阻力调整由服务端 {@link cn.blockforge.generated.origoldsar.MoonwalkNetwork}
 * 与 {@code LunarGravityMixin} 负责；空气按住潜行的“双倍常态重力”属于持续按住而非双击，
 * 由 {@code DoubleGravityMixin} 在客户端与服务端结合 {@code onGround()} 与 {@code isShiftKeyDown()}
 * 各自判定并改 {@code getGravity()} 返回值，无需在这里发包。
 *
 * <p>与刺客突进一样直接轮询物理按键状态（26.3 底层为 SDL），避免 KeyMapping 内部状态在界面/失焦时被弄脏，
 * 导致双击“按下沿”检测不到。
 */
@EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT)
public final class MoonwalkClient {
    /** 两次按下之间的最大间隔（0.6 秒）。 */
    private static final int DOUBLE_PRESS_TICKS = 12;
    private static final int COOLDOWN_TICKS = 15;

    private static final KeyMapping MOONWALK = OrigoldKeyBindings.MOONWALK;

    private static int clientTick;
    private static int invertCooldownUntil;
    private static int lunarCooldownUntil;
    private static boolean prevShiftDown;
    private static boolean prevMoonwalkDown;
    private static int lastShiftTap = -9999;
    private static int lastMoonwalkTap = -9999;

    private MoonwalkClient() {}

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            clientTick++;
            prevShiftDown = false;
            prevMoonwalkDown = false;
            lastShiftTap = -9999;
            lastMoonwalkTap = -9999;
            return;
        }
        clientTick++;

        // 打开任何界面时不触发，并清一次边缘与双击记录，避免界面吞键产生假双击。
        if (mc.gui.screen() != null) {
            prevShiftDown = false;
            prevMoonwalkDown = false;
            lastShiftTap = -9999;
            lastMoonwalkTap = -9999;
            return;
        }

        boolean holdMoonwalk = isPhysicallyDown(MOONWALK);
        boolean shiftDown = isPhysicallyDown(mc.options.keyShift);
        boolean shiftEdge = shiftDown && !prevShiftDown;
        boolean moonwalkEdge = holdMoonwalk && !prevMoonwalkDown;
        prevShiftDown = shiftDown;
        prevMoonwalkDown = holdMoonwalk;

        // 触发一：双击必须发生在“按住月行键”期间；按下之前敲的潜行键一律丢弃。
        if (!holdMoonwalk) {
            lastShiftTap = -9999;
        } else if (shiftEdge) {
            if (clientTick - lastShiftTap <= DOUBLE_PRESS_TICKS
                    && clientTick >= invertCooldownUntil
                    && hasMedallion(player)) {
                invertCooldownUntil = clientTick + COOLDOWN_TICKS;
                ClientPacketDistributor.sendToServer(new MoonwalkTogglePayload());
                lastShiftTap = -9999;
            } else {
                lastShiftTap = clientTick;
            }
        }

        // 触发二：双击必须发生在“按住潜行键”期间；按下之前敲的月行键一律丢弃。
        if (!shiftDown) {
            lastMoonwalkTap = -9999;
        } else if (moonwalkEdge) {
            if (clientTick - lastMoonwalkTap <= DOUBLE_PRESS_TICKS
                    && clientTick >= lunarCooldownUntil
                    && hasMedallion(player)) {
                lunarCooldownUntil = clientTick + COOLDOWN_TICKS;
                ClientPacketDistributor.sendToServer(new MoonwalkLunarPayload());
                lastMoonwalkTap = -9999;
            } else {
                lastMoonwalkTap = clientTick;
            }
        }
    }

    /**
     * 读取按键物理状态；鼠标/其它类型按键退回 KeyMapping 状态。直接查物理状态，
     * 不依赖会被界面与失焦弄脏的 KeyMapping 内部状态。
     */
    private static boolean isPhysicallyDown(KeyMapping mapping) {
        InputConstants.Key key = mapping.getKey();
        if (key.getType() == InputConstants.Type.KEYBOARD && key.getValue() != InputConstants.UNKNOWN.getValue()) {
            return InputConstants.isKeyDown(key.getValue());
        }
        return mapping.isDown();
    }

    private static boolean hasMedallion(LocalPlayer player) {
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (AbilityItems.hasMedallionAbility(stack, GeneratedMod.MOONWALKER_MEDALLION.get())) {
                return true;
            }
        }
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        return AbilityItems.hasMedallionAbility(main, GeneratedMod.MOONWALKER_MEDALLION.get())
                || AbilityItems.hasMedallionAbility(off, GeneratedMod.MOONWALKER_MEDALLION.get());
    }
}
