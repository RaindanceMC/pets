package de.raindancer.modules.pets.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("how a pet goes about a fight")
class TacticTest {

    @Test
    @DisplayName("a new pet fights balanced; the owner cycles through every tactic")
    void cycling() {
        Pet rex = Pet.adopt(UUID.randomUUID(), "wolf", "Rex", Temperament.of(Personality.CHEEKY), Instant.EPOCH);
        assertThat(rex.tactic()).isEqualTo(Tactic.BALANCED);
        Tactic at = Tactic.BALANCED;
        java.util.Set<Tactic> seen = new java.util.HashSet<>();
        for (int i = 0; i < Tactic.values().length; i++) {
            seen.add(at);
            at = at.next();
        }
        assertThat(seen).containsExactlyInAnyOrder(Tactic.values());
        assertThat(at).isEqualTo(Tactic.BALANCED);
    }

    @Test
    @DisplayName("typed in any case, with or without dashes; nonsense is nothing")
    void parsing() {
        assertThat(Tactic.parse("hit-and-run")).contains(Tactic.HIT_AND_RUN);
        assertThat(Tactic.parse("Cautious")).contains(Tactic.CAUTIOUS);
        assertThat(Tactic.parse("berserk")).isEmpty();
        assertThat(Tactic.parse(null)).isEmpty();
    }

    @Test
    @DisplayName("each is one of Core's")
    void core() {
        for (Tactic tactic : Tactic.values()) {
            assertThat(tactic.core().name()).isEqualTo(tactic.name());
        }
    }
}
