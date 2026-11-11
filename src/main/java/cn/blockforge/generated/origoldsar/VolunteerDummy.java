package cn.blockforge.generated.origoldsar;

import java.util.ArrayDeque;
import java.util.Deque;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 测伤志愿者（Volunteer Dummy）：一只关闭了 AI 的村民，站立不动、不开交易，
 * 只用来挨打测伤害。
 *
 * <p>与普通村民的差异仅在“测伤”用途上：
 * <ul>
 *   <li>拥有极高的生命值（默认 10 亿，配合 AttributeFix 才能真正突破原版的 1024 上限）；</li>
 *   <li>完整地“受伤”——{@link #hurt} 会照常调用 {@code super.hurt}，
 *       因此护甲、抗性、附魔、暴击以及其它模组挂在伤害事件上的加成都会正常结算；
 *       结算完成后按“实际掉的血”统计伤害，并立刻把血量回满，所以永远打不死；</li>
 *   <li>受到伤害时，在头顶显示两行字：第一行是过去 3 秒的 DPS，第二行是上一次实际受到的伤害；
 *       停止受伤 5 秒后隐藏；</li>
 *   <li>潜行 + 右键会拆掉它：返还 1 个该物品，并把身上所有装备（含村民随身物品）
 *       掉落到地上。</li>
 * </ul>
 *
 * <p>穿戴盔甲沿用原版村民的方式：用发射器朝它发射盔甲即可穿上（{@code ArmorItem.dispenseArmor}），
 * 头盔还会像普通村民一样渲染出来。装备只影响防御数值，不会因为挨打掉耐久。
 *
 * <p>它虽然继承了村民（模型与音效完全一致），但刻意不“算作”一只村民：
 * <ul>
 *   <li>{@link #setLastHurtByMob} 为空：受击不再触发 {@code VILLAGER_HURT} 声望事件，
 *       玩家打它不会掉声望，因此也不会因为声望变低而招来铁傀儡；</li>
 *   <li>{@link #die} 改为直接移除：就算被 {@code /kill} 之类强行击杀，也不会走
 *       {@code tellWitnessesThatIWasMurdered} 触发 {@code VILLAGER_KILLED} 声望惩罚；</li>
 *   <li>{@link #addParticlesAroundSelf} 为空：受击时不再喷出村民的“愤怒”等粒子，
 *       避免这些粒子糊住视线，影响看伤害读数。</li>
 * </ul>
 */
public class VolunteerDummy extends Villager {
    /**
     * 最大生命值：10 亿。原版 {@code MAX_HEALTH} 属性区间是 [1, 1024]，
     * 装了 AttributeFix 之类的模组后才能真正生效；没装则会被原版钳到 1024。
     */
    public static final float MAX_HEALTH = 1_000_000_000.0F;
    /** 每 0.5 秒（10 刻）重置生命值，作为兜底。 */
    private static final int RESET_INTERVAL_TICKS = 10;
    /** DPS 统计窗口：过去 3 秒（60 刻）。 */
    private static final int DPS_WINDOW_TICKS = 60;
    /** 停止受伤后仍显示提示的时间：5 秒（100 刻）。 */
    private static final int DISPLAY_TICKS = 100;
    /** 1.2.0 起：同一次攻击常被拆成多笔伤害（如穿刺/多重射击一箭多段），间隔在此刻数内的连击合并显示为一次。 */
    private static final int HIT_MERGE_TICKS = 3;

    private static final EntityDataAccessor<Boolean> DATA_ACTIVE =
            SynchedEntityData.defineId(VolunteerDummy.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_LAST_DAMAGE =
            SynchedEntityData.defineId(VolunteerDummy.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DPS =
            SynchedEntityData.defineId(VolunteerDummy.class, EntityDataSerializers.FLOAT);

    /** 伤害事件历史，每个元素为 {@code [游戏刻, 伤害值]}。 */
    private final Deque<double[]> damageHistory = new ArrayDeque<>();
    /** 当前合并段（连击窗口内）已累计的单次伤害。 */
    private float currentHitDamage = 0.0F;
    private long lastHitGameTime = -1L;
    private long lastHealthReset = -1L;
    /** hurtServer 重入深度：同一次攻击嵌套触发时只在最外层统计伤害。 */
    private int hurtNesting = 0;

    public VolunteerDummy(EntityType<? extends VolunteerDummy> type, Level level) {
        super(type, level);
        // 没有 AI：村民的大脑与寻路完全停摆，它只是个静止靶子。
        this.setNoAi(true);
        // 不被自然清除，方便长期摆着测伤害。
        this.setPersistenceRequired();
    }

    /** 注册本实体属性：在村民属性基础上把最大生命值尽可能拉高。 */
    public static AttributeSupplier.Builder createAttributes() {
        return Villager.createAttributes().add(Attributes.MAX_HEALTH, MAX_HEALTH);
    }

    /**
     * 强制让身体与头部朝向都跟随 {@code yRot}。
     *
     * <p>普通生物靠 {@code BodyRotationControl}/{@code LookControl} 把 {@code yBodyRot}/{@code yHeadRot}
     * 慢慢转向 {@code getYRot()}；但靶子没有 AI（{@code setNoAi(true)}），这条路是断的，
     * 于是这两个字段会停在出生时的默认值 0（南方），从玩家角度看就“朝向随机”。
     *
     * <p>放置时 {@code moveTo} 已经按“面朝玩家最近的 8 个方向”算好了 {@code yRot}，
     * 这里每个 tick 让身体/头部直接对齐它，两端（服务端与客户端）都会指向正确方向。
     * 靶子不会转头，所以这个方向是稳定的。
     */
    @Override
    public void aiStep() {
        super.aiStep();
        float yRot = this.getYRot();
        this.setYBodyRot(yRot);
        this.setYHeadRot(yRot);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ACTIVE, false);
        builder.define(DATA_LAST_DAMAGE, 0.0F);
        builder.define(DATA_DPS, 0.0F);
    }

    /** 靶子不发出村民的环境音（“嗯嗯”声），免得干扰测伤害。 */
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        long now = this.level().getGameTime();
        if (this.lastHealthReset < 0L || now - this.lastHealthReset >= RESET_INTERVAL_TICKS) {
            this.lastHealthReset = now;
            restoreFullHealth();
        }
        pruneDamageHistory(now);
        if (this.entityData.get(DATA_ACTIVE)) {
            updateDps(now);
            if (now - this.lastHitGameTime > DISPLAY_TICKS) {
                this.entityData.set(DATA_ACTIVE, false);
            }
        }
    }

    /**
     * 测伤靶子的核心：必须真正“受伤”，否则附加在伤害管线上的东西都不会生效。
     *
     * <p>之前这里直接 {@code return false}，相当于告诉游戏“这一下没打中”，
     * 于是暴击、附魔、药水效果，以及其它模组通过 NeoForge 伤害事件追加的加成
     * （比如打出上万点的“跳劈”）全部被跳过，只剩武器基础攻击力，
     * 所以一万二的伤害也会被读成 13。
     *
     * <p>现在改为先清掉原版的受击无敌帧，再走完整的 {@code super.hurt} 伤害管线，
     * 然后按“实际掉的血 + 被打掉的伤害吸收”读出真正结算的伤害并统计，
     * 最后立刻把血量回满——它是靶子，永远不该被打死。
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.isRemoved() || this.isDeadOrDying()) {
            return false;
        }

        // 取消无敌帧和“上一击伤害”比较：连击、多支箭、快速平A都应逐次完整结算，
        // 否则 0.5 秒内的高频测试会被原版减伤逻辑吃掉。
        this.invulnerableTime = 0;
        this.lastHurt = 0.0F;

        float healthBefore = this.getHealth();
        float absorptionBefore = this.getAbsorptionAmount();

        // 1.2.0 起加防重入：一次攻击的伤害管线偶尔会嵌套回调 hurtServer
        // （如附魔/事件链），嵌套层只走伤害结算、不参与统计与回血，避免重复计数。
        boolean outermost = this.hurtNesting == 0;
        ++this.hurtNesting;
        boolean hurt;
        try {
            hurt = super.hurtServer(level, source, amount);
        } finally {
            --this.hurtNesting;
        }
        if (!outermost) {
            return hurt;
        }

        // 真正结算到身上的伤害：护甲、抗性、保护附魔等都已经在 super 里算过了。
        float dealt = (healthBefore - this.getHealth())
                + (absorptionBefore - this.getAbsorptionAmount());
        if (dealt > 0.0F) {
            registerDamage(dealt);
            // 受击反馈：红闪 + 受击音效（走原版伤害事件包）。
            this.level().broadcastDamageEvent(this, source);
        }

        // 打完立刻回满：既保证打不死，也不会因为连续挨打而掉血到危险区。
        restoreFullHealth();
        return hurt || dealt > 0.0F;
    }

    /** 任何外力都不推动靶子，保证它站在原地，测试条件稳定（也不会被“跳劈”击飞）。 */
    @Override
    public void push(double x, double y, double z) {
    }

    @Override
    public void push(Vec3 vec) {
    }

    @Override
    public void push(Entity entity) {
    }

    /** 靶子身上的装备只用来改变防御数值，不消耗耐久，方便反复测试。 */
    @Override
    protected void doHurtEquipment(DamageSource source, float amount, EquipmentSlot[] slots) {
    }

    /**
     * 覆写为空：原版村民在这里会触发 {@code VILLAGER_HURT} 声望事件（玩家打它扣声望、
     * 声望变低后招来铁傀儡），并广播字节 13 让客户端喷出“愤怒”粒子。
     * 靶子不承担这些后果，因此彻底不做任何事。
     */
    @Override
    public void setLastHurtByMob(LivingEntity source) {
    }

    /**
     * 覆写为直接移除：就算被 {@code /kill} 等强行击杀，也不触发
     * {@code tellWitnessesThatIWasMurdered}（{@code VILLAGER_KILLED}）声望惩罚。
     * 正常情况下它会一直被打不死，此方法只是兜底。
     */
    @Override
    public void die(DamageSource source) {
        this.discard();
    }

    /**
     * 覆写为空：不再喷出村民的“愤怒/爱心/高兴”等粒子（原版受击时会在身边炸开一圈，
     * 挡视线又干扰看伤害读数）。受击反馈只保留模型红闪与普通受击音效。
     */
    @Override
    public void handleEntityEvent(byte id) {
        // 26.1.2 起不再有 addParticlesAroundSelf；把村民的受击粒子事件（id 13）吞掉即可。
        if (id == 13) {
            return;
        }
        super.handleEntityEvent(id);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) {
            if (!this.level().isClientSide() && !this.isRemoved()) {
                dismantle(player);
            }
            return (this.level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER);
        }
        // 测伤志愿者不提供交易：普通村民会打开的界面在这里一律忽略。
        return InteractionResult.PASS;
    }

    private void registerDamage(float amount) {
        long now = this.level().getGameTime();
        // 1.2.0 起连击合并：同一次攻击被拆成多笔时（穿刺一箭多段、快速连击等），
        // 3 刻窗口内的伤害累加成"单次伤害"显示；DPS 历史仍逐笔记录，总量不受影响。
        this.currentHitDamage = this.lastHitGameTime >= 0L && now - this.lastHitGameTime <= HIT_MERGE_TICKS
                ? this.currentHitDamage + amount
                : amount;
        this.lastHitGameTime = now;
        this.damageHistory.addLast(new double[] {now, amount});
        pruneDamageHistory(now);
        this.entityData.set(DATA_ACTIVE, true);
        this.entityData.set(DATA_LAST_DAMAGE, this.currentHitDamage);
        updateDps(now);
    }

    private void pruneDamageHistory(long now) {
        while (!this.damageHistory.isEmpty()
                && now - this.damageHistory.peekFirst()[0] > DPS_WINDOW_TICKS) {
            this.damageHistory.removeFirst();
        }
    }

    private void updateDps(long now) {
        double sum = 0.0D;
        for (double[] event : this.damageHistory) {
            if (now - event[0] <= DPS_WINDOW_TICKS) {
                sum += event[1];
            }
        }
        this.entityData.set(DATA_DPS, (float) (sum / (DPS_WINDOW_TICKS / 20.0D)));
    }

    /** 把血量拉回最大值，靶子永远保持可继续测试的状态。 */
    private void restoreFullHealth() {
        float max = this.getMaxHealth();
        if (this.getHealth() < max) {
            this.setHealth(max);
        }
    }

    private void dismantle(Player player) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = this.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                this.spawnAtLocation(serverLevel, stack);
                this.setItemSlot(slot, ItemStack.EMPTY);
            }
        }
        // 村民随身携带的物品也一并掉出来。
        SimpleContainer inventory = this.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                this.spawnAtLocation(serverLevel, stack);
                inventory.setItem(i, ItemStack.EMPTY);
            }
        }
        ItemStack returned = new ItemStack(GeneratedMod.VOLUNTEER_DUMMY.get());
        player.getInventory().add(returned);
        if (!returned.isEmpty()) {
            this.spawnAtLocation(serverLevel, returned);
        }
        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ARMOR_STAND_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);
        this.discard();
    }

    /** 头顶提示是否应显示（受到伤害后 5 秒内）。 */
    public boolean isDisplayActive() {
        return this.entityData.get(DATA_ACTIVE);
    }

    /** 上一次实际受到的伤害。 */
    public float getLastDamage() {
        return this.entityData.get(DATA_LAST_DAMAGE);
    }

    /** 过去 3 秒的 DPS。 */
    public float getDps() {
        return this.entityData.get(DATA_DPS);
    }
}
