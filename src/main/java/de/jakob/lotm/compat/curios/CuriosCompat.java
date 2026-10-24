package de.jakob.lotm.compat.curios;

import de.jakob.lotm.beyonders.potions.BlasphemyCardItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Único lugar que usa a API do Curios. Só é chamada quando o Curios está instalado
 * (ver BlasphemyCardCurioHandler), então o mod continua funcionando sem ele.
 */
public final class CuriosCompat {
    private CuriosCompat() {}

    public static ItemStack findEquippedCard(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity)
                .flatMap(inv -> inv.findFirstCurio(stack -> stack.getItem() instanceof BlasphemyCardItem))
                .map(result -> result.stack())
                .orElse(ItemStack.EMPTY);
    }
}
