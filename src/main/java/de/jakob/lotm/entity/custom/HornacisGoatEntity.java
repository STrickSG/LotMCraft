package de.jakob.lotm.entity.custom;

import de.jakob.lotm.entity.ModEntities;
import de.jakob.lotm.item.ModIngredients;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Cabra Montanhosa Cinzenta de Hornacis: vive na Cordilheira de Hornacis. Faz tudo o que a cabra comum faz
 * (escala, salta longe, dá cabeçadas), mas tem um chifre só, no meio da testa. O chifre da cabra adulta é o
 * ingrediente principal da poção do Palhaço (Bobo, Sequência 8): ela o solta ao morrer, ou quando o quebra
 * dando uma cabeçada numa pedra, num tronco ou num minério, como a cabra comum.
 */
public class HornacisGoatEntity extends Goat {
    public HornacisGoatEntity(EntityType<? extends Goat> type, Level level) {
        super(type, level);
    }

    /** Um chifre só: ele está lá enquanto qualquer um dos dois "chifres" da cabra comum estiver. */
    public boolean hasHorn() {
        return hasLeftHorn() || hasRightHorn();
    }

    @Override
    public ItemStack createHorn() {
        return new ItemStack(ModIngredients.HORNACIS_GRAY_MOUNTAIN_GOAT_HORN.get());
    }

    @Override
    public boolean dropHorn() {
        if (!hasHorn()) return false;
        removeHorns();
        Vec3 pos = position();
        level().addFreshEntity(new ItemEntity(level(), pos.x, pos.y, pos.z, createHorn(),
                Mth.randomBetween(random, -0.2F, 0.2F), Mth.randomBetween(random, 0.3F, 0.7F), Mth.randomBetween(random, -0.2F, 0.2F)));
        return true;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);
        if (!isBaby() && hasHorn()) spawnAtLocation(createHorn());
    }

    @Override
    public Goat getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        HornacisGoatEntity kid = ModEntities.HORNACIS_GRAY_MOUNTAIN_GOAT.get().create(level);
        if (kid != null) {
            RandomSource random = level.getRandom();
            // as mesmas esperas da cabra comum antes do primeiro salto longo e da primeira cabeçada
            kid.getBrain().setMemory(MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS, UniformInt.of(600, 1200).sample(random));
            kid.getBrain().setMemory(MemoryModuleType.RAM_COOLDOWN_TICKS, UniformInt.of(600, 6000).sample(random));
            AgeableMob parent = random.nextBoolean() ? this : otherParent;
            kid.setScreamingGoat(parent instanceof Goat goat && goat.isScreamingGoat() || random.nextDouble() < 0.02);
        }
        return kid;
    }
}
