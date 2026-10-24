package de.jakob.lotm.network.packets.toServer;

import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.addons.rituals.AdvancementRituals;
import de.jakob.lotm.beyonders.potions.PotionRecipe;
import de.jakob.lotm.gui.custom.recipe.RecipeMenu;
import de.jakob.lotm.gui.custom.recipe.RecipeMenuProvider;
import de.jakob.lotm.beyonders.potions.PotionRecipeItem;
import de.jakob.lotm.beyonders.potions.PotionRecipeItemHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Abre o menu da receita. {@code view} diz o que mostrar: só a receita (receita de poção),
 * só o ritual (pergaminho de ritual) ou a receita e o ritual lado a lado (Carta da Blasfêmia).
 */
public record OpenRecipeMenuPacket(int sequence, String pathway, int view) implements CustomPacketPayload {
    public static final int VIEW_RECIPE = 0, VIEW_RITUAL = 1, VIEW_CARD = 2;

    public static final Type<OpenRecipeMenuPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LOTMCraft.MOD_ID, "open_recipe"));

    public static final StreamCodec<FriendlyByteBuf, OpenRecipeMenuPacket> STREAM_CODEC =
            StreamCodec.composite(
                    StreamCodec.of(FriendlyByteBuf::writeInt, FriendlyByteBuf::readInt),
                    OpenRecipeMenuPacket::sequence,
                    StreamCodec.of(FriendlyByteBuf::writeUtf, FriendlyByteBuf::readUtf),
                    OpenRecipeMenuPacket::pathway,
                    StreamCodec.of(FriendlyByteBuf::writeInt, FriendlyByteBuf::readInt),
                    OpenRecipeMenuPacket::view,
                    OpenRecipeMenuPacket::new
            );

    public OpenRecipeMenuPacket(int sequence, String pathway) {
        this(sequence, pathway, VIEW_RECIPE);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenRecipeMenuPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().getReceptionSide().isServer()) {
                ServerPlayer player = (ServerPlayer) context.player();

                int sequence = packet.sequence();
                String pathway = packet.pathway();

                boolean showRecipe = packet.view() != VIEW_RITUAL;
                AdvancementRituals.Ritual ritual = packet.view() != VIEW_RECIPE ? AdvancementRituals.get(pathway, sequence) : null;
                if (!showRecipe && ritual == null) return;

                List<ItemStack> ingredients = new ArrayList<>();
                if (showRecipe) {
                    PotionRecipeItem potionRecipeItem = PotionRecipeItemHandler.selectRecipeOfPathwayAndSequence(pathway, sequence);

                    if(potionRecipeItem == null || potionRecipeItem.getRecipe() == null) {
                        return;
                    }

                    PotionRecipe recipe = potionRecipeItem.getRecipe();

                    ingredients.add(recipe.supplementaryIngredient1());
                    ingredients.add(recipe.supplementaryIngredient2());
                    ingredients.add(recipe.supplementaryIngredient3());
                    ingredients.add(recipe.supplementaryIngredient4());
                    ingredients.add(recipe.mainIngredient());
                }

                List<ItemStack> materials = ritual != null ? ritual.materials().get() : List.of();
                RecipeMenu.Layout layout = new RecipeMenu.Layout(pathway, sequence, showRecipe, ritual != null ? materials.size() : -1);
                player.openMenu(new RecipeMenuProvider(ingredients, materials, layout), layout::write);
            }
        });
    }

}
