package cn.blockforge.generated.origoldsar;

import cn.blockforge.generated.origoldsar.client.AssemblyStationScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * 客户端注册。
 *
 * <p>26.1.2 起“物品属性 / 状态切换”搬到了资源包的物品定义文件
 * （{@code assets/&lt;modid&gt;/items/*.json}），药水染色也由物品定义里的
 * {@code minecraft:potion} 染色源负责，因此这里不再有物品属性与着色注册。
 */
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(GeneratedMod.ASSEMBLY_STATION_MENU.get(), AssemblyStationScreen::new);
    }
}
