package cn.blockforge.generated.origoldsar.client;

import cn.blockforge.generated.origoldsar.GeneratedMod;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * 客户端实体渲染注册：测伤志愿者的自定义渲染器，
 * 以及自循环附魔之瓶（1.2.0 新功能）的原版投掷物外观。
 */
public final class VolunteerDummyClientEvents {
    private VolunteerDummyClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(GeneratedMod.VOLUNTEER_DUMMY_TYPE.get(), VolunteerDummyRenderer::new);
        event.registerEntityRenderer(GeneratedMod.SELF_CIRCULATING_BOTTLE_ENTITY.get(), ThrownItemRenderer::new);
    }
}
