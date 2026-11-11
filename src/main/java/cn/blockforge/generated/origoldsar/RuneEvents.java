package cn.blockforge.generated.origoldsar;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;


/** 四枚符文（迅影 / 贪婪 / 收割 / 虹吸）的效果逻辑。 */
@EventBusSubscriber(modid = GeneratedMod.MOD_ID)
public final class RuneEvents {
    private RuneEvents() {}

    /** 迅影符文的移动速度加成 id（基础速度 ×1.15）。 */
    private static final Identifier SWIFT_SPEED_ID =
            Identifier.fromNamespaceAndPath(GeneratedMod.MOD_ID, "swiftness_rune_speed");
    /** 迅影符文的移动效率加成 id：把通用移动效率提到 1.0，抵消灵魂沙等方块速度因子的减速。 */
    private static final Identifier SWIFT_EFFICIENCY_ID =
            Identifier.fromNamespaceAndPath(GeneratedMod.MOD_ID, "swiftness_rune_efficiency");
    /** 虹吸符文的内置冷却：0.45 秒 = 9 tick。 */
    private static final int SIPHON_COOLDOWN_TICKS = 9;
    private static final String SIPHON_LAST_HIT_KEY = "origolds_ar_siphon_last_hit";

    /** 符文是否在玩家背包中（含主手/副手）。野兽符文与永夜擢升之证视为持有所有符文。 */
    private static boolean hasRuneInInventory(Player player, Item rune) {
        return AbilityItems.hasRuneInInventory(player, rune);
    }

