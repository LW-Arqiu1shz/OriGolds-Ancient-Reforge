package cn.blockforge.generated.origoldsar.client;

import cn.blockforge.generated.origoldsar.GeneratedMod;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/**
 * 集中注册“金源工艺”类别下的三个按键，并据此确定它们在控制界面里的顺序：
 * 幽灵勋章闪避 → 刺客勋章冲刺 → 月亮行者勋章重力修改。
 *
 * <p>控制界面的选项排列顺序由 {@code RegisterKeyMappingsEvent.register(...)} 的调用顺序决定，
 * 而不同 {@code @EventBusSubscriber} 类的处理顺序并不保证稳定，因此把三个按键集中到一处
 * 按“闪避、冲刺、重力修改”的顺序注册。
 *
 * <p>26.1.2 起原版 {@link KeyMapping} 的第三个参数由 {@code String} 改为
 * {@link KeyMapping.Category}，自定义分类需要先通过
 * {@link RegisterKeyMappingsEvent#registerCategory(KeyMapping.Category)} 注册。
 */
public final class OrigoldKeyBindings {
    /** 本模组的按键分类；语言键由 {@code Category.label()} 生成为 {@code key.category.<namespace>.<path>}。 */
    public static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(GeneratedMod.MOD_ID, "main"));

    public static final KeyMapping DODGE =
            new KeyMapping("key.origolds_ar.dodge", GLFW.GLFW_KEY_R, CATEGORY);
    public static final KeyMapping DASH =
            new KeyMapping("key.origolds_ar.dash", GLFW.GLFW_KEY_SPACE, CATEGORY);
    public static final KeyMapping MOONWALK =
            new KeyMapping("key.origolds_ar.moonwalk", GLFW.GLFW_KEY_SPACE, CATEGORY);

    private OrigoldKeyBindings() {}

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(DODGE);
        event.register(DASH);
        event.register(MOONWALK);
    }
}
