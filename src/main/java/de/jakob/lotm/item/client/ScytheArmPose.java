package de.jakob.lotm.item.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

/**
 * Pose da Foice da Escuridão: o braço que segura fica levantado para a frente, com a mão no alto do cabo,
 * o cabo indo para trás e a lâmina pendurada na frente.
 * A outra mão fica livre (só não pode ser usada enquanto a foice está na mão, ver TwoHandedWeaponHandler).
 * Registrada em META-INF/enumextensions.json.
 */
public final class ScytheArmPose {
    private ScytheArmPose() {}

    // Ângulos calculados junto com as transformações de exibição do modelo item/singularity/darkness_scythe_3d.json
    private static final float HOLD_X = -1.05f, HOLD_Y = -0.35f;

    public static final EnumProxy<HumanoidModel.ArmPose> POSE = new EnumProxy<>(
            HumanoidModel.ArmPose.class, false, (IArmPoseTransformer) (model, entity, arm) -> {
                boolean right = arm == HumanoidArm.RIGHT;
                ModelPart hold = right ? model.rightArm : model.leftArm;
                hold.xRot = HOLD_X;
                hold.yRot = HOLD_Y * (right ? 1f : -1f);   // espelha quando a foice está na mão esquerda
                hold.zRot = 0f;
            });
}
