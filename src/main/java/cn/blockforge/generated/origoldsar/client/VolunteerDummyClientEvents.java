package cn.blockforge.generated.origoldsar.client;

import cn.blockforge.generated.origoldsar.GeneratedMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * 测伤志愿者的客户端渲染注册：把自定义渲染器挂到实体类型上。
 */
@EventBusSubscriber(modid = GeneratedMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class VolunteerDummyClientEvents {
    private VolunteerDummyClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(GeneratedMod.VOLUNTEER_DUMMY_TYPE.get(), VolunteerDummyRenderer::new);
        // 自循环附魔之瓶的投掷物直接复用原版投掷物渲染（按物品模型绘制），外观即附魔之瓶。
        event.registerEntityRenderer(GeneratedMod.SELF_CIRCULATING_BOTTLE_ENTITY.get(),
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
    }
}
