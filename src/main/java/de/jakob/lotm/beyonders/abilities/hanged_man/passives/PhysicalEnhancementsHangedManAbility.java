package de.jakob.lotm.beyonders.abilities.hanged_man.passives;

import de.jakob.lotm.beyonders.abilities.core.PhysicalEnhancementsAbility;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PhysicalEnhancementsHangedManAbility extends PhysicalEnhancementsAbility {

    public PhysicalEnhancementsHangedManAbility(String id) {
        super(id);
    }

    @Override
    public Map<String, Integer> getRequirements() {
        return new HashMap<>(Map.of("hanged_man", 9));
    }

    @Override
    public List<PhysicalEnhancement> getEnhancements() {
        return List.of();
    }

    @Override
    protected List<PhysicalEnhancement> getEnhancementsForSequence(int sequenceLevel) {
        return switch (sequenceLevel) {
            case 9 -> List.of(
                    new PhysicalEnhancement(EnhancementType.HEALTH, 2)
            );
            case 8 -> List.of(
                    new PhysicalEnhancement(EnhancementType.SPEED, 1),
                    new PhysicalEnhancement(EnhancementType.HEALTH, 4)
            );
            case 7 -> List.of(
                    new PhysicalEnhancement(EnhancementType.STRENGTH, 1),
                    new PhysicalEnhancement(EnhancementType.SPEED, 2),
                    new PhysicalEnhancement(EnhancementType.HEALTH, 6)
            );
            default -> List.of();
        };
    }
}
