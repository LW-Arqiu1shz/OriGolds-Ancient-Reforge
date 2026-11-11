package cn.blockforge.generated.origoldsar;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

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

    public record Instance(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player")
                        .forGetter(Instance::player)
        ).apply(instance, Instance::new));
    }
}
