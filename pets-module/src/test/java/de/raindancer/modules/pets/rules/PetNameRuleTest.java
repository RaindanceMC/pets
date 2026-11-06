package de.raindancer.modules.pets.rules;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("what a pet may be called")
class PetNameRuleTest {

    private final PetNameRule rule = new PetNameRule();

    @Test
    @DisplayName("ordinary names are fine, trimmed")
    void fine() {
        assertThat(rule.clean("  Sir Wuffles ")).contains("Sir Wuffles");
        assertThat(rule.clean("Mr. O'Hare-2")).contains("Mr. O'Hare-2");
        assertThat(rule.clean("Ümit")).contains("Ümit");
    }

    @Test
    @DisplayName("empty, too long or markup is refused")
    void refused() {
        assertThat(rule.clean("")).isEmpty();
        assertThat(rule.clean("   ")).isEmpty();
        assertThat(rule.clean(null)).isEmpty();
        assertThat(rule.clean("a".repeat(21))).isEmpty();
        assertThat(rule.clean("<red>Evil")).isEmpty();
        assertThat(rule.clean("§cEvil")).isEmpty();
        assertThat(rule.clean("Tab\tName")).isEmpty();
    }

    @Test
    @DisplayName("inner runs of spaces become one")
    void spaces() {
        assertThat(rule.clean("Big    Boy")).contains("Big Boy");
    }
}
