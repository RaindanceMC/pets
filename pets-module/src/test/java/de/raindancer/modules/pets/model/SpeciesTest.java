package de.raindancer.modules.pets.model;

import org.bukkit.Material;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("how big a pet is")
class SpeciesTest {

    private static Species sized(double size) {
        return new Species("x", "X", "WOLF", Role.FIGHTER, Rarity.COMMON, Material.BONE, 10, 2, 0.3, size, false,
                false, Unlock.free(), "", "SNOWBALL", "");
    }

    @Test
    @DisplayName("sizes stay small: between 0.15 and 0.6 of a player")
    void bounded() {
        assertThat(sized(5).size()).isEqualTo(Species.LARGEST);
        assertThat(sized(0.01).size()).isEqualTo(Species.SMALLEST);
    }
}
