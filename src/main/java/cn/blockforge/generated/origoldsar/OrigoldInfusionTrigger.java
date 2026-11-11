package cn.blockforge.generated.origoldsar;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * “在金源装配站上为破损金源部件赋源”进度触发器。
 * 玩家在装配站菜单点击特性按钮完成赋源时由服务端触发。
 */
public final class OrigoldInfusionTrigger extends SimpleCriterionTrigger<OrigoldInfusionTrigger.Instance> {
    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public void trigger(ServerPlayer player) {
        this.trigger(player, instance -> true);
    }

    // 26.3：SimpleInstance.player() 改为 Optional<Holder<LootItemCondition>>，
    // 原来的 ContextAwarePredicate / EntityPredicate.ADVANCEMENT_CODEC 已删除。
    public record Instance(Optional<Holder<LootItemCondition>> player) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                LootItemCondition.CODEC.optionalFieldOf("player")
                        .forGetter(Instance::player)
        ).apply(inst, Instance::new));
    }
}
