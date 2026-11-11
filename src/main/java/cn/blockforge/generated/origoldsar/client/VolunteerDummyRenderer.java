package cn.blockforge.generated.origoldsar.client;

import java.util.Locale;

import cn.blockforge.generated.origoldsar.VolunteerDummy;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.villager.Villager;

/**
 * 测伤志愿者的渲染器：完全复用村民模型与贴图（含原版村民自带的头盔渲染层），
 * 只在“头顶名字”位置额外绘制两行文本——第一行过去 3 秒 DPS，第二行上一次受到的伤害。
 *
 * <p>26.1.2 的实体渲染改为“渲染状态 + 提交节点”模型：{@code renderNameTag(...)} 已被
 * {@link #submitNameDisplay} 取代，名字/分数文本通过
 * {@link SubmitNodeCollector#submitNameTag} 提交。因为要在名字标签处额外塞两行数据，
 * 这里用一个 {@link VillagerRenderState} 子类携带 DPS / 上次伤害。
 */
public class VolunteerDummyRenderer extends VillagerRenderer {
    /** 名字标签两行的行高（Font 每行约 9 像素），作为第二行相对名字锚点的像素偏移。 */
    private static final int LINE_HEIGHT = 9;

    public VolunteerDummyRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public VolunteerDummyRenderState createRenderState() {
        return new VolunteerDummyRenderState();
    }

    @Override
    public void extractRenderState(Villager entity, VillagerRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        if (state instanceof VolunteerDummyRenderState dummyState && entity instanceof VolunteerDummy dummy) {
            dummyState.displayActive = dummy.isDisplayActive();
            dummyState.dps = dummy.getDps();
            dummyState.lastDamage = dummy.getLastDamage();
        }
    }

    @Override
    protected boolean shouldShowName(Villager entity, double distanceToCameraSq) {
        float range = entity.isCrouching() ? 32.0F : 64.0F;
        if (distanceToCameraSq >= (double) (range * range)) {
            return false;
        }
        return (entity instanceof VolunteerDummy dummy && dummy.isDisplayActive())
                || entity.isCustomNameVisible();
    }

    @Override
    protected void submitNameDisplay(VillagerRenderState state, PoseStack poseStack,
                                     SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (!(state instanceof VolunteerDummyRenderState dummyState) || !dummyState.displayActive
                || state.nameTagAttachment == null) {
            super.submitNameDisplay(state, poseStack, submitNodeCollector, camera);
            return;
        }
        boolean seeThrough = !state.isDiscrete;
        Component line1 = Component.translatable("text.origolds_ar.volunteer_dps",
                formatNumber(dummyState.dps));
        Component line2 = Component.translatable("text.origolds_ar.volunteer_last",
                formatNumber(dummyState.lastDamage));

        // 第一行（DPS）在上，第二行（上次伤害）在下；offset 就是文字基线相对名字锚点的像素 y。
        submitNodeCollector.submitNameTag(poseStack, state.nameTagAttachment, -LINE_HEIGHT, line1,
                seeThrough, state.lightCoords, state.distanceToCameraSq, camera);
        submitNodeCollector.submitNameTag(poseStack, state.nameTagAttachment, 0, line2,
                seeThrough, state.lightCoords, state.distanceToCameraSq, camera);
    }

    private static String formatNumber(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    /** 携带测伤志愿者额外数据的渲染状态。 */
    public static final class VolunteerDummyRenderState extends VillagerRenderState {
        boolean displayActive;
        float dps;
        float lastDamage;
    }
}
