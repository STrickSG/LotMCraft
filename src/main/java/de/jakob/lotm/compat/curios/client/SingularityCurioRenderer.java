package de.jakob.lotm.compat.curios.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.jakob.lotm.beyonders.singularity.SingularityData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/** Desenha a Singularidade equipada no corpo do personagem, em um lugar que combina com cada uma. */
public class SingularityCurioRenderer implements ICurioRenderer {

    private enum Placement { FACE, MONOCLE, BACK, HIP, HALO }

    private static Placement placementFor(String pathway) {
        return switch (pathway) {
            case "fool" -> Placement.FACE;               // Máscara do Tolo
            case "error" -> Placement.MONOCLE;           // Monóculo de Cristal
            case "red_priest" -> Placement.BACK;         // Estandarte de Sangue de Salinger
            case "wheel_of_fortune", "justiciar" -> Placement.HIP;
            default -> Placement.HALO;                   // flutua atrás da cabeça
        };
    }

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext, PoseStack poseStack,
                                                                          RenderLayerParent<T, M> renderLayerParent, MultiBufferSource buffer,
                                                                          int light, float limbSwing, float limbSwingAmount, float partialTicks,
                                                                          float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(renderLayerParent.getModel() instanceof HumanoidModel<?> model)) return;
        LivingEntity entity = slotContext.entity();
        var itemRenderer = Minecraft.getInstance().getItemRenderer();
        Placement placement = placementFor(SingularityData.pathwayOf(stack));

        poseStack.pushPose();
        switch (placement) {
            case FACE, MONOCLE -> {
                model.head.translateAndRotate(poseStack);
                // Mesmo posicionamento usado pelo Minecraft para itens na cabeça (fica na frente do rosto)
                poseStack.translate(0, -0.25, 0);
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
                if (placement == Placement.MONOCLE) {
                    poseStack.translate(0.12, 0.06, 0.02);
                    poseStack.scale(0.28f, -0.28f, -0.28f);
                } else {
                    poseStack.scale(0.6f, -0.6f, -0.6f);
                }
                itemRenderer.renderStatic(stack, ItemDisplayContext.HEAD, light, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
            }
            case HALO -> {
                model.head.translateAndRotate(poseStack);
                float bob = (float) Math.sin((ageInTicks + partialTicks) * 0.08f) * 0.05f;
                poseStack.translate(0, -0.72 + bob, 0.28);
                poseStack.mulPose(Axis.YP.rotationDegrees((ageInTicks + partialTicks) * 2f));
                poseStack.scale(0.42f, -0.42f, -0.42f);
                itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
            }
            case BACK -> {
                ICurioRenderer.translateIfSneaking(poseStack, entity);
                ICurioRenderer.rotateIfSneaking(poseStack, entity);
                model.body.translateAndRotate(poseStack);
                poseStack.translate(0, 0.32, 0.2);
                poseStack.scale(0.75f, -0.75f, -0.75f);
                itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
            }
            case HIP -> {
                ICurioRenderer.translateIfSneaking(poseStack, entity);
                ICurioRenderer.rotateIfSneaking(poseStack, entity);
                model.body.translateAndRotate(poseStack);
                poseStack.translate(0.3, 0.62, 0);
                poseStack.mulPose(Axis.YP.rotationDegrees(90));
                poseStack.scale(0.3f, -0.3f, -0.3f);
                itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
            }
        }
        poseStack.popPose();
    }
}
