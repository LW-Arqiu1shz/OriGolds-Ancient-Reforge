package cn.blockforge.generated.origoldsar.client;

import cn.blockforge.generated.origoldsar.GeneratedMod;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.modlist.DefaultModDisplayInfo;

/**
 * 26.3 模组列表的双语适配。
 *
 * <p>26.3 的模组列表改成了 {@code ModDisplayInfo} 扩展点：名称默认是 neoforge.mods.toml 里写死的
 * displayName，简介按 {@code neoforge.screen.mods.info.description.<modid>} 翻译。这里注册一个只改名称的
 * 实现：当前语言有 {@value #NAME_KEY} 译文时显示翻译名，没有则原样显示。其余（图标、版本、作者、简介…）
 * 全部沿用 NeoForge 默认行为，不需要 Mixin。</p>
 *
 * <p>搜索框只匹配显示的名称，所以“中文界面也能搜英文名”另由 {@code ModListSearchMixin} 补上。</p>
 */
public final class ArModDisplayInfo extends DefaultModDisplayInfo {
    public static final String NAME_KEY = "fml.menu.mods.info.displayname." + GeneratedMod.MOD_ID;

    public ArModDisplayInfo(ModContainer container) {
        super(container);
    }

    @Override
    public Component displayName() {
        return Component.translatableWithFallback(NAME_KEY, originalName());
    }

    /** neoforge.mods.toml 里的原名（英文）。 */
    public String originalName() {
        return container().getModInfo().getDisplayName();
    }

    /** 搜索用：翻译名 + 原名（只用于包含匹配，不显示）。 */
    public static Component searchName(Object info, Component shown) {
        if (info instanceof ArModDisplayInfo ar) {
            String original = ar.originalName();
            String text = shown.getString();
            return Component.literal(text.equals(original) ? original : text + " " + original);
        }
        return shown;
    }
}
