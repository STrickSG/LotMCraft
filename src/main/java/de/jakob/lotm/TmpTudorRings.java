package de.jakob.lotm;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.List;

/** TEMPORÁRIO (não commitar): confere as 3 Ruínas Tudor-Trunsoest por mundo. */
@EventBusSubscriber(modid = LOTMCraft.MOD_ID, value = Dist.CLIENT)
public class TmpTudorRings {
    private static int stage = 0, timer = 0;
    private static long seed = 4242L;

    private static void log(String msg) {
        LOTMCraft.LOGGER.info("[ANEIS] {}", msg);
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (stage == 0 && mc.screen instanceof TitleScreen) {
            stage = 1;
            String s = System.getenv("TMP_SEEDS");
            if (s != null) seed = Long.parseLong(s);
            LevelSettings settings = new LevelSettings("Aneis" + seed, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                    new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("Aneis" + seed, settings, new WorldOptions(seed, true, false),
                    access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.NORMAL).value().createWorldDimensions(),
                    new TitleScreen());
        } else if (stage == 1 && mc.player != null && mc.getSingleplayerServer() != null && ++timer > 60) {
            stage = 2;
            var server = mc.getSingleplayerServer();
            server.execute(() -> {
                ServerLevel level = server.overworld();
                var setKey = ResourceKey.create(Registries.STRUCTURE_SET, ResourceLocation.fromNamespaceAndPath(LOTMCraft.MOD_ID, "tudor_trunsoest_ruins_set"));
                Holder<StructureSet> set = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).getHolderOrThrow(setKey);
                Structure structure = set.value().structures().get(0).structure().value();
                var placement = (de.jakob.lotm.structure.FarRingPlacement) set.value().placement();
                List<ChunkPos> rings = placement.positions(level.getChunkSource().getGeneratorState());
                log("semente " + seed + ": " + rings.size() + " posições");
                int ok = 0;
                for (ChunkPos pos : rings) {
                    ChunkAccess chunk = level.getChunk(pos.x, pos.z, ChunkStatus.STRUCTURE_STARTS);
                    StructureStart start = level.structureManager().getStartForStructure(SectionPos.bottomOf(chunk), structure, chunk);
                    boolean valid = start != null && start.isValid();
                    if (valid) ok++;
                    log("  bloco " + pos.getMiddleBlockX() + ", " + pos.getMiddleBlockZ() + " (distância " + (int) Math.sqrt(pos.getMiddleBlockX() * (double) pos.getMiddleBlockX() + pos.getMiddleBlockZ() * (double) pos.getMiddleBlockZ()) + ") -> " + (valid ? "gerou" : "NÃO gerou")
                            + " bioma " + level.getBiome(pos.getMiddleBlockPosition(64)).unwrapKey().map(k -> k.location().toString()).orElse("?"));
                }
                log("total gerado: " + ok);
                var altars = de.jakob.lotm.addons.blasphemy.BlasphemyCardRotation.altars(level, "red_priest");
                log("altares: " + altars);
                for (int i = 0; i < altars.size(); i++) for (int j = i + 1; j < altars.size(); j++)
                    log("  distância entre ruína " + (i + 1) + " e " + (j + 1) + ": " + (int) Math.sqrt(altars.get(i).distSqr(altars.get(j))));
                var reg = de.jakob.lotm.addons.blasphemy.BlasphemyCardRegistry.get(server);
                for (int i = 0; i < 4; i++) {
                    de.jakob.lotm.addons.blasphemy.BlasphemyCardRotation.rotate(server);
                    log("  troca " + (i + 1) + ": carta em " + reg.get("red_priest"));
                }
                mc.execute(mc::stop);
            });
        }
    }
}
