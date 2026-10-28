package de.jakob.lotm.hornacis.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.jakob.lotm.hornacis.HornacisRegion;
import de.jakob.lotm.hornacis.HornacisWorld;
import de.jakob.lotm.structure.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Uma estrutura num lugar da Cordilheira de Hornacis (ver HornacisSitePlacement). O template sai da lista pelo
 * número do lugar, então cada lugar tem sempre a mesma; acima de {@code high_y} usa a lista {@code high_templates}
 * (por exemplo, os acampamentos soterrados na neve). Com {@code houses}, vira um vilarejo: o template é o centro
 * (a praça) e {@code house_count} casas ficam num anel de raio {@code house_ring} em volta dele.
 */
public class HornacisSiteStructure extends Structure {
    public static final MapCodec<HornacisSiteStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            Codec.STRING.xmap(s -> HornacisRegion.SiteType.valueOf(s.toUpperCase(Locale.ROOT)), t -> t.name().toLowerCase(Locale.ROOT))
                    .fieldOf("site").forGetter(s -> s.site),
            ResourceLocation.CODEC.listOf().fieldOf("templates").forGetter(s -> s.templates),
            ResourceLocation.CODEC.listOf().optionalFieldOf("high_templates", List.of()).forGetter(s -> s.highTemplates),
            Codec.INT.optionalFieldOf("high_y", Integer.MAX_VALUE).forGetter(s -> s.highY),
            ResourceLocation.CODEC.listOf().optionalFieldOf("houses", List.of()).forGetter(s -> s.houses),
            Codec.INT.optionalFieldOf("house_count", 0).forGetter(s -> s.houseCount),
            Codec.INT.optionalFieldOf("house_ring", 20).forGetter(s -> s.houseRing)
    ).apply(instance, HornacisSiteStructure::new));

    private final HornacisRegion.SiteType site;
    private final List<ResourceLocation> templates, highTemplates;
    private final int highY;
    /** Vilarejo: casas em volta do template do centro, cada uma no chão do próprio lugar e com a porta para o centro */
    private final List<ResourceLocation> houses;
    private final int houseCount, houseRing;

    public HornacisSiteStructure(StructureSettings settings, HornacisRegion.SiteType site, List<ResourceLocation> templates,
                                 List<ResourceLocation> highTemplates, int highY, List<ResourceLocation> houses, int houseCount, int houseRing) {
        super(settings);
        this.site = site;
        this.templates = templates;
        this.highTemplates = highTemplates;
        this.highY = highY;
        this.houses = houses;
        this.houseCount = houseCount;
        this.houseRing = houseRing;
    }

    @Override
    protected @NotNull Optional<GenerationStub> findGenerationPoint(@NotNull GenerationContext context) {
        HornacisRegion region = HornacisWorld.region();
        if (region == null || templates.isEmpty()) return Optional.empty();
        ChunkPos chunk = context.chunkPos();
        for (HornacisRegion.Site s : region.sites()) {
            if (s.type() != site || s.x() >> 4 != chunk.x || s.z() >> 4 != chunk.z) continue;
            List<ResourceLocation> list = s.y() >= highY && !highTemplates.isEmpty() ? highTemplates : templates;
            ResourceLocation template = list.get(Math.floorMod(s.index() * 7 + s.rotation(), list.size()));
            Rotation rotation = Rotation.values()[s.rotation() & 3];
            return Optional.of(new GenerationStub(new BlockPos(s.x(), s.y(), s.z()), builder -> {
                int cy = houses.isEmpty() ? s.y() : groundAt(context, s.x(), s.z(), 4);
                builder.addPiece(new HornacisSitePiece(context.structureTemplateManager(), template, s.x(), cy, s.z(), rotation));
                addHouses(context, builder, s);
            }));
        }
        return Optional.empty();
    }

    /**
     * As casas do vilarejo: espalhadas num anel em volta do centro (ângulos e distâncias sorteados pelo número do
     * lugar, sempre os mesmos), cada uma girada para a porta (no sul do template) ficar virada para o centro.
     */
    private void addHouses(GenerationContext context, StructurePiecesBuilder builder, HornacisRegion.Site s) {
        if (houses.isEmpty() || houseCount <= 0) return;
        RandomSource random = RandomSource.create(s.index() * 341873128712L + s.rotation() * 132897987541L + 0x484F524EL);
        double start = random.nextDouble() * Math.PI * 2;
        int first = random.nextInt(houses.size());
        for (int k = 0; k < houseCount; k++) {
            double a = start + k * Math.PI * 2 / houseCount + (random.nextDouble() - 0.5) * 0.35;
            double r = houseRing + random.nextInt(5) - 2;
            int x = s.x() + (int) Math.round(Math.cos(a) * r), z = s.z() + (int) Math.round(Math.sin(a) * r);
            // a porta olha para o sul no template: gira para ela olhar para o centro
            double toCenter = Math.atan2(s.z() - z, s.x() - x);
            int quarter = Math.floorMod((int) Math.round((toCenter - Math.PI / 2) / (Math.PI / 2)), 4);
            ResourceLocation house = houses.get((first + k) % houses.size());
            builder.addPiece(new HornacisSitePiece(context.structureTemplateManager(), house, x, groundAt(context, x, z, 4), z, Rotation.values()[quarter]));
        }
    }

    /** O primeiro bloco de ar acima do terreno em (x, z): a média do centro e de quatro pontos em volta. */
    private static int groundAt(GenerationContext context, int x, int z, int spread) {
        int sum = 0;
        int[][] pts = {{0, 0}, {spread, 0}, {-spread, 0}, {0, spread}, {0, -spread}};
        for (int[] p : pts) {
            sum += context.chunkGenerator().getBaseHeight(x + p[0], z + p[1], Heightmap.Types.WORLD_SURFACE_WG,
                    context.heightAccessor(), context.randomState());
        }
        return Math.round(sum / (float) pts.length);
    }

    @Override
    public @NotNull StructureType<?> type() {
        return ModStructures.HORNACIS_SITE.get();
    }
}
