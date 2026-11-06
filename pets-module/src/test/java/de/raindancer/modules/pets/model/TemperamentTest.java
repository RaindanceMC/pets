package de.raindancer.modules.pets.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("a pet's personality")
class TemperamentTest {

    @Test
    @DisplayName("every personality is what its own traits read as")
    void archetypes() {
        for (Personality personality : Personality.values()) {
            assertThat(Temperament.of(personality).personality()).as(personality.name()).isEqualTo(personality);
        }
    }

    @Test
    @DisplayName("born at random, every personality turns up")
    void random() {
        Random random = new Random(7);
        EnumSet<Personality> seen = EnumSet.noneOf(Personality.class);
        for (int i = 0; i < 5000; i++) {
            Temperament born = Temperament.born(random);
            assertThat(born.nudgeCourage()).isZero();
            seen.add(born.personality());
        }
        assertThat(seen).containsExactlyInAnyOrder(Personality.values());
    }

    @Test
    @DisplayName("an owner can bend a pet's nature, but only so far")
    void bounded() {
        Temperament timid = Temperament.of(Personality.TIMID);
        Temperament trained = timid;
        for (int i = 0; i < 500; i++) {
            trained = trained.nudge(Temperament.Trait.COURAGE, 1);
        }
        assertThat(trained.nudgeCourage()).isEqualTo(Temperament.MOST_NUDGE);
        assertThat(trained.courage()).isEqualTo(timid.innateCourage() + Temperament.MOST_NUDGE);
        assertThat(trained.innateCourage()).isEqualTo(timid.innateCourage());
        assertThat(trained.nudge(Temperament.Trait.COURAGE, -500).nudgeCourage()).isEqualTo(-Temperament.MOST_NUDGE);
    }

    @Test
    @DisplayName("traits never leave −100…100, however born and however nudged")
    void clamped() {
        Temperament wild = new Temperament(100, 100, 100, 0, 0, 0).nudge(Temperament.Trait.TEMPER, 30);
        assertThat(wild.temper()).isEqualTo(100);
        assertThat(new Temperament(500, -500, 0, 0, 0, 0).innateCourage()).isEqualTo(100);
    }

    @Test
    @DisplayName("an admin's choice replaces the nature and forgets the training")
    void setByAdmin() {
        Temperament trained = Temperament.of(Personality.LAZY).nudge(Temperament.Trait.ENERGY, 20);
        Temperament fierce = trained.becoming(Personality.FIERCE);
        assertThat(fierce.personality()).isEqualTo(Personality.FIERCE);
        assertThat(fierce.nudgeEnergy()).isZero();
    }

    @Test
    @DisplayName("fierce hits harder, lively runs faster, brave lasts longer, gentle heals more — within a sixth")
    void effects() {
        Temperament fierce = Temperament.of(Personality.FIERCE);
        Temperament gentle = Temperament.of(Personality.GENTLE);
        assertThat(fierce.damageFactor()).isGreaterThan(1).isLessThanOrEqualTo(1.15 + 1e-9);
        assertThat(gentle.damageFactor()).isLessThan(1);
        assertThat(gentle.healFactor()).isGreaterThan(1);
        assertThat(Temperament.of(Personality.PLAYFUL).speedFactor()).isGreaterThan(1);
        assertThat(Temperament.of(Personality.LAZY).speedFactor()).isLessThan(1);
        assertThat(Temperament.of(Personality.BRAVE).healthFactor()).isGreaterThan(1);
        assertThat(Temperament.of(Personality.BALANCED).damageFactor()).isEqualTo(1, within(1e-9));
    }

    @Test
    @DisplayName("a timid pet backs off when hurt; a brave one does not")
    void fleeing() {
        assertThat(Temperament.of(Personality.TIMID).fleesBelow()).isGreaterThan(0.2);
        assertThat(Temperament.of(Personality.BRAVE).fleesBelow()).isZero();
    }
}
