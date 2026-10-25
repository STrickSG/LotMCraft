package de.jakob.lotm.addons.rituals;

import de.jakob.lotm.LOTMCraft;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Frasco de Emoções do ritual do Sem Sombra (Sol, Sequência 4): um frasco do próprio Minecraft,
 * com líquido dourado, que guarda as emoções mais fortes de um jogador até o avanço.
 */
@EventBusSubscriber(modid = LOTMCraft.MOD_ID)
public final class EmotionFlask {
    private static final String OWNER = "EmotionOwner", OWNER_NAME = "EmotionOwnerName";
    private static final int COLOR = 0xF2C14E;

    private EmotionFlask() {}

    /** Frasco cheio com as emoções deste jogador */
    public static ItemStack of(Player player) {
        ItemStack flask = base(Component.translatable("item.lotmcraft.emotion_flask.owned", player.getName().getString()));
        CompoundTag tag = new CompoundTag();
        tag.putUUID(OWNER, player.getUUID());
        tag.putString(OWNER_NAME, player.getName().getString());
        flask.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return flask;
    }

    /** Frasco sem dono, só para mostrar no menu do ritual */
    public static ItemStack example() {
        return base(Component.translatable("item.lotmcraft.emotion_flask"));
    }

    private static ItemStack base(Component name) {
        ItemStack flask = new ItemStack(Items.POTION);
        flask.set(DataComponents.ITEM_NAME, name);
        flask.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), Optional.of(COLOR), List.of()));
        flask.set(DataComponents.LORE, new ItemLore(List.of(Component.translatable("item.lotmcraft.emotion_flask.lore").withStyle(ChatFormatting.GRAY))));
        flask.set(DataComponents.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
        return flask;
    }

    /** De quem são as emoções guardadas no frasco (null se não for um Frasco de Emoções) */
    public static UUID ownerOf(ItemStack stack) {
        if (!stack.is(Items.POTION)) return null;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.hasUUID(OWNER) ? tag.getUUID(OWNER) : null;
    }

    // Agachado + clique direito com um frasco de vidro vazio (sem mirar na água): guarda as emoções mais fortes nele
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.GLASS_BOTTLE) || !player.isShiftKeyDown() || aimingAtWater(player)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide));
        if (!(player.level() instanceof ServerLevel level)) return;

        if (!player.getAbilities().instabuild) stack.shrink(1);
        ItemStack flask = of(player);
        if (!player.getInventory().add(flask)) player.drop(flask, false);
        level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY(1.1), player.getZ(), 30, 0.4, 0.4, 0.4, 0.6);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 0.7f);
        player.displayClientMessage(Component.translatable("item.lotmcraft.emotion_flask.filled").withColor(0xFFE29A), true);
    }

    // o frasco não é para beber: as emoções só voltam durante o avanço
    @SubscribeEvent
    public static void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (ownerOf(event.getItem()) != null) event.setCanceled(true);
    }

    private static boolean aimingAtWater(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(player.blockInteractionRange()));
        BlockHitResult hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY, player));
        return hit.getType() == HitResult.Type.BLOCK && player.level().getFluidState(hit.getBlockPos()).is(FluidTags.WATER);
    }
}
