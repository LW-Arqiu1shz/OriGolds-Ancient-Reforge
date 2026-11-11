package cn.blockforge.generated.origoldsar.mixin.client;

import cn.blockforge.generated.origoldsar.ArModListNames;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.neoforged.neoforge.client.gui.ModListScreen;
import net.neoforged.neoforgespi.language.IModInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 模组列表界面：左栏宽度计算、右侧详情标题用翻译名；搜索框同时匹配翻译名与原名。 */
@Mixin(ModListScreen.class)
public abstract class ModListScreenMixin {
    @WrapOperation(method = {"init", "updateCache"},
            at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforgespi/language/IModInfo;getDisplayName()Ljava/lang/String;"))
    private String origoldsAr$translatedName(IModInfo info, Operation<String> original) {
        return ArModListNames.displayName(info, original.call(info));
    }

    /**
     * 搜索过滤用的 lambda（名字由编译器生成，已对照本版 NeoForge 字节码确认）。
     * require = 0：以后 NeoForge 换了 lambda 编号也只会少掉“双名搜索”，绝不会让模组列表崩溃。
     */
    @WrapOperation(method = "lambda$reloadMods$0", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforgespi/language/IModInfo;getDisplayName()Ljava/lang/String;"))
    private String origoldsAr$searchBothNames(IModInfo info, Operation<String> original) {
        return ArModListNames.searchName(info, original.call(info));
    }
}
