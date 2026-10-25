package de.jakob.lotm.structure;

import com.mojang.serialization.MapCodec;
import de.jakob.lotm.LOTMCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModStructures {

    //tutorial for structure placement: https://github.com/TelepathicGrunt/StructureTutorialMod/blob/1.21-Neoforge-Jigsaw/src/main/java/com/telepathicgrunt/structure_tutorial/STStructures.java

    public static final DeferredRegister<StructureType<?>> DEFERRED_REGISTRY_STRUCTURE = DeferredRegister.create(Registries.STRUCTURE_TYPE, LOTMCraft.MOD_ID);


    public static final DeferredHolder<StructureType<?>, StructureType<SimpleStructures>> SIMPLE_STRUCTURES = DEFERRED_REGISTRY_STRUCTURE.register("simple_structures", () -> explicitStructureTypeTyping(SimpleStructures.CODEC));
    public static final DeferredHolder<StructureType<?>, StructureType<CityStructure>> CITY_STRUCTURE =
            DEFERRED_REGISTRY_STRUCTURE.register("city_structure", () -> () -> CityStructure.CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<PairedStructure>> PAIRED_STRUCTURE =
            DEFERRED_REGISTRY_STRUCTURE.register("paired_structure", () -> () -> PairedStructure.CODEC);

    /**
     * Originally, I had a double lambda ()->()-> for the RegistryObject line above, but it turns out that
     * some IDEs cannot resolve the typing correctly. This method explicitly states what the return type
     * is so that the IDE can put it into the DeferredRegistry properly.
     */
    private static <T extends Structure> StructureType<T> explicitStructureTypeTyping(MapCodec<T> structureCodec) {
        return () -> structureCodec;
    }

    // estrutura subterrânea com tumba na superfície e escadaria em espiral (Ruínas de Tudor-Trunsoest e as próximas)
    public static final DeferredHolder<StructureType<?>, StructureType<de.jakob.lotm.structure.tomb.TombRuinsStructure>> TOMB_RUINS =
            DEFERRED_REGISTRY_STRUCTURE.register("tomb_ruins", () -> explicitStructureTypeTyping(de.jakob.lotm.structure.tomb.TombRuinsStructure.CODEC));
    public static final DeferredHolder<StructureType<?>, StructureType<de.jakob.lotm.structure.tomb.SurfaceTemplateStructure>> SURFACE_TEMPLATE =
            DEFERRED_REGISTRY_STRUCTURE.register("surface_template", () -> explicitStructureTypeTyping(de.jakob.lotm.structure.tomb.SurfaceTemplateStructure.CODEC));

    public static final DeferredRegister<net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType> PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, LOTMCraft.MOD_ID);
    public static final DeferredHolder<net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType,
            net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType> TOMB_TEMPLATE_PIECE = PIECE_TYPES.register("tomb_template",
            () -> (net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType.StructureTemplateType) de.jakob.lotm.structure.tomb.TombTemplatePiece::new);
    public static final DeferredHolder<net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType,
            net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType> STAIR_SHAFT_PIECE = PIECE_TYPES.register("stair_shaft",
            () -> (net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType.ContextlessType) de.jakob.lotm.structure.tomb.StairShaftPiece::new);

    // posicionamento que evita outras estruturas (para não nascerem umas em cima das outras)
    public static final DeferredRegister<net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType<?>> PLACEMENT_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, LOTMCraft.MOD_ID);
    public static final DeferredHolder<net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType<?>,
            net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType<ExclusiveSpreadPlacement>> EXCLUSIVE_SPREAD =
            PLACEMENT_TYPES.register("exclusive_spread", () -> () -> ExclusiveSpreadPlacement.CODEC);
    public static final DeferredHolder<net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType<?>,
            net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType<FarRingPlacement>> FAR_RING =
            PLACEMENT_TYPES.register("far_ring", () -> () -> FarRingPlacement.CODEC);

    public static void register(IEventBus eventBus) {
        DEFERRED_REGISTRY_STRUCTURE.register(eventBus);
        PLACEMENT_TYPES.register(eventBus);
        PIECE_TYPES.register(eventBus);
    }

}
