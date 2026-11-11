package cn.blockforge.generated.origoldsar.client;

import java.util.Locale;

import cn.blockforge.generated.origoldsar.VolunteerDummy;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * 测伤志愿者的渲染器：完全复用村民模型与贴图（含原版村民自带的头盔渲染层），
 * 只在“头顶名字”位置额外绘制两行文本——第一行过去 3 秒 DPS，第二行上一次受到的伤害。
 */
public class VolunteerDummyRenderer extends VillagerRenderer {
    /** 名字标签两行的行高（Font 每行约 9 像素）。 */
    private static final float LINE_HEIGHT = 9.0F;

    public VolunteerDummyRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected boolean shouldShowName(Villager entity) {
        double distance = this.entityRenderDispatcher.distanceToSqr(entity);
        float range = entity.isCrouching() ? 32.0F : 64.0F;
        if (distance >= (double) (range * range)) {
            return false;
        }
        return (entity instanceof VolunteerDummy dummy && dummy.isDisplayActive())
                || entity.isCustomNameVisible();
    }

    @Override
    protected void renderNameTag(Villager entity, Component component, PoseStack poseStack,
                                 MultiBufferSource bufferSource, int packedLight, float partialTick) {
        if (!(entity instanceof VolunteerDummy dummy) || !dummy.isDisplayActive()) {
            super.renderNameTag(entity, component, poseStack, bufferSource, packedLight, partialTick);
            return;
        }
        Vec3 tagPos = entity.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, entity.getViewYRot(partialTick));
        if (tagPos == null) {
            return;
        }
        boolean seeThrough = !entity.isDiscrete();
        Font font = this.getFont();
        int background = (int) (Minecraft.getInstance().options.getBackgroundOpacity(0.25F) * 255.0F) << 24;

        Component line1 = Component.translatable("text.origolds_ar.volunteer_dps",
                formatNumber(dummy.getDps()));
        Component line2 = Component.translatable("text.origolds_ar.volunteer_last",
                formatNumber(dummy.getLastDamage()));
        float width1 = font.width(line1);
        float width2 = font.width(line2);

        poseStack.pushPose();
        poseStack.translate(tagPos.x, tagPos.y + 0.5, tagPos.z);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(0.025F, -0.025F, 0.025F);
        Matrix4f matrix = poseStack.last().pose();

        // 第一行（DPS）在上，第二行（上次伤害）在下。
        drawLine(line1, -width1 / 2.0F, -LINE_HEIGHT, seeThrough, font, matrix, bufferSource, background, packedLight);
        drawLine(line2, -width2 / 2.0F, 0.0F, seeThrough, font, matrix, bufferSource, background, packedLight);

        poseStack.popPose();
    }

    private static void drawLine(Component text, float x, float y, boolean seeThrough, Font font,
                                 Matrix4f matrix, MultiBufferSource bufferSource, int background, int packedLight) {
        font.drawInBatch(text, x, y, 553648127, false, matrix, bufferSource,
                seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, background, packedLight);
        if (seeThrough) {
            font.drawInBatch(text, x, y, -1, false, matrix, bufferSource,
                    Font.DisplayMode.NORMAL, 0, packedLight);
        }
    }

    private static String formatNumber(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
