package cn.blockforge.generated.origoldsar;

import cn.blockforge.generated.origoldsar.client.AssemblyStationScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = GeneratedMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(GeneratedMod.ASSEMBLY_STATION_MENU.get(), AssemblyStationScreen::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ClientModEvents::registerGrapnelBallistaPredicates);
    }

    /**
     * 钩爪弩炮虽然是 {@link CrossbowItem} 的子类，但原版的 {@code pull / pulling / charged}
     * 物品属性是**按物品实例**注册的（原版只给 {@code Items.CROSSBOW} 注册过），
     * 所以必须给本模组的物品补上，否则模型 JSON 里的 overrides 永远不会命中，
     * 拉弦时也就不会切换到对应贴图。三个函数的判定逻辑与原版弩完全一致。
     */
    private static void registerGrapnelBallistaPredicates() {
        var ballista = GeneratedMod.GRAPNEL_BALLISTA.get();

        // 蓄力进度：0 为完全未拉，1 为蓄满。已装填时按 0 处理（交给 charged）。
        ItemProperties.register(ballista, ResourceLocation.withDefaultNamespace("pull"),
                (stack, level, entity, seed) -> {
                    if (entity == null) {
                        return 0.0F;
                    }
                    return CrossbowItem.isCharged(stack)
                            ? 0.0F
                            : (float) (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks())
                                    / (float) CrossbowItem.getChargeDuration(stack, entity);
                });

        // 是否正在拉这把弩（已装填时不再算“正在拉”）。
        ItemProperties.register(ballista, ResourceLocation.withDefaultNamespace("pulling"),
                (stack, level, entity, seed) -> entity != null
                        && entity.isUsingItem()
                        && entity.getUseItem() == stack
                        && !CrossbowItem.isCharged(stack)
                        ? 1.0F
                        : 0.0F);

        // 是否已经装填完毕（蓄力完成）。
        ItemProperties.register(ballista, ResourceLocation.withDefaultNamespace("charged"),
                (stack, level, entity, seed) -> CrossbowItem.isCharged(stack) ? 1.0F : 0.0F);
    }

    /**
     * 永恒药水直接复用原版药水的贴图（layer0 为药液遮罩，layer1 为瓶身），
     * 药液颜色跟随其封装药水的 potion_contents，与原版药水一致。
     */
    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        ItemColor potionTint = (stack, tintIndex) -> {
            if (tintIndex != 0) {
                return -1;
            }
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            return contents == null ? -1 : contents.getColor();
        };
        event.register(potionTint, GeneratedMod.ETERNAL_POTION.get());
        event.register(potionTint, GeneratedMod.ETERNAL_SPLASH_POTION.get());
        event.register(potionTint, GeneratedMod.ETERNAL_LINGERING_POTION.get());
    }
}
