package de.jakob.lotm.compat.curios;

import de.jakob.lotm.beyonders.potions.BlasphemyCardItem;
import de.jakob.lotm.beyonders.singularity.SingularityData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.List;

/**
 * Único lugar que usa a API do Curios. Só é chamada quando o Curios está instalado,
 * então o mod continua funcionando sem ele.
 */
public final class CuriosCompat {
    private CuriosCompat() {}

    public static ItemStack findEquippedCard(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity)
                .flatMap(inv -> inv.findFirstCurio(stack -> stack.getItem() instanceof BlasphemyCardItem))
                .map(result -> result.stack())
                .orElse(ItemStack.EMPTY);
    }

    public static List<ItemStack> findEquippedSingularities(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity)
                .map(inv -> inv.findCurios(SingularityData::isSingularity).stream().map(SlotResult::stack).toList())
                .orElse(List.of());
    }

    public static void removeEquippedSingularities(LivingEntity entity) {
        CuriosApi.getCuriosInventory(entity).ifPresent(inv -> {
            for (SlotResult result : inv.findCurios(SingularityData::isSingularity)) {
                inv.getStacksHandler(result.slotContext().identifier()).ifPresent(handler ->
                        handler.getStacks().setStackInSlot(result.slotContext().index(), ItemStack.EMPTY));
            }
        });
    }
}
