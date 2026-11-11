package cn.blockforge.generated.origoldsar.mixin.client;

import cn.blockforge.generated.origoldsar.ArModListNames;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.neoforged.neoforgespi.language.IModInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 模组列表左栏的每一行（名称绘制与旁白）用翻译名。 */
@Mixin(targets = "net.neoforged.neoforge.client.gui.widget.ModListWidget$ModEntry")
public abstract class ModListEntryMixin {
    @WrapOperation(method = "*",
            at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforgespi/language/IModInfo;getDisplayName()Ljava/lang/String;"))
    private String origoldsAr$translatedName(IModInfo info, Operation<String> original) {
        return ArModListNames.displayName(info, original.call(info));
    }
}
