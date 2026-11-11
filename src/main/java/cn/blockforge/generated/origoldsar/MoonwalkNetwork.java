package cn.blockforge.generated.origoldsar;

import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * 处理月亮行者勋章的三类重力请求：反转重力、月球重力 与 双倍重力。
 *
 * <p>实现思路：不改任何移动逻辑，直接通过 {@code Attributes.GRAVITY} 属性修饰符改变重力。
 * <ul>
 *   <li><b>反转重力</b>：翻转 GRAVITY 属性的符号，让玩家“向上掉落”。原版
 *       {@code Entity.applyGravity()} 每 tick 执行 {@code setDeltaMovement(delta.add(0, -getGravity(), 0))}，
 *       属性为正时向下拉、为负时向上拉。给 GRAVITY 加一个使总和变负的瞬时修饰符即可。</li>
 *   <li><b>月球重力</b>：把 GRAVITY 属性整体缩放到原来的 25%（加一个
 *       {@code ADD_MULTIPLIED_TOTAL = -0.75} 的瞬时修饰符），配合
 *       {@code LunarGravityMixin} 把垂直空气阻力调整到让最大终端下落速度为原来的 40%。</li>
 *   <li><b>双倍重力</b>：玩家在空中按住潜行时，把 {@code getGravity()} 的返回值临时强制为
 *       常态重力的两倍（约 0.16），由 {@code DoubleGravityMixin} 实现；松开潜行或落地即恢复原值。</li>
 * </ul>
 * 属性变化会被服务端自动同步给客户端（{@code ClientboundUpdateAttributesPacket}），
 * 因此客户端的重力预测与服务端一致，不会来回弹跳。移除修饰符即恢复。
 * 在成功更改重力（无论反转还是月球）时会播放一个重力更改音效。
 */
public final class MoonwalkNetwork {
    private static final Identifier GRAVITY_MODIFIER_ID =
            Identifier.fromNamespaceAndPath(GeneratedMod.MOD_ID, "moonwalk_gravity_invert");
    /** 默认重力约为 0.08；此修饰符使其变为 -0.08，等强度、方向相反。 */
    private static final double INVERT_AMOUNT = -0.16D;
    private static final Identifier LUNAR_GRAVITY_MODIFIER_ID =
            Identifier.fromNamespaceAndPath(GeneratedMod.MOD_ID, "moonwalk_lunar_gravity");
    /** 把重力缩放为原来的 25%（0.08 → 0.02）。 */
    private static final double LUNAR_GRAVITY_MULTIPLIER = -0.75D;

    /**
     * 原版终端下落速度：0.98 × 0.08 / (1 - 0.98) ≈ 3.92 格/tick。
     * 即垂直空气阻力系数 0.98F 与重力 0.08 平衡时的“最大下落速度”。
     */
    public static final double VANILLA_TERMINAL_FALL = 0.98D * 0.08D / 0.02D;
    /** 月亮行者模式的目标终端下落速度 = 原版的 40%。 */
    public static final double LUNAR_TERMINAL_FALL = 0.40D * VANILLA_TERMINAL_FALL;
    /** 超重力（空中按潜行、双倍常态重力）下的终端下落速度 = 原版的 2 倍（约 7.84 格/tick）。 */
    public static final double SUPERGRAVITY_TERMINAL_FALL = 2.0D * VANILLA_TERMINAL_FALL;

