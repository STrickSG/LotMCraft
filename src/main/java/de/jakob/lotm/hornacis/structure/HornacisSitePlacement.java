package de.jakob.lotm.hornacis.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.jakob.lotm.hornacis.HornacisRegion;
import de.jakob.lotm.hornacis.HornacisWorld;
import de.jakob.lotm.structure.ModStructures;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Vec3i;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * As estruturas da Cordilheira de Hornacis ficam nos lugares que a própria cordilheira escolheu (os terraços
 * planos do HornacisRegion: o palácio no platô do cume, as ruínas nas florestas, vales e encostas suaves em volta
 * do pico, os acampamentos, o mosteiro). Para o Minecraft é um concentric_rings (como as Fortalezas), então o
 * /locate e mods como o Explorer's Compass acham cada uma; as posições vêm daqui (FarRingPositionsMixin).
 */
public class HornacisSitePlacement extends ConcentricRingsStructurePlacement {
    public static final MapCodec<HornacisSitePlacement> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("salt").forGetter(HornacisSitePlacement::saltValue),
            Codec.STRING.xmap(s -> HornacisRegion.SiteType.valueOf(s.toUpperCase(Locale.ROOT)), t -> t.name().toLowerCase(Locale.ROOT))
                    .fieldOf("site").forGetter(p -> p.site)
    ).apply(instance, HornacisSitePlacement::new));

    private final HornacisRegion.SiteType site;

    public HornacisSitePlacement(int salt, HornacisRegion.SiteType site) {
        super(Vec3i.ZERO, FrequencyReductionMethod.DEFAULT, 1.0F, salt, Optional.empty(), 0, 1, 1, HolderSet.empty());
        this.site = site;
    }

    private int saltValue() {
        return salt();
    }

    public HornacisRegion.SiteType site() {
        return site;
    }

    /** Os chunks dos lugares deste tipo na cordilheira do mundo aberto (nenhum se o mundo não tem cordilheira). */
    public List<ChunkPos> positions() {
        HornacisRegion region = HornacisWorld.region();
        List<ChunkPos> list = new ArrayList<>();
        if (region == null) return list;
        for (HornacisRegion.Site s : region.sites()) {
            if (s.type() == site) list.add(new ChunkPos(s.x() >> 4, s.z() >> 4));
        }
        return list;
    }

    @Override
    public @NotNull StructurePlacementType<?> type() {
        return ModStructures.HORNACIS_SITE_PLACEMENT.get();
    }
}
