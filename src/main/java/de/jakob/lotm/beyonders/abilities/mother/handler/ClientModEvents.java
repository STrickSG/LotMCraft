package de.jakob.lotm.beyonders.abilities.mother.handler;

import com.mojang.blaze3d.vertex.PoseStack;
import de.jakob.lotm.LOTMCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

@EventBusSubscriber(modid = LOTMCraft.MOD_ID, value = Dist.CLIENT)
public class ClientModEvents {
    private record HybridDummy(CompoundTag tag, Entity dummy) {}
    private static final java.util.Map<LivingEntity, HybridDummy> DUMMIES = new java.util.WeakHashMap<>();

    
    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        if(event.getEntity() instanceof LivingEntity living) {
            CompoundTag persistentData = living.getPersistentData();
            
            if(persistentData.contains("HybridMobData")) {
                // antes criava uma entidade nova a cada quadro: agora a mesma é reaproveitada enquanto o híbrido não muda
                CompoundTag hybridTag = persistentData.getCompound("HybridMobData");
                HybridDummy cached = DUMMIES.get(living);
                if (cached == null || !cached.tag().equals(hybridTag) || cached.dummy().level() != living.level()) {
                    HybridMobData hybridData = HybridMobData.load(hybridTag);
                    ResourceLocation modelEntityType = hybridData.getModelEntityType();
                    EntityType<?> type = EntityType.byString(modelEntityType.toString()).orElse(null);
                    cached = new HybridDummy(hybridTag.copy(), type == null ? null : type.create(living.level()));
                    DUMMIES.put(living, cached);
                }

                if(cached.dummy() != null) {
                    Entity dummyEntity = cached.dummy();
                    
                    if(dummyEntity instanceof LivingEntity livingDummy) {
                        // Copy all rendering-relevant data
                        livingDummy.copyPosition(living);
                        livingDummy.setYRot(living.getYRot());
                        livingDummy.setXRot(living.getXRot());
                        livingDummy.yBodyRot = living.yBodyRot;
                        livingDummy.yHeadRot = living.yHeadRot;
                        livingDummy.yBodyRotO = living.yBodyRotO;
                        livingDummy.yHeadRotO = living.yHeadRotO;

                        // Get and use the original renderer
                        EntityRenderer<?> originalRenderer = Minecraft.getInstance()
                            .getEntityRenderDispatcher()
                            .getRenderer(livingDummy);

                        renderEntity((EntityRenderer) originalRenderer, livingDummy,
                                living.getYRot(),
                                event.getPartialTick(),
                                event.getPoseStack(),
                                event.getMultiBufferSource(),
                                event.getPackedLight());

                        // Cancel the original rendering
                        event.setCanceled(true);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Entity> void renderEntity(EntityRenderer<T> renderer, T entity,
                                                        float yaw, float partialTick, PoseStack poseStack,
                                                        MultiBufferSource buffer, int light) {
        renderer.render(entity, yaw, partialTick, poseStack, buffer, light);
    }

}