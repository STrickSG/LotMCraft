package de.jakob.lotm.item.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

/**
 * Pose de duas mãos da Foice da Escuridão: a mão que segura fica pouco abaixo do meio do cabo
 * e a outra mão segura mais acima, no mesmo cabo. Registrada em META-INF/enumextensions.json.
 */
public final class ScytheArmPose {
    private ScytheArmPose() {}

    // Ângulos calculados junto com as transformações de exibição do modelo darkness_uniqueness.json
    private static final float HOLD_X = -0.55f, HOLD_Y = -0.30f;
    private static final float SUPPORT_X = -1.62f, SUPPORT_Y = 0.80f;

    public static final EnumProxy<HumanoidModel.ArmPose> POSE = new EnumProxy<>(
            HumanoidModel.ArmPose.class, true, (IArmPoseTransformer) (model, entity, arm) -> {
                boolean right = arm == HumanoidArm.RIGHT;
                ModelPart hold = right ? model.rightArm : model.leftArm;
                ModelPart support = right ? model.leftArm : model.rightArm;
                float side = right ? 1f : -1f;   // espelha a pose quando a foice está na mão esquerda
                hold.xRot = HOLD_X;
                hold.yRot = HOLD_Y * side;
                hold.zRot = 0f;
                support.xRot = SUPPORT_X;
                support.yRot = SUPPORT_Y * side;
                support.zRot = 0f;
            });
}
