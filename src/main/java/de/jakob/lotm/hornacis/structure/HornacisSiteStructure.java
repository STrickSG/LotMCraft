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
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Uma estrutura num lugar da Cordilheira de Hornacis (ver HornacisSitePlacement). O template sai da lista pelo
 * número do lugar, então cada lugar tem sempre a mesma; acima de {@code high_y} usa a lista {@code high_templates}
 * (por exemplo, os acampamentos soterrados na neve).
 */
public class HornacisSiteStructure extends Structure {
    public static final MapCodec<HornacisSiteStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            Codec.STRING.xmap(s -> HornacisRegion.SiteType.valueOf(s.toUpperCase(Locale.ROOT)), t -> t.name().toLowerCase(Locale.ROOT))
                    .fieldOf("site").forGetter(s -> s.site),
            ResourceLocation.CODEC.listOf().fieldOf("templates").forGetter(s -> s.templates),
            ResourceLocation.CODEC.listOf().optionalFieldOf("high_templates", List.of()).forGetter(s -> s.highTemplates),
            Codec.INT.optionalFieldOf("high_y", Integer.MAX_VALUE).forGetter(s -> s.highY)
    ).apply(instance, HornacisSiteStructure::new));

    private final HornacisRegion.SiteType site;
    private final List<ResourceLocation> templates, highTemplates;
    private final int highY;

    public HornacisSiteStructure(StructureSettings settings, HornacisRegion.SiteType site, List<ResourceLocation> templates,
                                 List<ResourceLocation> highTemplates, int highY) {
        super(settings);
        this.site = site;
        this.templates = templates;
        this.highTemplates = highTemplates;
        this.highY = highY;
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
            return Optional.of(new GenerationStub(new BlockPos(s.x(), s.y(), s.z()), builder ->
                    builder.addPiece(new HornacisSitePiece(context.structureTemplateManager(), template, s.x(), s.y(), s.z(), rotation))));
        }
        return Optional.empty();
    }

    @Override
    public @NotNull StructureType<?> type() {
        return ModStructures.HORNACIS_SITE.get();
    }
}
