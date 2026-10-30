package de.jakob.lotm.hornacis;

/**
 * Marca a fonte de biomas do Overworld com a Cordilheira de Hornacis do mundo aberto. Fica num campo da
 * própria fonte, então vale também para as cópias que o TerraBlender faz dela a cada chunk.
 */
public interface HornacisSource {
    HornacisRegion lotmcraft$hornacis();

    void lotmcraft$setHornacis(HornacisRegion region);
}
