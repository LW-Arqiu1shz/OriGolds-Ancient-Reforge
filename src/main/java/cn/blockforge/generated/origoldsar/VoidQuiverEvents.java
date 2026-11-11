package cn.blockforge.generated.origoldsar;

import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingGetProjectileEvent;

/**
 * 让虚空箭匣充当“永不消耗的箭袋”：弓/弩询问弹药时，如果玩家身上没有任何真正的箭矢，
 * 就向箭匣借一根内容箭矢。原版消耗弹药是按“物品栏里那个堆栈对象本身”扣除的，
 * 而这里给出的是游离副本且箭匣物品不是箭，因此箭匣与内容箭矢都不会被消耗。
 *
 * <p>借出的箭矢会带上原版 {@link DataComponents#INTANGIBLE_PROJECTILE} 组件——
 * 这正是无限附魔/创造模式弹药使用的标记，箭实体生成时会据此把拾取模式设为
 * {@code CREATIVE_ONLY}，所以从箭匣射出的箭与无限附魔射出的箭一样无法在生存模式被捡起。
 */
@EventBusSubscriber(modid = GeneratedMod.MOD_ID)
public final class VoidQuiverEvents {
    private VoidQuiverEvents() {}

    @SubscribeEvent
    public static void onGetProjectile(LivingGetProjectileEvent event) {
        if (!event.getProjectileItemStack().isEmpty()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack quiver = findQuiver(player);
        if (quiver.isEmpty()) {
            return;
        }
        ItemStack projectile = VoidQuiverItem.arrowOf(quiver);
        if (VoidQuiverItem.isArrow(projectile)) {
            ItemStack borrowed = projectile.copyWithCount(1);
            borrowed.set(DataComponents.INTANGIBLE_PROJECTILE, Unit.INSTANCE);
            event.setProjectileItemStack(borrowed);
        } else if (VoidQuiverItem.isFireworkRocket(projectile)) {
            lendFirework(event, projectile);
        }
    }

    /**
     * 1.2.0 起箭匣也可封装烟花：只有当"武器自己认可该弹药"（弩的
     * {@link net.minecraft.world.item.ProjectileWeaponItem#getAllSupportedProjectiles}）时
     * 才借出，弓或其它武器不受影响。借出的是数量 1 的游离副本，箭匣不被消耗。
     */
    private static void lendFirework(LivingGetProjectileEvent event, ItemStack firework) {
        ItemStack weapon = event.getProjectileWeaponItemStack();
        if (!(weapon.getItem() instanceof ProjectileWeaponItem weaponItem)) {
            return;
        }
        if (!weaponItem.getAllSupportedProjectiles(weapon).test(firework)) {
            return;
        }
        event.setProjectileItemStack(firework.copyWithCount(1));
    }

    private static ItemStack findQuiver(Player player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(GeneratedMod.VOID_QUIVER.get())) {
                return stack;
            }
        }
        ItemStack offhand = player.getOffhandItem();
        return offhand.is(GeneratedMod.VOID_QUIVER.get()) ? offhand : ItemStack.EMPTY;
    }
}
