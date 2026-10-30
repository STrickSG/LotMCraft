package de.jakob.lotm.hornacis;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.placement.CaveSurface;

import static net.minecraft.world.level.levelgen.SurfaceRules.*;

/**
 * Superfície da Cordilheira de Hornacis por altitude (como nas cordilheiras reais, numa escala de ~3,75 m por bloco):
 * floresta até ~530, prados alpinos até ~760 (linha da neve), rocha e neve até ~1050 e, acima, neve, gelo e
 * geleiras. As faces íngremes ficam de rocha nua (a neve não para nelas, como nos Alpes): quanto mais alto, menos
 * inclinação basta. As bordas entre as faixas variam com o ruído da superfície.
 */
public final class HornacisSurfaceRules {
    private HornacisSurfaceRules() {}

    private static RuleSource block(Block b) {
        return state(b.defaultBlockState());
    }

    private static ConditionSource above(int y) {
        return yBlockCheck(VerticalAnchor.absolute(y), 5);
    }

    public static RuleSource make() {
        ConditionSource onFloor = stoneDepthCheck(0, false, CaveSurface.FLOOR);
        ConditionSource underFloor = stoneDepthCheck(0, true, 4, CaveSurface.FLOOR);
        ConditionSource dry = waterBlockCheck(-1, 0);
        ConditionSource steep = steep();
        // inclinação (1 = 45°): faces de rocha no alto, encostas fortes com manchas de neve, paredões em qualquer altura
        ConditionSource face = new HornacisSlopeCondition(0.75);
        ConditionSource strong = new HornacisSlopeCondition(0.5);
        ConditionSource bank = new HornacisSlopeCondition(0.9);
        ConditionSource cliff = new HornacisSlopeCondition(1.15);

        // rocha à mostra: pedra com veios de andesito e tufo
        RuleSource rock = sequence(
                ifTrue(noiseCondition(Noises.SURFACE, 0.30, 0.55), block(Blocks.ANDESITE)),
                ifTrue(noiseCondition(Noises.CALCITE, -0.0125, 0.0125), block(Blocks.TUFF)),
                block(Blocks.STONE));

        RuleSource peaks = sequence(
                ifTrue(steep, rock),
                ifTrue(face, rock),
                ifTrue(strong, sequence(ifTrue(noiseCondition(Noises.SURFACE, -0.15, 0.3), block(Blocks.SNOW_BLOCK)), rock)),
                ifTrue(noiseCondition(Noises.PACKED_ICE, 0.0, 0.2), block(Blocks.PACKED_ICE)),
                ifTrue(noiseCondition(Noises.ICE, 0.0, 0.025), block(Blocks.BLUE_ICE)),
                ifTrue(noiseCondition(Noises.POWDER_SNOW, 0.45, 0.58), block(Blocks.POWDER_SNOW)),
                block(Blocks.SNOW_BLOCK));

        RuleSource snowline = sequence(
                ifTrue(steep, rock),
                ifTrue(face, rock),
                ifTrue(strong, sequence(ifTrue(noiseCondition(Noises.GRAVEL, -0.1, 0.2), block(Blocks.GRAVEL)), rock)),
                ifTrue(noiseCondition(Noises.SURFACE, -0.45, 0.10), block(Blocks.SNOW_BLOCK)),
                ifTrue(noiseCondition(Noises.GRAVEL, -0.12, 0.2), block(Blocks.GRAVEL)),
                rock);

        RuleSource meadows = sequence(
                ifTrue(steep, sequence(ifTrue(noiseCondition(Noises.GRAVEL, -0.1, 0.25), block(Blocks.GRAVEL)), rock)),
                ifTrue(bank, sequence(ifTrue(noiseCondition(Noises.GRAVEL, -0.1, 0.25), block(Blocks.GRAVEL)), rock)),
                ifTrue(noiseCondition(Noises.SURFACE, 0.45, 0.62), block(Blocks.COARSE_DIRT)),
                ifTrue(noiseCondition(Noises.GRAVEL, 0.1, 0.18), block(Blocks.GRAVEL)),
                block(Blocks.GRASS_BLOCK));

        RuleSource forest = sequence(
                ifTrue(steep, sequence(ifTrue(noiseCondition(Noises.SURFACE, -0.2, 0.25), block(Blocks.COARSE_DIRT)), rock)),
                ifTrue(cliff, sequence(ifTrue(noiseCondition(Noises.SURFACE, -0.2, 0.25), block(Blocks.COARSE_DIRT)), rock)),
                ifTrue(noiseCondition(Noises.SURFACE, 0.20, 0.42), block(Blocks.PODZOL)),
                ifTrue(noiseCondition(Noises.SURFACE, 0.55, 0.65), block(Blocks.COARSE_DIRT)),
                block(Blocks.GRASS_BLOCK));

        RuleSource top = sequence(
                ifTrue(above(1050), peaks),
                ifTrue(above(760), snowline),
                ifTrue(above(530), meadows),
                forest);

        // fundo de rios e lagos da montanha: cascalho
        RuleSource wet = sequence(ifTrue(above(760), rock), block(Blocks.GRAVEL));

        RuleSource under = sequence(
                ifTrue(steep, block(Blocks.STONE)),
                ifTrue(above(760), block(Blocks.STONE)),
                ifTrue(above(530), sequence(ifTrue(bank, block(Blocks.STONE)), block(Blocks.DIRT))),
                ifTrue(cliff, block(Blocks.STONE)),
                block(Blocks.DIRT));

        return ifTrue(isBiome(HornacisWorld.BIOME), sequence(
                ifTrue(onFloor, sequence(ifTrue(dry, top), wet)),
                ifTrue(underFloor, under)));
    }
}
