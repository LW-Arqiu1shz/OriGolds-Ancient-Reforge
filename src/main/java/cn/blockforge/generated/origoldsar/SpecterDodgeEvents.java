package cn.blockforge.generated.origoldsar;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;

@EventBusSubscriber(modid = GeneratedMod.MOD_ID)
public final class SpecterDodgeEvents {
    /** 免疫成功音效的节流标签：同一实体每 0.2 秒（4 tick）最多播放一次。 */
    private static final String LAST_IMMUNITY_SOUND_TAG = "origolds_ar_immunity_sound_tick";
    private static final int IMMUNITY_SOUND_INTERVAL_TICKS = 4;

    private SpecterDodgeEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void cancelDamage(LivingIncomingDamageEvent event) {
        if (SpecterDodgeNetwork.isDodgeInvulnerable(event.getEntity())) {
            event.setCanceled(true);
            playImmunitySound(event.getEntity());
        }
    }

    /** 幽灵勋章免疫伤害成功时，播放盾牌格挡音效与清脆的“叮”声（吸收经验的音效），每 0.2 秒最多一次。 */
    private static void playImmunitySound(LivingEntity entity) {
        long now = entity.level().getGameTime();
        if (now - entity.getPersistentData().getLongOr(LAST_IMMUNITY_SOUND_TAG, 0L) < IMMUNITY_SOUND_INTERVAL_TICKS) {
            return;
        }
        entity.getPersistentData().putLong(LAST_IMMUNITY_SOUND_TAG, now);
        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();
        // “叮”声用更高的音调（2.0）让它更清脆锐利；盾牌格挡压到 0.6，只作一声黏膜下的闷响，不再盖住清脆的叮声。
        entity.level().playSound(null, x, y, z, SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.6F, 1.0F);
        entity.level().playSound(null, x, y, z, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 2.0F);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void cancelKnockback(LivingKnockBackEvent event) {
        if (SpecterDodgeNetwork.isDodgeInvulnerable(event.getEntity())) {
            event.setCanceled(true);
        }
    }
}