    private MoonwalkNetwork() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(
                MoonwalkTogglePayload.TYPE,
                MoonwalkTogglePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    Player player = context.player();
                    if (player == null || !hasMoonwalker(player)) {
                        return;
                    }
                    toggleGravity(player);
                }));
        event.registrar("1").playToServer(
                MoonwalkLunarPayload.TYPE,
                MoonwalkLunarPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    Player player = context.player();
                    if (player == null || !hasMoonwalker(player)) {
                        return;
                    }
                    toggleLunarGravity(player);
                }));
    }

    private static void toggleGravity(Player player) {
        AttributeInstance gravity = player.getAttribute(Attributes.GRAVITY);
        if (gravity == null) {
            return;
        }
        if (gravity.hasModifier(GRAVITY_MODIFIER_ID)) {
            gravity.removeModifier(GRAVITY_MODIFIER_ID);
            player.playSound(SoundEvents.CHORUS_FRUIT_TELEPORT, 1.0F, 1.25F);
        } else {
            gravity.addTransientModifier(new AttributeModifier(
                    GRAVITY_MODIFIER_ID, INVERT_AMOUNT, AttributeModifier.Operation.ADD_VALUE));
            player.playSound(SoundEvents.CHORUS_FRUIT_TELEPORT, 1.0F, 0.75F);
        }
    }

    private static void toggleLunarGravity(Player player) {
        AttributeInstance gravity = player.getAttribute(Attributes.GRAVITY);
        if (gravity == null) {
            return;
        }
        if (gravity.hasModifier(LUNAR_GRAVITY_MODIFIER_ID)) {
            gravity.removeModifier(LUNAR_GRAVITY_MODIFIER_ID);
            player.playSound(SoundEvents.CHORUS_FRUIT_TELEPORT, 1.0F, 0.9F);
        } else {
            gravity.addTransientModifier(new AttributeModifier(
                    LUNAR_GRAVITY_MODIFIER_ID, LUNAR_GRAVITY_MULTIPLIER, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            player.playSound(SoundEvents.CHORUS_FRUIT_TELEPORT, 1.0F, 1.5F);
        }
    }

    /** 月球重力是否激活：以是否持有月球重力修饰符为准。 */
    public static boolean hasLunarGravity(Player player) {
        AttributeInstance gravity = player.getAttribute(Attributes.GRAVITY);
        return gravity != null && gravity.hasModifier(LUNAR_GRAVITY_MODIFIER_ID);
    }

    /**
     * 双倍重力是否激活：玩家在空中（不在地面）且正按住潜行键，并且持有月亮行者勋章。
     * 该判定在客户端与服务端基于同样的玩家状态（{@code onGround()}、{@code isShiftKeyDown()}）
     * 各自独立计算，因此双方的 {@code getGravity()} 会得到一致的值，无需网络同步。
     */
    public static boolean isDoubleGravityActive(Player player) {
        return !player.onGround() && player.isShiftKeyDown() && hasMoonwalker(player);
    }

    /**
     * 当前模式下的“最大下落速度”（下落速度上限，仅在超重力结束时用于重置判定）：
     * <ul>
     *   <li>已开启月球重力：{@link #LUNAR_TERMINAL_FALL}（原版的 40%）。</li>
     *   <li>未开启：{@link #VANILLA_TERMINAL_FALL}（原版终端下落速度）。</li>
     * </ul>
     * 超重力（空中按住潜行、双倍重力）期间不下落到这个上限，而是按
     * {@link #SUPERGRAVITY_TERMINAL_FALL}（原版的两倍）；当超重力结束时若当前下落速度
     * 仍高于本方法返回的上限，则立刻把它重置回该上限。月球重力与普通状态下终端速度
     * 本就等于该值，不干预也不会突破。
     */
    public static double fallSpeedCap(Player player) {
        return hasLunarGravity(player) ? LUNAR_TERMINAL_FALL : VANILLA_TERMINAL_FALL;
    }

    /**
     * 常态重力的两倍：基于 GRAVITY 属性“基础值”计算（原版为基础值 0.08）。
     * 刻意不用 {@code getAttributeValue(...)}，因为月球/反转重力通过修饰符改变了“当前值”，
     * 而“常态的两倍”应当始终是 2 × 基础值（约 0.16），不受这些修饰符影响。
     * 玩家没有 GRAVITY 属性时退回 0.16。
     */
    public static double doubleGravityValue(Player player) {
        AttributeInstance gravity = player.getAttribute(Attributes.GRAVITY);
        if (gravity == null) {
            return 0.16D;
        }
        return 2.0D * gravity.getBaseValue();
    }

    /**
     * 兜底（每 tick 调用）：玩家当前处于反转/月球重力状态但已不再持有任何提供该能力的物品时，
     * 自动恢复重力。没有对应修饰符的玩家直接跳过，开销极小。
     */
    public static void clearIfNotHeld(Player player) {
        AttributeInstance gravity = player.getAttribute(Attributes.GRAVITY);
        if (gravity == null || hasMoonwalker(player)) {
            return;
        }
        if (gravity.hasModifier(GRAVITY_MODIFIER_ID)) {
            gravity.removeModifier(GRAVITY_MODIFIER_ID);
        }
        if (gravity.hasModifier(LUNAR_GRAVITY_MODIFIER_ID)) {
            gravity.removeModifier(LUNAR_GRAVITY_MODIFIER_ID);
        }
    }

    /** “持有时”按物品栏整体判定：月亮行者勋章本体、暗影勋章、永夜擢升之证均视为持有。 */
    public static boolean hasMoonwalker(Player player) {
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
