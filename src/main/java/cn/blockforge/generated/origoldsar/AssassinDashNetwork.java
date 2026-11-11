package cn.blockforge.generated.origoldsar;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = GeneratedMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class AssassinDashNetwork {
    /** 3 seconds at 20 ticks/sec. */
    public static final int COOLDOWN_TICKS = 60;
    /** +48 blocks/sec horizontal speed = 48 / 20 = 2.4 blocks per tick. */
    private static final double DASH_SPEED = 48.0D / 20.0D;
    /** +16 blocks/sec vertical speed = 16 / 20 = 0.8 blocks per tick. */
    private static final double LEAP_SPEED = 16.0D / 20.0D;
    private static final String COOLDOWN_TAG = "origolds_ar_assassin_dash_cooldown";
    /** 大跳与水平冲刺各自独立计算冷却（时长同为 3 秒）。 */
    private static final String LEAP_COOLDOWN_TAG = "origolds_ar_assassin_leap_cooldown";

    private AssassinDashNetwork() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(
                AssassinDashPayload.TYPE,
                AssassinDashPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    Player player = context.player();
                    if (player == null || !hasMedallionInHand(player)) {
                        return;
                    }
                    long now = player.level().getGameTime();
                    if (now < player.getPersistentData().getLong(COOLDOWN_TAG)) {
                        return;
                    }
                    player.getPersistentData().putLong(COOLDOWN_TAG, now + COOLDOWN_TICKS);
                    applyDash(player, payload.direction());
                    player.playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 1.0F, 0.6F);
                }));
        registrar.playToServer(
                AssassinLeapPayload.TYPE,
                AssassinLeapPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    Player player = context.player();
                    if (player == null || !hasMedallionInHand(player)) {
                        return;
                    }
                    long now = player.level().getGameTime();
                    if (now < player.getPersistentData().getLong(LEAP_COOLDOWN_TAG)) {
                        return;
                    }
                    player.getPersistentData().putLong(LEAP_COOLDOWN_TAG, now + COOLDOWN_TICKS);
                    applyLeap(player);
                    player.playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 1.0F, 0.9F);
                }));
    }

    private static void applyLeap(Player player) {
        Vec3 current = player.getDeltaMovement();
        player.setDeltaMovement(current.x, LEAP_SPEED, current.z);
        player.fallDistance = 0.0F;
        // 强制服务端把新的运动量同步回客户端。
        player.hurtMarked = true;
    }

    private static void applyDash(Player player, byte direction) {
        float rad = player.getYRot() * Mth.DEG_TO_RAD;
        float s = Mth.sin(rad);
        float c = Mth.cos(rad);
        double dx;
        double dz;
        switch (direction) {
            case 1 -> { dx = c;  dz = s;  }   // left  (A)
            case 2 -> { dx = s;  dz = -c; }   // back  (S)
            case 3 -> { dx = -c; dz = -s; }   // right (D)
            default -> { dx = -s; dz = c; }   // forward (W)
        }
        Vec3 current = player.getDeltaMovement();
        player.setDeltaMovement(dx * DASH_SPEED, current.y, dz * DASH_SPEED);
        // Force the server to sync the new motion to the client immediately.
        player.hurtMarked = true;
    }

    /** “持有时”按物品栏整体判定（主手、副手或背包中任意一格）。 */
    private static boolean hasMedallionInHand(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (AbilityItems.hasMedallionAbility(stack, GeneratedMod.ASSASSIN_MEDALLION.get())) {
                return true;
            }
        }
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        return AbilityItems.hasMedallionAbility(main, GeneratedMod.ASSASSIN_MEDALLION.get())
                || AbilityItems.hasMedallionAbility(off, GeneratedMod.ASSASSIN_MEDALLION.get());
    }
}
