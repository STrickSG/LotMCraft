package de.jakob.lotm.events;

import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.beyonders.abilities.core.PassiveAbilityHandler;
import de.jakob.lotm.beyonders.abilities.core.PassiveAbility;
import de.jakob.lotm.beyonders.abilities.core.PhysicalEnhancementsAbility;
import de.jakob.lotm.beyonders.abilities.core.Ability;
import de.jakob.lotm.beyonders.abilities.core.ToggleAbility;
import de.jakob.lotm.beyonders.abilities.door.passives.VoidImmunityAbility;
import de.jakob.lotm.beyonders.abilities.wheel_of_fortune.passives.PassiveLuckAbility;
import de.jakob.lotm.attachments.*;
import de.jakob.lotm.effect.FoolingEffect;
import de.jakob.lotm.effect.ModEffects;
import de.jakob.lotm.network.packets.toClient.SyncBeyonderDataPacket;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import de.jakob.lotm.item.ModItems;
import de.jakob.lotm.item.custom.SubordinateControllerItem;
import de.jakob.lotm.network.PacketHandler;
import de.jakob.lotm.network.packets.toClient.SyncOnHoldAbilityPacket;
import de.jakob.lotm.network.packets.toClient.SyncToggleAbilityPacket;
import de.jakob.lotm.util.BeyonderData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static de.jakob.lotm.util.BeyonderData.getMaxWormAmount;

@EventBusSubscriber(modid = LOTMCraft.MOD_ID)
public class BeyonderDataTickHandler {

    private static final Set<PassiveAbility> passiveAbilities = ConcurrentHashMap.newKeySet();


    private static final Map<UUID, Set<PassiveAbility>> cachedAbilities = new ConcurrentHashMap<>();

    public static void invalidateCache(LivingEntity entity) {
        cachedAbilities.remove(entity.getUUID());
        // Without this, PassiveAbility#shouldApplyTo keeps answering from its own cache for up to
        // 20 ticks, so the recomputed set (and setBeyonder's onPassiveAbilityGained calls) used stale data.
        PassiveAbility.invalidateCache(entity);
    }

    private static final Object INIT_LOCK = new Object();

