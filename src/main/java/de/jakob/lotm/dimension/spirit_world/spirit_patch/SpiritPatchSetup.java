package de.jakob.lotm.dimension.spirit_world.spirit_patch;

import de.jakob.lotm.LOTMCraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import terrablender.api.Regions;
import terrablender.api.SurfaceRuleManager;

public class SpiritPatchSetup {

    public static void init(IEventBus modBus) {
        SpiritPatchRegistries.register(modBus);
        modBus.addListener(SpiritPatchSetup::commonSetup);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            Regions.register(new SpiritPatchRegion(ResourceLocation.fromNamespaceAndPath(LOTMCraft.MOD_ID, "spirit_patch")));
            // o TerraBlender guarda uma regra por namespace: as do mod (Retalho Espiritual e Hornacis) vão juntas
            SurfaceRuleManager.addSurfaceRules(SurfaceRuleManager.RuleCategory.OVERWORLD,
                    LOTMCraft.MOD_ID, net.minecraft.world.level.levelgen.SurfaceRules.sequence(
                            SpiritPatchSurfaceRules.make(), de.jakob.lotm.hornacis.HornacisSurfaceRules.make()));
        });
    }
}
