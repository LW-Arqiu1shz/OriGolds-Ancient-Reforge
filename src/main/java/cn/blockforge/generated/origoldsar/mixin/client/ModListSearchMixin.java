package cn.blockforge.generated.origoldsar.mixin.client;

import cn.blockforge.generated.origoldsar.client.ArModDisplayInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.gui.modlist.ModDisplayInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 26.3 模组列表的搜索过滤：对本模组同时匹配翻译名与原名（中文界面下搜英文名也能找到）。
 *
 * <p>目标是 {@code ModListScreen.updateModsList} 里过滤用的 lambda（名字由编译器生成，已对照
 * 26.3.0.51-beta 字节码确认）。require = 0：以后 NeoForge 换了 lambda 编号也只会少掉“双名搜索”，
 * 绝不会让模组列表崩溃。</p>
 */
@Mixin(targets = "net.neoforged.neoforge.client.gui.modlist.ModListScreen")
public abstract class ModListSearchMixin {
    @WrapOperation(method = "lambda$updateModsList$0", require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/client/gui/modlist/ModDisplayInfo;displayName()Lnet/minecraft/network/chat/Component;"))
    private static Component origoldsAr$searchBothNames(ModDisplayInfo info, Operation<Component> original) {
        return ArModDisplayInfo.searchName(info, original.call(info));
    }
}
