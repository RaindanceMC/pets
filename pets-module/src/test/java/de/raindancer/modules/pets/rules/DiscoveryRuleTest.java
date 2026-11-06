package de.raindancer.modules.pets.rules;

import de.raindancer.modules.pets.model.Rarity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("which species a player knows of")
class DiscoveryRuleTest {

    private final DiscoveryRule rule = new DiscoveryRule();

    @Test
    @DisplayName("common pets are known to everybody")
    void common() {
        assertThat(rule.known(Rarity.COMMON, new DiscoveryRule.Facts(false, false, false))).isTrue();
    }

    @Test
    @DisplayName("anything rarer is a secret until it was seen, is owned or could be taken")
    void rarer() {
        for (Rarity rarity : new Rarity[]{Rarity.UNCOMMON, Rarity.RARE, Rarity.EPIC, Rarity.LEGENDARY, Rarity.MYTHIC}) {
            assertThat(rule.known(rarity, new DiscoveryRule.Facts(false, false, false))).as(rarity.name()).isFalse();
            assertThat(rule.known(rarity, new DiscoveryRule.Facts(true, false, false))).isTrue();
            assertThat(rule.known(rarity, new DiscoveryRule.Facts(false, true, false))).isTrue();
            assertThat(rule.known(rarity, new DiscoveryRule.Facts(false, false, true))).isTrue();
        }
    }
}
