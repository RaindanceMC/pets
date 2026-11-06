package de.raindancer.modules.pets.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("pet levels")
class LevelsTest {

    private final Levels levels = new Levels(30);

    @Test
    @DisplayName("each level needs more experience than the one before")
    void growing() {
        for (int level = 1; level < 30; level++) {
            assertThat(levels.toNext(level + 1)).isGreaterThan(levels.toNext(level));
        }
        assertThat(levels.toNext(1)).isEqualTo(100);
    }

    @Test
    @DisplayName("experience carries over into the next levels")
    void carriesOver() {
        Levels.Progress progress = levels.gain(1, 0, levels.toNext(1) + levels.toNext(2) + 5);
        assertThat(progress.level()).isEqualTo(3);
        assertThat(progress.xp()).isEqualTo(5);
        assertThat(progress.levelsGained()).isEqualTo(2);
    }

    @Test
    @DisplayName("the top level keeps no experience and gains no more")
    void capped() {
        Levels.Progress progress = levels.gain(29, 0, 1_000_000);
        assertThat(progress.level()).isEqualTo(30);
        assertThat(progress.xp()).isZero();
        assertThat(levels.gain(30, 0, 50).levelsGained()).isZero();
    }

    @Test
    @DisplayName("nothing or less is nothing")
    void nothing() {
        assertThat(levels.gain(4, 20, 0)).isEqualTo(new Levels.Progress(4, 20, 0));
        assertThat(levels.gain(4, 20, -7)).isEqualTo(new Levels.Progress(4, 20, 0));
    }

    @Test
    @DisplayName("health and damage grow with the level")
    void stats() {
        assertThat(levels.health(20, 1)).isEqualTo(20, within(1e-9));
        assertThat(levels.health(20, 11)).isEqualTo(36, within(1e-9));
        assertThat(levels.damage(4, 1)).isEqualTo(4, within(1e-9));
        assertThat(levels.damage(4, 11)).isEqualTo(6.4, within(1e-9));
        assertThat(levels.health(20, 99)).isEqualTo(levels.health(20, 30));
    }
}
