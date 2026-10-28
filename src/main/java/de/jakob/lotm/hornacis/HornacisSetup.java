package de.jakob.lotm.hornacis;

import de.jakob.lotm.LOTMCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registros da Cordilheira de Hornacis (terreno, vegetação por altitude, superfície por inclinação). */
public final class HornacisSetup {
    public static final DeferredRegister<PlacementModifierType<?>> PLACEMENT_MODIFIERS =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, LOTMCraft.MOD_ID);
    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<HornacisAltitudeFilter>> ALTITUDE =
            PLACEMENT_MODIFIERS.register("hornacis_altitude", () -> () -> HornacisAltitudeFilter.CODEC);
    public static final DeferredRegister<com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.levelgen.SurfaceRules.ConditionSource>> SURFACE_CONDITIONS =
            DeferredRegister.create(Registries.MATERIAL_CONDITION, LOTMCraft.MOD_ID);

    static {
        SURFACE_CONDITIONS.register("hornacis_slope", () -> HornacisSlopeCondition.CODEC);
    }

    private HornacisSetup() {}

    public static void init(IEventBus modBus) {
        HornacisDensity.register(modBus);
        PLACEMENT_MODIFIERS.register(modBus);
        SURFACE_CONDITIONS.register(modBus);
    }
}
