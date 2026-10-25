package de.jakob.lotm.addons.blasphemy;

import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import de.jakob.lotm.structure.FarRingPlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cartas que trocam de ruína: a estrutura delas só existe poucas vezes por mundo (far_ring: longe do spawn e umas das outras)
 * e, enquanto ninguém pega a carta, a cada 10 minutos ela vai para o altar de outra ruína, sorteada.
 */
@EventBusSubscriber(modid = LOTMCraft.MOD_ID)
public final class BlasphemyCardRotation {
    public static final int INTERVAL = 20 * 60 * 10;

    /** Caminho da carta -> conjunto de estruturas (far_ring) onde ficam os altares dela */
    private static final Map<String, ResourceLocation> SITES = Map.of(
            "red_priest", ResourceLocation.fromNamespaceAndPath(LOTMCraft.MOD_ID, "tudor_trunsoest_ruins_set")
    );

    /** Altares de cada carta neste mundo (não mudam, então são calculados uma vez só) */
    private static final Map<String, List<BlockPos>> ALTARS = new HashMap<>();

    private BlasphemyCardRotation() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().overworld().getGameTime() % INTERVAL == 0) rotate(event.getServer());
    }

    /** Cada carta parada num altar (ou sumida) vai para o altar de outra ruína, sorteada. */
    public static void rotate(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlasphemyCardRegistry registry = BlasphemyCardRegistry.get(server);
        for (String pathway : SITES.keySet()) {
            BlasphemyCardRegistry.Entry entry = registry.get(pathway);
            // com alguém, num baú, no chão...: a carta só troca de ruína enquanto está num altar (ou sumiu)
            if (entry.state() == BlasphemyCardRegistry.State.OUT) continue;
            List<BlockPos> altars = altars(level, pathway);
            if (altars.isEmpty() || (altars.size() < 2 && entry.state() == BlasphemyCardRegistry.State.ALTAR)) continue;
            int current = entry.state() == BlasphemyCardRegistry.State.ALTAR && level.dimension().equals(entry.dimension())
                    ? altars.indexOf(entry.pos()) : -1;
            // sorteia um dos outros altares (nunca o mesmo em que ela já está)
            int next = level.random.nextInt(current < 0 ? altars.size() : altars.size() - 1);
            if (current >= 0 && next >= current) next++;
            registry.moveTo(pathway, level.dimension(), altars.get(next));
        }
    }

    /** Altares das ruínas da carta deste Caminho no mundo. */
    public static List<BlockPos> altars(ServerLevel level, String pathway) {
        List<BlockPos> altars = ALTARS.get(pathway);
        if (altars == null && SITES.containsKey(pathway)) {
            altars = findAltars(level, SITES.get(pathway));
            if (!altars.isEmpty()) ALTARS.put(pathway, altars);
        }
        return altars == null ? List.of() : altars;
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ALTARS.clear();
    }

    // posições dos altares nas ruínas do conjunto: só precisa das peças da estrutura, sem gerar os blocos
    private static List<BlockPos> findAltars(ServerLevel level, ResourceLocation setId) {
        List<BlockPos> result = new ArrayList<>();
        Holder<StructureSet> set = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET)
                .getHolder(ResourceKey.create(Registries.STRUCTURE_SET, setId)).orElse(null);
        if (set == null || !(set.value().placement() instanceof FarRingPlacement placement)) return result;
        List<ChunkPos> rings = placement.positions(level.getChunkSource().getGeneratorState());
        for (ChunkPos pos : rings) {
            ChunkAccess chunk = level.getChunk(pos.x, pos.z, ChunkStatus.STRUCTURE_STARTS);
            for (StructureSet.StructureSelectionEntry selection : set.value().structures()) {
                StructureStart start = level.structureManager().getStartForStructure(SectionPos.bottomOf(chunk), selection.structure().value(), chunk);
                if (start == null || !start.isValid()) continue;
                for (StructurePiece piece : start.getPieces()) {
                    if (!(piece instanceof TemplateStructurePiece templatePiece)) continue;
                    for (StructureTemplate.StructureBlockInfo info : templatePiece.template().filterBlocks(
                            templatePiece.templatePosition(), templatePiece.placeSettings(), ModBlocks.BLASPHEMY_ALTAR.get())) {
                        result.add(info.pos().immutable());
                    }
                }
            }
        }
        return result;
    }
}
