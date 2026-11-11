package cn.blockforge.generated.origoldsar;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 勋章 / 符文类物品的能力继承判定：
 * 暗影勋章 = 幽灵勋章 + 刺客勋章；
 * 野兽符文 = 迅影 / 贪婪 / 收割 / 虹吸四枚符文；
 * 永夜擢升之证 = 暗影勋章 + 野兽符文（即全部能力）。
 */
public final class AbilityItems {
    private AbilityItems() {}

    /** 判断玩家是否在背包（含主手/副手）中持有某符文（含野兽符文与永夜擢升之证的继承）。 */
    public static boolean hasRuneInInventory(Player player, Item rune) {
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && hasRuneAbility(stack, rune)) {
                return true;
            }
        }
        return !player.getOffhandItem().isEmpty() && hasRuneAbility(player.getOffhandItem(), rune);
    }

    /** 判定勋章能力：持有 base 本身、暗影勋章或永夜擢升之证均视为持有。 */
    public static boolean hasMedallionAbility(ItemStack stack, Item base) {
        return stack.is(base)
                || stack.is(GeneratedMod.SHADOW_MEDALLION.get())
                || stack.is(GeneratedMod.EVERNIGHT_ASCENSION_EMBLEM.get());
    }

    /** 判定符文能力：持有 rune 本身、野兽符文或永夜擢升之证均视为持有。 */
    public static boolean hasRuneAbility(ItemStack stack, Item rune) {
        return stack.is(rune)
                || stack.is(GeneratedMod.BEAST_RUNE.get())
                || stack.is(GeneratedMod.EVERNIGHT_ASCENSION_EMBLEM.get());
    }
}
