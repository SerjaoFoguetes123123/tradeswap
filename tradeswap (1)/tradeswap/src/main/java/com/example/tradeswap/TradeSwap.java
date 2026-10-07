package com.example.tradeswap;

import java.util.Optional;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffers;

/**
 * Shift + botao direito em um villager segurando um bloco de trabalho
 * (lectern, composter, smithing table...):
 *  - define a profissao correspondente ao bloco
 *  - volta para nivel 1 / 0 XP
 *  - apaga as ofertas, que sao geradas de novo na proxima vez que abrir o comercio
 *  - consome 1 bloco (exceto no criativo)
 */
public class TradeSwap implements ModInitializer {
    public static final String MOD_ID = "tradeswap";

    @Override
    public void onInitialize() {
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (!(entity instanceof Villager villager)) return InteractionResult.PASS;
            if (!player.isShiftKeyDown() || player.isSpectator()) return InteractionResult.PASS;

            ItemStack stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof BlockItem blockItem)) return InteractionResult.PASS;

            Optional<Holder<PoiType>> poi = PoiTypes.forState(blockItem.getBlock().defaultBlockState());
            if (poi.isEmpty()) return InteractionResult.PASS;

            Holder<VillagerProfession> profession = findProfession(poi.get());
            if (profession == null) return InteractionResult.PASS;

            // Bebes e nitwits nao negociam
            if (villager.isBaby() || villager.getVillagerData().profession().is(VillagerProfession.NITWIT)) {
                return InteractionResult.PASS;
            }

            // O evento roda nos dois lados; so o servidor altera o villager.
            if (level.isClientSide()) return InteractionResult.SUCCESS;

            villager.setVillagerData(
                    villager.getVillagerData().withProfession(profession).withLevel(1));
            villager.setVillagerXp(0);
            villager.overrideOffers((MerchantOffers) null); // regenera ao abrir o comercio

            villager.playSound(SoundEvents.VILLAGER_YES, 1.0F, 1.0F);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        });
    }

    private static Holder<VillagerProfession> findProfession(Holder<PoiType> poi) {
        return BuiltInRegistries.VILLAGER_PROFESSION.listElements()
                .filter(ref -> ref.value().heldJobSite().test(poi))
                .map(ref -> (Holder<VillagerProfession>) ref)
                .findFirst()
                .orElse(null);
    }
}
