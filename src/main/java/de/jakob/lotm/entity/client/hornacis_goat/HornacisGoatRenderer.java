package de.jakob.lotm.entity.client.hornacis_goat;

import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.entity.custom.HornacisGoatEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class HornacisGoatRenderer extends MobRenderer<HornacisGoatEntity, HornacisGoatModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(LOTMCraft.MOD_ID, "textures/entity/hornacis_gray_mountain_goat.png");

    public HornacisGoatRenderer(EntityRendererProvider.Context context) {
        super(context, new HornacisGoatModel(context.bakeLayer(HornacisGoatModel.LAYER_LOCATION)), 0.7F);
    }

    @Override
    public ResourceLocation getTextureLocation(HornacisGoatEntity entity) {
        return TEXTURE;
    }
}