    // ------------------------------------------------------------------
    // 迅影符文：持有时基础移动速度 ×1.15，并免疫方块速度因子（灵魂沙等）与
    // 使用物品（举盾 / 拉弓 / 吃喝）带来的减速。方块因子通过移动效率属性抵消
    // （服务端加属性会自动同步到客户端），使用物品的减速由客户端事件补偿。
    // ------------------------------------------------------------------
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator()) {
            return;
        }
        boolean shouldApply = hasRuneInInventory(player, GeneratedMod.SWIFTNESS_RUNE.get());

        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            boolean applied = speed.getModifier(SWIFT_SPEED_ID) != null;
            if (shouldApply && !applied) {
                speed.addTransientModifier(new AttributeModifier(
                        SWIFT_SPEED_ID, 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            } else if (!shouldApply && applied) {
                speed.removeModifier(SWIFT_SPEED_ID);
            }
        }

        // 移动效率加到 1.0：getBlockSpeedFactor() = lerp(移动效率, 方块速度因子, 1.0)，
        // 移动效率为 1.0 时恒等于 1.0，从而抵消灵魂沙（0.4）等方块的速度因子减速。
        AttributeInstance efficiency = player.getAttribute(Attributes.MOVEMENT_EFFICIENCY);
        if (efficiency != null) {
            boolean appliedEff = efficiency.getModifier(SWIFT_EFFICIENCY_ID) != null;
            if (shouldApply && !appliedEff) {
                efficiency.addTransientModifier(new AttributeModifier(
                        SWIFT_EFFICIENCY_ID, 1.0, AttributeModifier.Operation.ADD_VALUE));
            } else if (!shouldApply && appliedEff) {
                efficiency.removeModifier(SWIFT_EFFICIENCY_ID);
            }
        }
    }

    // ------------------------------------------------------------------
    // 收割符文：对生命值低于 75% 的目标伤害 +12.5%，低于 50% 再 +12.5%
    // ------------------------------------------------------------------
    @SubscribeEvent
    public static void onHarvestDamage(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!hasRuneInInventory(player, GeneratedMod.REAPING_RUNE.get())) {
            return;
        }
        LivingEntity target = event.getEntity();
        float maxHealth = target.getMaxHealth();
        if (maxHealth <= 0.0F) {
            return;
        }
        float healthFraction = target.getHealth() / maxHealth;
        float multiplier;
        if (healthFraction < 0.5F) {
            multiplier = 1.25F;
        } else if (healthFraction < 0.75F) {
            multiplier = 1.125F;
        } else {
            return;
        }
        event.setNewDamage(event.getNewDamage() * multiplier);
    }

    // ------------------------------------------------------------------
    // 虹吸符文：攻击命中恢复生命上限 5%（暴击 7.5%），每 0.45 秒至多一次
    // ------------------------------------------------------------------
    @SubscribeEvent
    public static void onSiphonHit(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        // 只对玩家自己的直接攻击生效，且本次伤害确实造成了伤害
        if (player != event.getSource().getDirectEntity() || event.getHealthDamage() <= 0.0F) {
            return;
        }
        if (!hasRuneInInventory(player, GeneratedMod.SIPHON_RUNE.get())) {
            return;
        }
        long gameTime = player.level().getGameTime();
        long lastHit = player.getPersistentData().getLongOr(SIPHON_LAST_HIT_KEY, 0L);
        if (gameTime - lastHit < SIPHON_COOLDOWN_TICKS) {
            return;
        }
        float fraction = isCriticalHit(player) ? 0.075F : 0.05F;
        player.heal(player.getMaxHealth() * fraction);
        player.getPersistentData().putLong(SIPHON_LAST_HIT_KEY, gameTime);
    }

    /** 近似原版的暴击判定条件（下落攻击且不处于禁用暴击的状态）。 */
    private static boolean isCriticalHit(Player player) {
        return player.fallDistance > 0.0F
                && !player.onGround()
                && !player.onClimbable()
                && !player.isInWater()
                && !player.hasEffect(MobEffects.BLINDNESS)
                && !player.isPassenger()
                && !player.isSprinting();
    }

    // ------------------------------------------------------------------
    // 贪婪符文：经验翻倍 + 稀有掉落率 ×10 + 常见掉落“掉落后再判定一次”（期望翻倍）
    // ------------------------------------------------------------------
    @SubscribeEvent
    public static void onGreedExperience(LivingExperienceDropEvent event) {
        Player attacker = event.getAttackingPlayer();
        if (attacker == null || !hasRuneInInventory(attacker, GeneratedMod.GREED_RUNE.get())) {
            return;
        }
        event.setDroppedExperience(event.getDroppedExperience() * 2);
    }

    @SubscribeEvent
    public static void onGreedDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide()
                || !(event.getSource().getEntity() instanceof ServerPlayer killer)) {
            return;
        }
        if (!hasRuneInInventory(killer, GeneratedMod.GREED_RUNE.get())) {
            return;
        }
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }
        int looting = EnchantmentHelper.getEnchantmentLevel(
                killer.level().registryAccess().holderOrThrow(Enchantments.LOOTING), killer);
        // 凋零骷髅头：特殊掉落（不在普通战利品表里），命中概率变为原来的 10 倍
        if (mob.getType() == EntityTypes.WITHER_SKELETON) {
            double vanillaChance = 0.025 + 0.01 * looting;
            if (mob.getRandom().nextDouble() < vanillaChance * 10.0) {
                spawnDrop(mob, new ItemStack(Items.WITHER_SKELETON_SKULL));
            }
        }
        dropGreedBonusLoot(mob, level, looting);
    }

    /**
     * 读取生物战利品表 JSON，对每个掉落池再补掷一次：
     * - 稀有池（条件概率 ≤ 0.2）：命中概率改为 10 倍，效果和凋零骷髅头一致；
     * - 常见池（概率 &gt; 0.2 或没有概率条件）：按原概率再判定一次并掉落，
     *   叠加原版本身的第一次掉落，即“掉落后再判定一次”，常见掉落期望翻倍。
     * 可叠加抢夺附魔。
     */
    private static void dropGreedBonusLoot(Mob mob, ServerLevel level, int looting) {
        try {
            ResourceKey<LootTable> tableKey = mob.getLootTable().orElse(null);
            if (tableKey == null) {
                return;
            }
            // 从服务端权威的战利品数据取表，再序列化回 JSON 逐池补掷。
            // 原版战利品表就存在这里，避免依赖资源文件路径解析而静默失败。
            LootTable table = level.getServer().reloadableRegistries().getLootTable(tableKey);
            if (table == null || table == LootTable.EMPTY) {
                return;
            }
            JsonArray pools = cachedPools(table, level.getServer().registryAccess());
            if (pools == null) {
                return;
            }
            for (JsonElement poolElement : pools) {
                JsonObject pool = poolElement.getAsJsonObject();
                double chance = greedPoolChance(pool, looting);
                if (mob.getRandom().nextDouble() >= chance) {
                    continue;
                }
                ItemStack rolled = rollPoolItem(pool, looting, mob);
                if (rolled != null && !rolled.isEmpty()) {
                    spawnSplit(mob, rolled);
                }
            }
        } catch (Exception ignored) {
            // 战利品表结构不符合预期时静默跳过，不影响正常掉落
        }
    }

    /**
     * 每张战利品表只序列化一次（以前每杀一只怪都把整张表编码成 JSON）。
     * 键是 LootTable 对象本身、弱引用：数据包 /reload 会生成新的表对象，旧缓存自然失效，
     * 不会拿着旧表补掷。值里不引用表对象，不会阻止回收。
     */
    private static final java.util.Map<LootTable, java.util.Optional<JsonArray>> POOL_CACHE =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    private static JsonArray cachedPools(LootTable table, net.minecraft.core.HolderLookup.Provider registries) {
        java.util.Optional<JsonArray> cached = POOL_CACHE.get(table);
        if (cached == null) {
            JsonElement encoded = encodeLootTable(table, registries);
            JsonArray pools = encoded != null && encoded.isJsonObject()
                    && encoded.getAsJsonObject().get("pools") instanceof JsonArray array ? array : null;
            cached = java.util.Optional.ofNullable(pools);
            POOL_CACHE.put(table, cached);
        }
        return cached.orElse(null);
    }

    /**
     * 补掷数量可能超过一组（数据包里的大数量、高等级抢夺）。超过上限的单个物品堆
     * 存盘时编码会失败，掉落物实体保存不了，所以按最大堆叠拆成多组再掉，并设个总量上限防刷屏。
     */
    private static void spawnSplit(Mob mob, ItemStack stack) {
        int max = Math.max(1, stack.getMaxStackSize());
        int remaining = Math.min(stack.getCount(), max * 16);
        while (remaining > 0) {
            int count = Math.min(max, remaining);
            spawnDrop(mob, stack.copyWithCount(count));
            remaining -= count;
        }
    }

    /** 把权威 LootTable 序列化回 JSON（保留 pools/functions/conditions），失败时返回 null。
     *  用带注册表的 RegistryOps 编码，确保含附魔/属性等注册表引用的战利品函数也能还原。 */
    private static JsonElement encodeLootTable(LootTable table, net.minecraft.core.HolderLookup.Provider registries) {
        try {
            @SuppressWarnings("unchecked")
            com.mojang.serialization.Codec<LootTable> codec = (com.mojang.serialization.Codec<LootTable>) LootTable.DIRECT_CODEC;
            com.mojang.serialization.DataResult<JsonElement> result = codec.encodeStart(
                    net.minecraft.resources.RegistryOps.create(JsonOps.INSTANCE, registries), table);
            return result.result().orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 计算本次贪婪补掷中该池允许命中的概率。
     * - 没有任何概率条件（无条件或仅 killed_by_player 等）→ 1.0，视为必中并补掷一次，
     *   使该池最常见的掉落与第一次掉落叠加，实现“掉落后再判定一次”；
     * - 概率 ≤ 0.2 → 认定为稀有池，概率 ×10；
     * - 概率 &gt; 0.2 → 常见池，保持原概率补掷一次。
     */
    private static double greedPoolChance(JsonObject pool, int looting) {
        JsonArray conditions = pool.getAsJsonArray("conditions");
        if (conditions == null || conditions.isEmpty()) {
            return 1.0;
        }
        Double chance = poolChance(conditions, looting);
        if (chance == null) {
            // 只有非概率条件（如 killed_by_player），视为该池必中
            return 1.0;
        }
        if (chance <= 0.2) {
            return Math.min(1.0, chance * 10.0);
        }
        return chance;
    }

    /**
     * 从条件数组中提取控制概率的条件（random_chance / random_chance_with_looting /
     * random_chance_with_enchanted_bonus），返回考虑抢夺后的基础概率。
     * 返回 null 表示该池没有概率条件（非概率条件如 killed_by_player 照常允许）。
     */
    private static Double poolChance(JsonArray conditions, int looting) {
        Double chance = null;
        for (JsonElement conditionElement : conditions) {
            JsonObject condition = conditionElement.getAsJsonObject();
            String type = condition.has("condition") ? condition.get("condition").getAsString() : "";
            switch (type) {
                case "minecraft:random_chance" -> {
                    if (condition.get("chance") == null) {
                        return null;
                    }
                    chance = condition.get("chance").getAsDouble();
                }
                case "minecraft:random_chance_with_looting" -> {
                    if (condition.get("chance") == null || condition.get("looting_chance_multiplier") == null) {
                        return null;
                    }
                    double value = condition.get("chance").getAsDouble();
                    chance = Math.min(1.0, value
                            + condition.get("looting_chance_multiplier").getAsDouble() * looting);
                }
                case "minecraft:random_chance_with_enchanted_bonus" -> {
                    // 1.21.1 各大生物稀有掉落池的标准条件：无抢夺时按 unenchanted_chance，
                    // 有抢夺时按 enchanted_chance（线性/常量 LevelBasedValue）取该抢夺等级下的概率。
                    if (condition.get("unenchanted_chance") == null) {
                        return null;
                    }
                    double unenchanted = condition.get("unenchanted_chance").getAsDouble();
                    if (looting <= 0) {
                        chance = unenchanted;
                        break;
                    }
                    Double value = evalEnchantedChance(condition.get("enchanted_chance"), looting);
                    chance = value != null ? value : unenchanted;
                }
                default -> {
                    // killed_by_player / weather_check / entity_properties 等非概率条件照常允许
                }
            }
        }
        return chance;
    }

    /** 解析 random_chance_with_enchanted_bonus 的 enchanted_chance（LevelBasedValue），返回给定抢夺等级下的概率。 */
    private static Double evalEnchantedChance(JsonElement enchantedJson, int looting) {
        if (enchantedJson == null) {
            return null;
        }
        try {
            LevelBasedValue value = LevelBasedValue.CODEC.parse(JsonOps.INSTANCE, enchantedJson).result().orElse(null);
            if (value == null) {
                return null;
            }
            return (double) value.calculate(looting);
        } catch (Exception ignored) {
            return null;
        }
    }

    /** 从掉落池的物品条目里掷出一个物品堆（支持 set_count / looting_enchant 的数量修正）。 */
    private static ItemStack rollPoolItem(JsonObject pool, int looting, Mob mob) {
        JsonArray entries = pool.getAsJsonArray("entries");
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        java.util.ArrayList<ItemStack> candidates = new java.util.ArrayList<>();
        for (JsonElement entryElement : entries) {
            JsonObject entry = entryElement.getAsJsonObject();
            if (!entry.has("type") || !"minecraft:item".equals(entry.get("type").getAsString())) {
                continue;
            }
            if (!entry.has("name")) {
                continue;
            }
            Identifier itemId = Identifier.tryParse(entry.get("name").getAsString());
            if (itemId == null) {
                continue;
            }
            Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(itemId)
                    .map(net.minecraft.core.Holder::value).orElse(Items.AIR);
            if (item == Items.AIR) {
                continue;
            }
            int count = rollEntryCount(entry, looting, mob);
            if (count <= 0) {
                continue;
            }
            candidates.add(new ItemStack(item, count));
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(mob.getRandom().nextInt(candidates.size()));
    }

    /** 解析条目的数量：支持 set_count 与 looting_enchant（含均匀分布）。 */
    private static int rollEntryCount(JsonObject entry, int looting, Mob mob) {
        int count = 1;
        if (!entry.has("functions")) {
            return count;
        }
        JsonArray functions = entry.getAsJsonArray("functions");
        for (JsonElement functionElement : functions) {
            JsonObject function = functionElement.getAsJsonObject();
            String type = function.has("function") ? function.get("function").getAsString() : "";
            if ("minecraft:set_count".equals(type) && function.has("count")) {
                int value = rollNumberDistribution(function.get("count"), mob);
                if (value >= 0) {
                    count = value;
                }
            } else if ("minecraft:looting_enchant".equals(type) && function.has("count")) {
                int value = rollNumberDistribution(function.get("count"), mob);
                if (value >= 0) {
                    count = value;
                }
                if (function.has("each")) {
                    int each = rollNumberDistribution(function.get("each"), mob);
                    count += each * Math.max(0, looting);
                }
            }
        }
        return count;
    }

    /** 数量字段可以是常量或 {type:"uniform",min,max} 分布。 */
    private static int rollNumberDistribution(JsonElement element, Mob mob) {
        if (element.isJsonPrimitive()) {
            return element.getAsInt();
        }
        if (element.isJsonObject()) {
            JsonObject distribution = element.getAsJsonObject();
            String type = distribution.has("type") ? distribution.get("type").getAsString() : "";
            if ("minecraft:uniform".equals(type)
                    && distribution.has("min") && distribution.has("max")) {
                int min = distribution.get("min").getAsInt();
                int max = distribution.get("max").getAsInt();
                if (max < min) {
                    int temp = min;
                    min = max;
                    max = temp;
                }
                return min + mob.getRandom().nextInt(max - min + 1);
            }
            if ("minecraft:constant".equals(type) && distribution.has("value")) {
                return distribution.get("value").getAsInt();
            }
        }
        return -1;
    }

    private static void spawnDrop(Mob mob, ItemStack stack) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 pos = mob.position();
        net.minecraft.world.entity.item.ItemEntity drop =
                new net.minecraft.world.entity.item.ItemEntity(level, pos.x, pos.y + 0.5, pos.z, stack);
        drop.setPickUpDelay(10);
        level.addFreshEntity(drop);
        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F,
                ((mob.getRandom().nextFloat() - mob.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
    }
}
