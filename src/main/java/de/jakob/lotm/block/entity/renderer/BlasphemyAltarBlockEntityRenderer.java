package de.jakob.lotm.block.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.jakob.lotm.addons.blasphemy.BlasphemyCards;
import de.jakob.lotm.beyonders.potions.BlasphemyCardItemHandler;
import de.jakob.lotm.block.entity.BlasphemyAltarBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** A carta flutua e gira devagar acima do altar. */
public class BlasphemyAltarBlockEntityRenderer implements BlockEntityRenderer<BlasphemyAltarBlockEntity> {
    public BlasphemyAltarBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(BlasphemyAltarBlockEntity altar, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        if (!altar.hasCard() || altar.getLevel() == null) return;
        ItemStack card = BlasphemyCardItemHandler.cardOf(altar.getPathway());
        if (card.isEmpty()) return;
        float time = altar.getLevel().getGameTime() + partialTick;
        poseStack.pushPose();
        poseStack.translate(0.5, 1.45 + Math.sin(time * 0.06) * 0.06, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 1.2f));
        poseStack.scale(0.8f, 0.8f, 0.8f);
        Minecraft.getInstance().getItemRenderer().renderStatic(card, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, poseStack, buffer, altar.getLevel(), 0);
        poseStack.popPose();
    }
}
