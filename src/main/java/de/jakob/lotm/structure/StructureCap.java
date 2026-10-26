package de.jakob.lotm.structure;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Estruturas com limite por mundo (ex.: no máximo 12 Altares Solares): guarda onde cada uma já foi criada.
 * A geração de chunks roda em várias threads, então tudo passa por um lock.
 */
public class StructureCap extends SavedData {
    private static final String NAME = "lotmcraft_structure_caps";
    private static final Object LOCK = new Object();
    private final Map<String, List<ChunkPos>> created = new HashMap<>();

    @Nullable
    private static StructureCap get() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(StructureCap::new, StructureCap::load, null), NAME);
    }

    /** Ainda cabe mais uma desta estrutura neste mundo? */
    public static boolean hasRoom(ResourceLocation structure, int max) {
        synchronized (LOCK) {
            StructureCap cap = get();
            return cap == null || cap.created.getOrDefault(structure.toString(), List.of()).size() < max;
        }
    }

    /** Reserva um lugar para a estrutura neste chunk; falso se o limite já foi alcançado. */
    public static boolean claim(ResourceLocation structure, int max, ChunkPos pos) {
        synchronized (LOCK) {
            StructureCap cap = get();
            if (cap == null) return true;
            List<ChunkPos> list = cap.created.computeIfAbsent(structure.toString(), k -> new ArrayList<>());
            if (list.contains(pos)) return true;
            if (list.size() >= max) return false;
            list.add(pos);
            cap.setDirty();
            return true;
        }
    }

    /** Onde as estruturas deste tipo já foram criadas neste mundo. */
    public static List<ChunkPos> created(ResourceLocation structure) {
        synchronized (LOCK) {
            StructureCap cap = get();
            return cap == null ? List.of() : List.copyOf(cap.created.getOrDefault(structure.toString(), List.of()));
        }
    }

    private static StructureCap load(CompoundTag tag, HolderLookup.Provider provider) {
        StructureCap cap = new StructureCap();
        for (String key : tag.getAllKeys()) {
            List<ChunkPos> list = new ArrayList<>();
            for (Tag t : tag.getList(key, Tag.TAG_LONG)) list.add(new ChunkPos(((LongTag) t).getAsLong()));
            cap.created.put(key, list);
        }
        return cap;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
        created.forEach((key, list) -> {
            ListTag l = new ListTag();
            list.forEach(p -> l.add(LongTag.valueOf(p.toLong())));
            tag.put(key, l);
        });
        return tag;
    }
}
