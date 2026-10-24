package de.jakob.lotm.compat.curios;

import de.jakob.lotm.beyonders.singularity.SingularityData;
import de.jakob.lotm.beyonders.singularity.SingularityItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

/** Registro das regras de equipar as Singularidades (só chamado com o Curios instalado). */
public final class CuriosSetup {
    private CuriosSetup() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(CuriosSetup::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (Item item : SingularityItems.all()) {
            if (item instanceof de.jakob.lotm.item.custom.DarknessScytheItem) continue; // a foice é arma, não Curio
            event.registerItem(CuriosCapability.ITEM, (stack, context) -> new ICurio() {
                @Override
                public ItemStack getStack() {
                    return stack;
                }

                // Só o dono, sendo do mesmo Caminho (Seq 1 ou 0) ou um Deus de Caminho vizinho, pode equipar
                @Override
                public boolean canEquip(SlotContext slotContext) {
                    return slotContext.entity() instanceof Player player && SingularityData.canUse(stack, player);
                }

                @Override
                public boolean canEquipFromUse(SlotContext slotContext) {
                    return canEquip(slotContext);
                }
            }, item);
        }
    }
}
