package de.jakob.lotm.block.entity;

import de.jakob.lotm.addons.blasphemy.BlasphemyCardRegistry;
import de.jakob.lotm.addons.blasphemy.BlasphemyCards;
import de.jakob.lotm.block.ModBlockEntities;
import de.jakob.lotm.util.BeyonderData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Altar da Blasfêmia: guarda a carta única de um Caminho (definido na estrutura).
 * Só recebe a carta se ela não existir em nenhum outro lugar do mundo.
 * Quem é do mesmo Caminho sente a carta de longe, como na Convergência.
 */
public class BlasphemyAltarBlockEntity extends BlockEntity {
    public static final int SENSE_RADIUS = 100;
    private static final int SENSE_INTERVAL = 20 * 20;

    private String pathway = "";
    private boolean hasCard = false;
    private final Map<UUID, Long> lastSensed = new HashMap<>();

    public BlasphemyAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BLASPHEMY_ALTAR_BE.get(), pos, state);
    }

    public String getPathway() { return pathway; }
    public boolean hasCard() { return hasCard; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BlasphemyAltarBlockEntity altar) {
        if (!(level instanceof ServerLevel server) || altar.pathway.isEmpty() || level.getGameTime() % 40 != 0) return;
        BlasphemyCardRegistry registry = BlasphemyCardRegistry.get(server.getServer());
        BlasphemyCardRegistry.Entry entry = registry.get(altar.pathway);
        boolean here = entry.state() == BlasphemyCardRegistry.State.ALTAR && pos.equals(entry.pos()) && level.dimension().equals(entry.dimension());

        if (altar.hasCard && !here) altar.setHasCard(false);
        // a carta não existe em lugar nenhum: ela se manifesta neste altar
        if (!altar.hasCard && entry.state() == BlasphemyCardRegistry.State.NONE) {
            registry.placeAtAltar(altar.pathway, level.dimension(), pos);
            altar.setHasCard(true);
        }
        if (altar.hasCard) altar.senseNearby(server);
    }

    // só quem é do Caminho da carta sente: mensagens cada vez mais fortes ao se aproximar
    private void senseNearby(ServerLevel level) {
        long now = level.getGameTime();
        int color = BeyonderData.pathwayInfos.containsKey(pathway) ? BeyonderData.pathwayInfos.get(pathway).color() : 0xFFFFFF;
        for (ServerPlayer player : level.players()) {
            if (!BeyonderData.isBeyonder(player) || !pathway.equals(BeyonderData.getPathway(player))) continue;
            double dist = Math.sqrt(player.distanceToSqr(worldPosition.getCenter()));
            if (dist > SENSE_RADIUS) continue;
            Long last = lastSensed.get(player.getUUID());
            int interval = dist < 25 ? SENSE_INTERVAL / 4 : SENSE_INTERVAL;
            if (last != null && now - last < interval) continue;
            lastSensed.put(player.getUUID(), now);
            String key = dist < 25 ? "near" : dist < 60 ? "closer" : "far";
            player.displayClientMessage(Component.translatable("lotmcraft.blasphemy_card.sense." + key).withColor(color), true);
            if (dist < 25) level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.AMBIENT, 0.6f, 0.6f);
        }
    }

    /** Clique direito: pega a carta. */
    public void take(Player player) {
        if (!(level instanceof ServerLevel server) || !hasCard) return;
        BlasphemyCardRegistry registry = BlasphemyCardRegistry.get(server.getServer());
        ItemStack card = BlasphemyCards.trueCard(pathway, registry.get(pathway).uid());
        registry.takeOut(pathway);
        setHasCard(false);
        if (!player.getInventory().add(card)) player.drop(card, false);
        server.playSound(null, worldPosition, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1f, 0.6f);
        server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, worldPosition.getX() + .5, worldPosition.getY() + 1.4, worldPosition.getZ() + .5, 30, .3, .4, .3, .02);
    }

    /** O altar foi quebrado com a carta: ela cai no chão (e continua sendo a única). */
    public ItemStack releaseCard() {
        if (!(level instanceof ServerLevel server) || !hasCard) return ItemStack.EMPTY;
        BlasphemyCardRegistry registry = BlasphemyCardRegistry.get(server.getServer());
        ItemStack card = BlasphemyCards.trueCard(pathway, registry.get(pathway).uid());
        registry.takeOut(pathway);
        hasCard = false;
        return card;
    }

    private void setHasCard(boolean value) {
        hasCard = value;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("pathway", pathway);
        tag.putBoolean("has_card", hasCard);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        pathway = tag.getString("pathway");
        hasCard = tag.getBoolean("has_card");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
