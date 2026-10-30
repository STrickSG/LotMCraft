package de.jakob.lotm.beyonders.singularity;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Guarda quem está com cada Singularidade em forma de item, para ela não reaparecer
 * no mundo enquanto alguém a possui (mesmo offline).
 */
public class SingularityRegistry extends SavedData {
    private static final String NAME = "lotmcraft_singularities";
    public static final SavedData.Factory<SingularityRegistry> FACTORY =
            new SavedData.Factory<>(SingularityRegistry::new, SingularityRegistry::load, null);

    private final Map<String, UUID> holders = new HashMap<>();

    public static SingularityRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    @Nullable
    public UUID getHolder(String pathway) {
        return holders.get(pathway);
    }

    public boolean isHeld(String pathway) {
        return holders.containsKey(pathway);
    }

    public void setHolder(String pathway, UUID holder) {
        holders.put(pathway, holder);
        setDirty();
    }

    public void clear(String pathway) {
        if (holders.remove(pathway) != null) setDirty();
    }

    private static SingularityRegistry load(CompoundTag tag, HolderLookup.Provider provider) {
        SingularityRegistry registry = new SingularityRegistry();
        CompoundTag map = tag.getCompound("holders");
        for (String pathway : map.getAllKeys()) {
            if (map.hasUUID(pathway)) registry.holders.put(pathway, map.getUUID(pathway));
        }
        return registry;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
        CompoundTag map = new CompoundTag();
        holders.forEach(map::putUUID);
        tag.put("holders", map);
        return tag;
    }
}
