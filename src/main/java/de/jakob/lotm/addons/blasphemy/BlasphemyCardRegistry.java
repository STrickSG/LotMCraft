package de.jakob.lotm.addons.blasphemy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Só existe uma Carta da Blasfêmia de cada Caminho no mundo. Aqui fica onde ela está:
 * nenhuma (pode aparecer num altar), num altar, ou fora do altar (com alguém, num baú, no chão...).
 */
public class BlasphemyCardRegistry extends SavedData {
    public enum State { NONE, ALTAR, OUT }

    public record Entry(State state, ResourceKey<Level> dimension, BlockPos pos, UUID uid) {
        static final Entry NONE = new Entry(State.NONE, null, null, null);
    }

    private static final String NAME = "lotmcraft_blasphemy_cards";
    private static final Factory<BlasphemyCardRegistry> FACTORY = new Factory<>(BlasphemyCardRegistry::new, BlasphemyCardRegistry::load, null);
    private final Map<String, Entry> cards = new HashMap<>();

    public static BlasphemyCardRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    public Entry get(String pathway) {
        return cards.getOrDefault(pathway, Entry.NONE);
    }

    /** A carta aparece neste altar; ganha uma identidade nova. */
    public UUID placeAtAltar(String pathway, ResourceKey<Level> dimension, BlockPos pos) {
        UUID uid = UUID.randomUUID();
        cards.put(pathway, new Entry(State.ALTAR, dimension, pos.immutable(), uid));
        setDirty();
        return uid;
    }

    /** A carta parada num altar vai para outro altar (continua sendo a mesma carta). */
    public void moveTo(String pathway, ResourceKey<Level> dimension, BlockPos pos) {
        Entry e = get(pathway);
        if (e.state() == State.NONE) {
            placeAtAltar(pathway, dimension, pos);
            return;
        }
        cards.put(pathway, new Entry(State.ALTAR, dimension, pos.immutable(), e.uid()));
        setDirty();
    }

    /** A carta saiu do altar (alguém pegou ou o altar quebrou). */
    public void takeOut(String pathway) {
        Entry e = get(pathway);
        cards.put(pathway, new Entry(State.OUT, null, null, e.uid()));
        setDirty();
    }

    /** A carta foi destruída: pode reaparecer em outro altar. Só vale para a carta verdadeira. */
    public void lost(String pathway, UUID uid) {
        Entry e = get(pathway);
        if (e.uid() != null && e.uid().equals(uid)) {
            cards.remove(pathway);
            setDirty();
        }
    }

    private static BlasphemyCardRegistry load(CompoundTag tag, HolderLookup.Provider provider) {
        BlasphemyCardRegistry registry = new BlasphemyCardRegistry();
        for (String pathway : tag.getAllKeys()) {
            CompoundTag c = tag.getCompound(pathway);
            State state = State.valueOf(c.getString("state"));
            ResourceKey<Level> dim = c.contains("dim") ? ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(c.getString("dim"))) : null;
            BlockPos pos = c.contains("pos") ? NbtUtils.readBlockPos(c, "pos").orElse(null) : null;
            registry.cards.put(pathway, new Entry(state, dim, pos, c.hasUUID("uid") ? c.getUUID("uid") : null));
        }
        return registry;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
        cards.forEach((pathway, e) -> {
            CompoundTag c = new CompoundTag();
            c.putString("state", e.state().name());
            if (e.dimension() != null) c.putString("dim", e.dimension().location().toString());
            if (e.pos() != null) c.put("pos", NbtUtils.writeBlockPos(e.pos()));
            if (e.uid() != null) c.putUUID("uid", e.uid());
            tag.put(pathway, c);
        });
        return tag;
    }
}
