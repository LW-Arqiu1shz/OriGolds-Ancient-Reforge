package cn.blockforge.generated.origoldsar;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 测伤志愿者物品：放置时生成一只没有 AI 的村民（{@link VolunteerDummy}）。
 * 物品栏贴图与模型依旧复用原版盔甲架。
 */
public class VolunteerDummyItem extends Item {
    public VolunteerDummyItem(Properties properties) {
        super(properties);
    }

    /**
     * 物品悬浮提示：与 1.21.1 参考版一致的一行灰色斜体风味文案。
     * 移植 26.1.2 时这个方法连同语言键一起被漏掉了，故在此补回
     * （26.1.2 起签名为 TooltipDisplay + Consumer&lt;Component&gt;）。
     */
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.origolds_ar.volunteer_dummy")
                .withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(true)));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Direction direction = context.getClickedFace();
        if (direction == Direction.DOWN) {
            return InteractionResult.FAIL;
        }
        Level level = context.getLevel();
        BlockPlaceContext blockPlaceContext = new BlockPlaceContext(context);
        BlockPos blockPos = blockPlaceContext.getClickedPos();
        ItemStack itemStack = context.getItemInHand();
        Vec3 vec3 = Vec3.atBottomCenterOf(blockPos);
        AABB aabb = GeneratedMod.VOLUNTEER_DUMMY_TYPE.get().getDimensions().makeBoundingBox(vec3.x(), vec3.y(), vec3.z());
        if (level.noCollision(null, aabb) && level.getEntities(null, aabb).isEmpty()) {
            if (level instanceof ServerLevel serverLevel) {
                Consumer<VolunteerDummy> consumer = EntityType.createDefaultStackConfig(serverLevel, itemStack, context.getPlayer());
                VolunteerDummy dummy = GeneratedMod.VOLUNTEER_DUMMY_TYPE.get().create(serverLevel, consumer, blockPos, EntitySpawnReason.SPAWN_ITEM_USE, true, true);
                if (dummy == null) {
                    return InteractionResult.FAIL;
                }
                float f = (float) Mth.floor((Mth.wrapDegrees(context.getRotation() - 180.0F) + 22.5F) / 45.0F) * 45.0F;
                dummy.snapTo(dummy.getX(), dummy.getY(), dummy.getZ(), f, 0.0F);
                // moveTo 只设 yRot；村民的身体/头部朝向（yBodyRot/yHeadRot）不会随之更新，
                // 这里在源头对齐，避免落下来时身体停在默认南面（看起来“朝向随机”）。
                dummy.setYBodyRot(f);
                dummy.setYHeadRot(f);
                serverLevel.addFreshEntityWithPassengers(dummy);
                level.playSound(null, dummy.getX(), dummy.getY(), dummy.getZ(), SoundEvents.ARMOR_STAND_PLACE, SoundSource.BLOCKS, 0.75F, 0.8F);
                dummy.gameEvent(GameEvent.ENTITY_PLACE, context.getPlayer());
            }
            itemStack.shrink(1);
            return (level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER);
        }
        return InteractionResult.FAIL;
    }
}
