package de.jakob.lotm.beyonders.blasphemy;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Only one Card of Blasphemy of each pathway may exist in a world. This remembers the identity of the
 * card that currently exists for each pathway; any other card of that pathway is a copy and vanishes.
 */
public class BlasphemyCardRegistry extends SavedData {
    private static final String NAME = "lotmcraft_blasphemy_cards";
    private static final Factory<BlasphemyCardRegistry> FACTORY = new Factory<>(BlasphemyCardRegistry::new, BlasphemyCardRegistry::load, null);

    private final Map<String, UUID> cards = new HashMap<>();

    public static BlasphemyCardRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    /** The identity of the existing card of this pathway, or null if there is none. */
    public UUID get(String pathway) {
        return cards.get(pathway);
    }

    /** Registers a new card for the pathway. Returns false if the pathway already has one. */
    public boolean claim(String pathway, UUID uid) {
        if (cards.containsKey(pathway)) return false;
        cards.put(pathway, uid);
        setDirty();
        return true;
    }

    /** The card was destroyed: a new one can be crafted. Only the genuine card frees the pathway. */
    public void lost(String pathway, UUID uid) {
        if (uid.equals(cards.get(pathway))) {
            cards.remove(pathway);
            setDirty();
        }
    }

    private static BlasphemyCardRegistry load(CompoundTag tag, HolderLookup.Provider provider) {
        BlasphemyCardRegistry registry = new BlasphemyCardRegistry();
        for (String pathway : tag.getAllKeys()) {
            if (tag.hasUUID(pathway)) registry.cards.put(pathway, tag.getUUID(pathway));
        }
        return registry;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
        cards.forEach(tag::putUUID);
        return tag;
    }
}
