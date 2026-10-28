package de.jakob.lotm.entity.client.hornacis_goat;

import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.entity.custom.HornacisGoatEntity;
import net.minecraft.client.model.GoatModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * O corpo da cabra comum com um chifre único no meio da testa, curvado para trás em três partes cada vez
 * mais finas (como o item do chifre).
 */
public class HornacisGoatModel extends GoatModel<HornacisGoatEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(LOTMCraft.MOD_ID, "hornacis_gray_mountain_goat"), "main");

    public HornacisGoatModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(2, 61).addBox("right ear", -6.0F, -11.0F, -10.0F, 3.0F, 2.0F, 1.0F)
                        .texOffs(2, 61).mirror().addBox("left ear", 2.0F, -11.0F, -10.0F, 3.0F, 2.0F, 1.0F)
                        .texOffs(23, 52).addBox("goatee", -0.5F, -3.0F, -14.0F, 0.0F, 7.0F, 5.0F),
                PartPose.offset(1.0F, 14.0F, 0.0F));
        // o chifre (no lugar do "esquerdo" da cabra comum, que o jogo esconde quando ele quebra)
        PartDefinition horn = head.addOrReplaceChild("left_horn", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                PartPose.offsetAndRotation(-0.5F, -9.0F, -9.0F, -0.35F, 0.0F, 0.0F));
        PartDefinition middle = horn.addOrReplaceChild("horn_middle", CubeListBuilder.create()
                        .texOffs(8, 0).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(-0.2F)),
                PartPose.offsetAndRotation(0.0F, -4.6F, 0.0F, -0.35F, 0.0F, 0.0F));
        middle.addOrReplaceChild("horn_tip", CubeListBuilder.create()
                        .texOffs(0, 8).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(-0.45F)),
                PartPose.offsetAndRotation(0.0F, -3.4F, 0.0F, -0.4F, 0.0F, 0.0F));
        head.addOrReplaceChild("right_horn", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(34, 46).addBox(-3.0F, -4.0F, -8.0F, 5.0F, 7.0F, 10.0F),
                PartPose.offsetAndRotation(0.0F, -8.0F, -8.0F, 0.9599F, 0.0F, 0.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(1, 1).addBox(-4.0F, -17.0F, -7.0F, 9.0F, 11.0F, 16.0F)
                        .texOffs(0, 28).addBox(-5.0F, -18.0F, -8.0F, 11.0F, 14.0F, 11.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(36, 29).addBox(0.0F, 4.0F, 0.0F, 3.0F, 6.0F, 3.0F), PartPose.offset(1.0F, 14.0F, 4.0F));
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(49, 29).addBox(0.0F, 4.0F, 0.0F, 3.0F, 6.0F, 3.0F), PartPose.offset(-3.0F, 14.0F, 4.0F));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(49, 2).addBox(0.0F, 0.0F, 0.0F, 3.0F, 10.0F, 3.0F), PartPose.offset(1.0F, 14.0F, -6.0F));
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(35, 2).addBox(0.0F, 0.0F, 0.0F, 3.0F, 10.0F, 3.0F), PartPose.offset(-3.0F, 14.0F, -6.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(HornacisGoatEntity goat, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(goat, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        this.head.getChild("left_horn").visible = goat.hasHorn();
        this.head.getChild("right_horn").visible = false;
    }
}
