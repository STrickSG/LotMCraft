package de.jakob.lotm.compat.curios.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import de.jakob.lotm.beyonders.singularity.SingularityData;
import de.jakob.lotm.beyonders.singularity.SingularityModels;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
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
            case "red_priest", "darkness" -> Placement.BACK; // Estandarte de Salinger e a Foice da Escuridão
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

        if (placement == Placement.FACE) {
            renderFoolMask(stack, model, poseStack, buffer, light);
            return;
        }

        poseStack.pushPose();
        // Espaço do modelo do jogador: Y aponta para baixo e a frente do rosto/corpo é -Z.
        // Os modelos 3D têm a frente em +Z, então escalamos com Y e Z negativos para virá-los.
        String pathway = SingularityData.pathwayOf(stack);
        switch (placement) {
            case FACE -> { }
            case MONOCLE -> {
                model.head.translateAndRotate(poseStack);
                poseStack.translate(-0.12, -0.26, -0.3);
                poseStack.scale(0.2f, -0.2f, -0.2f);
            }
            case HALO -> {
                model.head.translateAndRotate(poseStack);
                float bob = (float) Math.sin((ageInTicks + partialTicks) * 0.08f) * 0.05f;
                poseStack.translate(0, -0.78 + bob, 0.3);
                poseStack.mulPose(Axis.YP.rotationDegrees((ageInTicks + partialTicks) * 2f));
                poseStack.scale(0.42f, -0.42f, -0.42f);
            }
            case BACK -> {
                ICurioRenderer.translateIfSneaking(poseStack, entity);
                ICurioRenderer.rotateIfSneaking(poseStack, entity);
                model.body.translateAndRotate(poseStack);
                poseStack.translate(0, 0.35, 0.22);
                if (pathway.equals("darkness")) {
                    // a foice fica atravessada nas costas
                    poseStack.mulPose(Axis.ZP.rotationDegrees(35));
                    poseStack.scale(1.1f, -1.1f, -1.1f);
                } else {
                    poseStack.mulPose(Axis.YP.rotationDegrees(180));
                    poseStack.scale(0.8f, -0.8f, -0.8f);
                }
            }
            case HIP -> {
                ICurioRenderer.translateIfSneaking(poseStack, entity);
                ICurioRenderer.rotateIfSneaking(poseStack, entity);
                model.body.translateAndRotate(poseStack);
                poseStack.translate(0.3, 0.62, 0);
                poseStack.mulPose(Axis.YP.rotationDegrees(90));
                poseStack.scale(0.26f, -0.26f, -0.26f);
            }
        }
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
        poseStack.popPose();
    }

    /** Máscara do Tolo no rosto: placa com as bordas curvadas para trás e fitas com laço atrás da cabeça. */
    private static void renderFoolMask(ItemStack stack, HumanoidModel<?> model, PoseStack poseStack, MultiBufferSource buffer, int light) {
        var minecraft = Minecraft.getInstance();
        VertexConsumer consumer = buffer.getBuffer(Sheets.cutoutBlockSheet());

        poseStack.pushPose();
        model.head.translateAndRotate(poseStack);
        // o modelo tem o centro da cabeça em (8, 8, 8), Y para cima e frente em +Z
        poseStack.translate(0, -0.25, 0);
        poseStack.scale(1f, -1f, -1f);
        poseStack.translate(-0.5, -0.5, -0.5);
        minecraft.getItemRenderer().renderModelLists(minecraft.getModelManager().getModel(SingularityModels.FOOL_WORN),
                stack, light, OverlayTexture.NO_OVERLAY, poseStack, consumer);
        poseStack.popPose();
    }
}
