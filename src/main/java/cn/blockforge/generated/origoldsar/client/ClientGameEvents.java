package cn.blockforge.generated.origoldsar.client;

import cn.blockforge.generated.origoldsar.EternalPotionItem;
import cn.blockforge.generated.origoldsar.GeneratedMod;
import cn.blockforge.generated.origoldsar.TraitLibrary;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.lwjgl.glfw.GLFW;

import java.util.List;

@EventBusSubscriber(modid = GeneratedMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ClientGameEvents {
    private static final String HINT_PREFIX = "hint.origolds_ar.";

    private ClientGameEvents() {
    }

    @SubscribeEvent
    public static void addItemHints(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();
        if (stack.is(GeneratedMod.BROKEN_ORIGOLD_PARTS.get())) {            if (TraitLibrary.isInfused(stack)) {
                TraitLibrary.appendTooltip(stack, tooltip);
                appendHints(tooltip, "infused");
            } else {
                appendHints(tooltip, "broken");
            }
        } else if (stack.is(GeneratedMod.ORIGOLD_ASSEMBLY_STATION_ITEM.get())) {
            appendHints(tooltip, "station");
        } else if (stack.is(GeneratedMod.SPECTER_MEDALLION.get())) {
            appendHints(tooltip, "specter");
        } else if (stack.is(GeneratedMod.SWIFTNESS_RUNE.get())) {
            appendHints(tooltip, "swift");
        } else if (stack.is(GeneratedMod.GREED_RUNE.get())) {
            appendHints(tooltip, "greed");
        } else if (stack.is(GeneratedMod.REAPING_RUNE.get())) {
            appendHints(tooltip, "harvest");
        } else if (stack.is(GeneratedMod.SIPHON_RUNE.get())) {
            appendHints(tooltip, "siphon");
        } else if (stack.is(GeneratedMod.ASSASSIN_MEDALLION.get())) {
            appendHints(tooltip, "assassin");
        } else if (stack.is(GeneratedMod.MOONWALKER_MEDALLION.get())) {
            appendHints(tooltip, "moonwalker");
        } else if (EternalPotionItem.isEternalPotion(stack)) {
            appendHints(tooltip, "eternal");
        } else if (stack.is(GeneratedMod.VOID_QUIVER.get())) {
            appendHints(tooltip, "quiver");
        } else if (stack.is(GeneratedMod.FINIS_PEARL.get())) {
            appendHints(tooltip, "finis");
        } else if (stack.is(GeneratedMod.TOTEM_OF_RESURRECTION.get())) {
            appendHints(tooltip, "totem");
        } else if (stack.is(GeneratedMod.SHADOW_MEDALLION.get())) {
            appendHints(tooltip, "shadow");
        } else if (stack.is(GeneratedMod.BEAST_RUNE.get())) {
            appendHints(tooltip, "beast");
        } else if (stack.is(GeneratedMod.EVERNIGHT_ASCENSION_EMBLEM.get())) {
            appendHints(tooltip, "night");
        } else if (stack.is(GeneratedMod.GRAPNEL_BALLISTA.get())) {
            appendHints(tooltip, "grapnel");
        } else if (stack.is(GeneratedMod.WIRELESS_CONNECTOR.get())) {
            appendConnectorHints(tooltip);
        } else if (stack.is(Items.RECOVERY_COMPASS)) {
            // 原版回溯指针（追溯指针）：本模组为其注入“右键传送回重生点”功能。
            appendHints(tooltip, "compass");
        }
        // 词条列表独立于上面的提示分支：部件与镀金装备在铁砧敲合后，词条数据并入
        // 装备本体，任何已赋源的装备（原版武器盔甲，也包括钩爪弩炮这类挂在本模组
        // 自定义提示分支上的物品）都要像部件一样列出详细词条。此前它挂在 if-else
        // 链尾，命中过自定义提示分支的物品词条显示会被整个吞掉。
        if (!stack.is(GeneratedMod.BROKEN_ORIGOLD_PARTS.get()) && TraitLibrary.isInfused(stack)) {
            TraitLibrary.appendTooltip(stack, tooltip);
        }
    }

    /**
     * Appends the item-specific hints to the very bottom of the tooltip:
     * the useful hint in light blue regular text and the fluff line in
     * light gray italic underneath it. Both are translatable so they render
     * in the current client language.
     */
    private static void appendHints(List<Component> tooltip, String key) {
        tooltip.add(Component.translatable(HINT_PREFIX + key + "_useful")
                .withStyle(style -> style.withColor(ChatFormatting.AQUA).withItalic(false)));
        tooltip.add(Component.translatable(HINT_PREFIX + key + "_bullshit")
                .withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(true)));
    }

    /**
     * 金源无线连接装置的悬浮提示：首句实用提示常显且与其它物品的实用提示同为
     * 浅蓝色；其余明细收起为一行深灰色的“（按Shift显示详情）”，长按 Shift 时
     * 原地展开为同样深灰色的完整明细；末尾保留灰色斜体的风味引言。
     */
    private static void appendConnectorHints(List<Component> tooltip) {
        tooltip.add(Component.translatable("hint.origolds_ar.connector_useful")
                .withStyle(style -> style.withColor(ChatFormatting.AQUA).withItalic(false)));
        tooltip.add(Component.translatable(isShiftDown()
                        ? "hint.origolds_ar.connector_details" : "hint.origolds_ar.connector_shift")
                .withStyle(style -> style.withColor(ChatFormatting.DARK_GRAY).withItalic(false)));
        tooltip.add(Component.translatable("tooltip.origolds_ar.wireless_connector_quote")
                .withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(true)));
    }

    /** 键盘 Shift（左右任一）是否正被按住：tooltip 每帧重建，可实时响应按键。 */
    private static boolean isShiftDown() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }
}
