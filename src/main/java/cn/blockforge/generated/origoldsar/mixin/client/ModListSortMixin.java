package cn.blockforge.generated.origoldsar.mixin.client;

import cn.blockforge.generated.origoldsar.ArModListNames;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.neoforged.neoforgespi.language.IModInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 模组列表按名称排序时也用翻译名，排序结果与显示一致。 */
@Mixin(targets = "net.neoforged.neoforge.client.gui.ModListScreen$SortType")
public abstract class ModListSortMixin {
    @WrapOperation(method = "compare(Lnet/neoforged/fml/ModContainer;Lnet/neoforged/fml/ModContainer;)I",
            at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforgespi/language/IModInfo;getDisplayName()Ljava/lang/String;"))
    private String origoldsAr$translatedName(IModInfo info, Operation<String> original) {
        return ArModListNames.displayName(info, original.call(info));
    }
}