    private static Set<PassiveAbility> getApplicableAbilities(LivingEntity entity) {
        if (passiveAbilities.isEmpty()) {
            synchronized (INIT_LOCK) {
                if (passiveAbilities.isEmpty()) {
                    List<PassiveAbility> abilities = PassiveAbilityHandler.passiveAbilities.stream().toList();
                    passiveAbilities.addAll(abilities);
                }
            }
        }

        return cachedAbilities.computeIfAbsent(entity.getUUID(), k ->
                passiveAbilities.stream()
                        .filter(a -> a.shouldApplyTo(entity))
                        .collect(Collectors.toSet())
        );
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();

        if(!(entity instanceof LivingEntity livingEntity)) {
            return;
        }

        boolean clientSide = livingEntity.level().isClientSide;

        // This runs for every living entity in the world, so components are only read when present:
        // getData() would attach a default copy to every mob, which then also gets saved with the chunk.
        // A missing component behaves exactly like a default one in all checks below.

        // Tick cooldowns
        if(livingEntity.hasData(ModAttachments.COOLDOWN_COMPONENT)) {
            livingEntity.getData(ModAttachments.COOLDOWN_COMPONENT).tick();
        }

        //Virtual Personas heal
        if(livingEntity instanceof ServerPlayer player) {
            VirtualPersonaComponent virtualPersonaComponent = player.getData(ModAttachments.VIRTUAL_PERSONAS);
            virtualPersonaComponent.heal(player);
        }

        // Tick flight cooldown
        if(livingEntity.hasData(ModAttachments.FLIGHT_DISABLE_COMPONENT)) {
            DisabledFlightComponent disabledFlightComponent = livingEntity.getData(ModAttachments.FLIGHT_DISABLE_COMPONENT);
            if(disabledFlightComponent.getCooldownTicks() > 0) {
                disabledFlightComponent.setCooldownTicks(disabledFlightComponent.getCooldownTicks() - 1);
            }
        }

        if (!clientSide) {
            FoolingComponent foolingComponent = livingEntity.hasData(ModAttachments.FOOLING_COMPONENT)
                    ? livingEntity.getData(ModAttachments.FOOLING_COMPONENT) : null;
            if (foolingComponent != null && foolingComponent.isFooled()) {
                if (foolingComponent.getTicksRemaining() % FoolingEffect.STUN_INTERVAL_TICKS == 0) {
                    foolingComponent.applyStun(FoolingEffect.STUN_DURATION_TICKS);
                }

                if (foolingComponent.isStunned()) {
                    livingEntity.setDeltaMovement(0, 0, 0);
                    livingEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 2, 254, false, false, false));
                    livingEntity.hurtMarked = true;
                }

                foolingComponent.tick();
                livingEntity.addEffect(new MobEffectInstance(ModEffects.FOOLING, foolingComponent.getTicksRemaining(), 0, false, true, true));
            } else if (livingEntity.hasEffect(ModEffects.FOOLING)) {
                livingEntity.removeEffect(ModEffects.FOOLING);
            }
        }

        if(entity.hasData(ModAttachments.SANITY_COMPONENT) && entity.getData(ModAttachments.SANITY_COMPONENT.get()).getSanity() == 0.0f){
            entity.kill();
        }

        if(BeyonderData.isBeyonder(livingEntity)) {

            if(entity.tickCount % 20 == 0){
                entity.getData(ModAttachments.REGEN_DISABLER.get()).incrementCount();
            }

            // Server only: the enhancement maps are static and keyed by UUID, so in singleplayer the
            // client copy of the player used to wipe the server's entries on its own schedule.
            if(!clientSide && entity.tickCount % 200 == 0) {
                invalidateCache(livingEntity);
                PhysicalEnhancementsAbility.resetEnhancements(event.getEntity().getUUID(), livingEntity, false);
                invalidateCache(livingEntity);
            }

            if(entity.tickCount % getWormRecoverTime(livingEntity) == 0) {
                BeyonderData.incrementWormAmount(livingEntity, 1);
            }

            // Tick Passive Abilities, and onHold for currently selected Ability and tick luck
            if(entity.tickCount % 5 == 0) {
                tickPassiveAbilities(livingEntity);

                // Remove Unluck gradually
                LuckComponent luckComponent = livingEntity.getData(ModAttachments.LUCK_COMPONENT);
                if(luckComponent.getLuck() < 0) {
                    luckComponent.addLuckWithMax(1, 0);
                }

                // Remove Luck gradually
                if(luckComponent.getLuck() > PassiveLuckAbility.getNormalLuckForEntity(livingEntity)) {
                    luckComponent.addLuckWithMin(-1, PassiveLuckAbility.getNormalLuckForEntity(livingEntity));
                }
            }

            tickOnHold(livingEntity, entity.tickCount);

            // Tick Toggle Abilities (server only; clients tick them through SyncToggleAbilityPacket TICK.
            // In singleplayer the client thread used to read the server's map here and tick them twice.)
            if(!clientSide) {
                ToggleAbility.getActiveAbilitiesForEntity(livingEntity).forEach(toggleAbility -> {
                    if(entity.tickCount % toggleAbility.tickRate != 0) {
                        return;
                    }
                    toggleAbility.prepareTick(livingEntity.level(), livingEntity);
                    PacketHandler.sendToTrackingAndSelf(livingEntity, new SyncToggleAbilityPacket(livingEntity.getId(), toggleAbility.getId(), SyncToggleAbilityPacket.Action.TICK.getValue()));
                });
            }
        }
        // Periodic sync. Packets can only be sent from the server, and entities without a beyonder
        // component have never been beyonders, so the client already assumes the right defaults for them.
        if(!clientSide && entity.tickCount % 20 == 0 && livingEntity.hasData(ModAttachments.BEYONDER_COMPONENT)) {
            // Non-beyonder mobs only need the occasional refresh; this used to send one packet per mob per second
            if(entity instanceof Player || BeyonderData.isBeyonder(livingEntity) || entity.tickCount % 200 == 0) {
                BeyonderComponent beyonderComponent = entity.getData(ModAttachments.BEYONDER_COMPONENT);
                SyncBeyonderDataPacket packet = new SyncBeyonderDataPacket(entity.getUUID(), BeyonderData.getPathway(livingEntity), BeyonderData.getSequence(livingEntity), beyonderComponent.getSpirituality(), false, 0.0f, beyonderComponent.getPathwayHistory(), beyonderComponent.getCharacteristicStack(), getMaxWormAmount(BeyonderData.getSequence(livingEntity)));
                if(entity instanceof ServerPlayer serverPlayer) {
                    // Other players keep receiving the same data as before. The player's own client gets the full
                    // data instead: this packet carries griefing=false / digestion=0, which used to overwrite
                    // their HUD values every second until the next per-tick spirituality sync fixed them.
                    PacketHandler.sendToTracking(serverPlayer, packet);
                    PacketHandler.syncBeyonderDataToPlayer(serverPlayer);
                }
                else {
                    PacketHandler.sendToTrackingAndSelf(entity, packet);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        // Drop per-entity caches of entities that are gone for good (killed / discarded mobs),
        // otherwise these maps grow for every beyonder mob that ever existed.
        Entity entity = event.getEntity();
        if(!(entity instanceof LivingEntity livingEntity) || entity instanceof Player) {
            return;
        }
        Entity.RemovalReason reason = entity.getRemovalReason();
        if(reason != null && reason.shouldDestroy()) {
            invalidateCache(livingEntity);
        }
    }

    private static int getWormRecoverTime(LivingEntity entity) {
        return switch (BeyonderData.getPathway(entity)) {
            case "fool" -> 20 * 60 * 5;
            default -> 20 * 30;
        };
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        if (player.level().isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if(player.getY() < -200 && !VoidImmunityAbility.IMMUNE_ENTITIES.contains(player)) {
            player.kill();
        }

        if (BeyonderData.isBeyonder(player)) {
            // Regenerate Spirituality
            float amount = BeyonderData.getMaxSpirituality(BeyonderData.getPathway(player, true),
                    BeyonderData.getSequence(player, false, true), player) * 0.0006f;

            var disabler = player.getData(ModAttachments.REGEN_DISABLER.get());
            if(disabler.isDisabled())
                amount /= 2;

            BeyonderData.incrementSpirituality(player, amount);

            // Slowly digest potion
            if(player.tickCount % 20 == 0) {
                BeyonderData.digest(player, 1 / (20 * 60 * 60f), true);
            }
        }

        // Tick special items
        if(player.tickCount % 5 == 0) {
            if(player.getMainHandItem().is(ModItems.SUBORDINATE_CONTROLLER.get()) && player.getMainHandItem().getItem() instanceof SubordinateControllerItem) {
                SubordinateControllerItem.onHold(player, player.getMainHandItem());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        PhysicalEnhancementsAbility.resetEnhancements(event.getEntity().getUUID(), event.getEntity(), true);
        invalidateCache(event.getEntity());
    }

    private static void tickPassiveAbilities(LivingEntity entity) {
        if(entity.level().isClientSide) return;

        getApplicableAbilities(entity).forEach(abilityItem -> {
            abilityItem.tick(entity.level(), entity);
        });
    }

    private static void tickOnHold(LivingEntity entity, int tick) {
        if(entity instanceof ServerPlayer player) {
            AbilityWheelComponent component = player.getData(ModAttachments.ABILITY_WHEEL_COMPONENT);
            if(component.getSelectedAbility() < 0 || component.getSelectedAbility() >= component.getAbilities().size()) {
                return;
            }

            String abilityId = component.getAbilities().get(component.getSelectedAbility()).split(":")[0];
            Ability ability = LOTMCraft.abilityHandler.getById(abilityId);
            if(ability != null && tick % ability.onHoldTickInverval == 0) {
                ability.onHold(player.serverLevel(), player);
                PacketHandler.sendToTrackingAndSelf(player, new SyncOnHoldAbilityPacket(player.getId(), abilityId));
            }
        }
    }

    @SubscribeEvent
    public static void disableRegen(LivingDamageEvent.Pre event) {
        var entity = event.getEntity();
        if(!BeyonderData.isBeyonder(entity)) return;

        int duration = 10;

        if(BeyonderData.getPathway(entity).equals("mother")) duration = 2;

        entity.getData(ModAttachments.REGEN_DISABLER.get()).disableFor(duration);

        if (entity.hasEffect(MobEffects.REGENERATION)){
            entity.removeEffect(MobEffects.REGENERATION);
        }
    }
}