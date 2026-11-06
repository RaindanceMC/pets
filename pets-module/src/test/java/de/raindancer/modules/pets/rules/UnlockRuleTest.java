package de.raindancer.modules.pets.rules;

import de.raindancer.core.social.economy.Money;
import de.raindancer.core.world.activity.ActivityKind;
import de.raindancer.modules.pets.model.Role;
import de.raindancer.modules.pets.model.Species;
import de.raindancer.modules.pets.model.Unlock;
import org.bukkit.Material;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("who may have which pet")
class UnlockRuleTest {

    private final UnlockRule rule = new UnlockRule();

    private static Species species(Unlock unlock) {
        return new Species("fox", "Fox", "FOX", Role.FIGHTER, de.raindancer.modules.pets.model.Rarity.COMMON,
                Material.SWEET_BERRIES, 16, 3, 1.2, 0.35, false, true, unlock, "Quick and sly.", "SNOWBALL");
    }

    private static UnlockRule.Facts facts(boolean owned, boolean supporter, boolean granted, long progress) {
        return new UnlockRule.Facts(owned, supporter, granted, progress);
    }

    @Test
    @DisplayName("one already had is owned, whatever the way")
    void owned() {
        assertThat(rule.judge(species(Unlock.supporter()), facts(true, false, false, 0)).state())
                .isEqualTo(UnlockRule.State.OWNED);
    }

    @Test
    @DisplayName("a free one is there to take")
    void free() {
        assertThat(rule.judge(species(Unlock.free()), facts(false, false, false, 0)).state())
                .isEqualTo(UnlockRule.State.CLAIMABLE);
    }

    @Test
    @DisplayName("an earned one is locked until enough was done, then there to take")
    void earned() {
        Species fox = species(Unlock.earn(ActivityKind.FORAGE, 200));
        UnlockRule.Verdict halfway = rule.judge(fox, facts(false, false, false, 120));
        assertThat(halfway.state()).isEqualTo(UnlockRule.State.LOCKED);
        assertThat(halfway.progress()).isEqualTo(120);
        assertThat(halfway.needed()).isEqualTo(200);
        assertThat(rule.judge(fox, facts(false, false, false, 200)).state()).isEqualTo(UnlockRule.State.CLAIMABLE);
    }

    @Test
    @DisplayName("earning can be one particular thing — a dragon for slaying the dragon")
    void earnedWhat() {
        Unlock dragon = Unlock.earn(ActivityKind.BOSS, "ENDER_DRAGON", 1);
        assertThat(dragon.counts(ActivityKind.BOSS, "ENDER_DRAGON")).isTrue();
        assertThat(dragon.counts(ActivityKind.BOSS, "ender_dragon")).isTrue();
        assertThat(dragon.counts(ActivityKind.BOSS, "WITHER")).isFalse();
        assertThat(dragon.counts(ActivityKind.MONSTER, "ENDER_DRAGON")).isFalse();
        assertThat(Unlock.earn(ActivityKind.FORAGE, 10).counts(ActivityKind.FORAGE, "SWEET_BERRIES")).isTrue();
        assertThat(Unlock.free().counts(ActivityKind.FORAGE, "x")).isFalse();
    }

    @Test
    @DisplayName("a bought one has a price")
    void bought() {
        UnlockRule.Verdict verdict = rule.judge(species(Unlock.buy("2.5k")), facts(false, false, false, 0));
        assertThat(verdict.state()).isEqualTo(UnlockRule.State.BUYABLE);
        assertThat(verdict.price()).isEqualTo(Money.of(250_000));
    }

    @Test
    @DisplayName("a supporters' one is only for supporters")
    void supporters() {
        Species allay = species(Unlock.supporter());
        assertThat(rule.judge(allay, facts(false, false, false, 0)).state())
                .isEqualTo(UnlockRule.State.SUPPORTERS_ONLY);
        assertThat(rule.judge(allay, facts(false, true, false, 0)).state())
                .isEqualTo(UnlockRule.State.CLAIMABLE);
    }

    @Test
    @DisplayName("a rank-only one needs its permission")
    void permission() {
        Species panda = species(Unlock.permission());
        assertThat(rule.judge(panda, facts(false, false, false, 0)).state()).isEqualTo(UnlockRule.State.RANK_ONLY);
        assertThat(rule.judge(panda, facts(false, false, true, 0)).state()).isEqualTo(UnlockRule.State.CLAIMABLE);
    }

    @Test
    @DisplayName("being granted a species opens it whatever its way — a rank can hand out any pet")
    void grantedOpensEverything() {
        assertThat(rule.judge(species(Unlock.buy("100")), facts(false, false, true, 0)).state())
                .isEqualTo(UnlockRule.State.CLAIMABLE);
        assertThat(rule.judge(species(Unlock.supporter()), facts(false, false, true, 0)).state())
                .isEqualTo(UnlockRule.State.CLAIMABLE);
    }

    @Test
    @DisplayName("a price of nothing is free")
    void freeOfCharge() {
        assertThat(rule.judge(species(Unlock.buy("0")), facts(false, false, false, 0)).state())
                .isEqualTo(UnlockRule.State.CLAIMABLE);
    }

    @Test
    @DisplayName("an unlock with several parts opens only once every part is done")
    void everyPart() {
        Unlock everyBoss = Unlock.earnAll(java.util.List.of(
                new de.raindancer.modules.pets.model.Requirement(ActivityKind.BOSS, "ENDER_DRAGON", 1),
                new de.raindancer.modules.pets.model.Requirement(ActivityKind.BOSS, "WITHER", 1),
                new de.raindancer.modules.pets.model.Requirement(ActivityKind.RAID, "", 1)));
        Species dragon = species(everyBoss);
        UnlockRule.Verdict halfway = rule.judge(dragon,
                new UnlockRule.Facts(false, false, false, java.util.List.of(1L, 0L, 1L)));
        assertThat(halfway.state()).isEqualTo(UnlockRule.State.LOCKED);
        assertThat(halfway.parts()).hasSize(3);
        assertThat(halfway.parts().get(1).done()).isZero();
        assertThat(rule.judge(dragon, new UnlockRule.Facts(false, false, false, java.util.List.of(1L, 2L, 1L))).state())
                .isEqualTo(UnlockRule.State.CLAIMABLE);
        assertThat(everyBoss.matching(ActivityKind.BOSS, "wither")).containsExactly(1);
        assertThat(everyBoss.matching(ActivityKind.RAID, "anything")).containsExactly(2);
        assertThat(Unlock.progressKey("ender_dragon", 0)).isEqualTo("ender_dragon");
        assertThat(Unlock.progressKey("ender_dragon", 2)).isEqualTo("ender_dragon#2");
    }

    @Test
    @DisplayName("a dropped pet: locked until its mob let it fall, then there to take")
    void dropped() {
        Unlock blaze = Unlock.drop(ActivityKind.MONSTER, "BLAZE", 1.0 / 800);
        assertThat(blaze.way()).isEqualTo(Unlock.Way.DROP);
        assertThat(blaze.chance()).isEqualTo(1.0 / 800);
        assertThat(blaze.counts(ActivityKind.MONSTER, "blaze")).isTrue();
        assertThat(blaze.counts(ActivityKind.MONSTER, "zombie")).isFalse();
        assertThat(blaze.counts(ActivityKind.BOSS, "blaze")).isFalse();

        UnlockRule.Verdict notYet = rule.judge(species(blaze), facts(false, false, false, 0));
        assertThat(notYet.state()).isEqualTo(UnlockRule.State.LOCKED);
        assertThat(rule.judge(species(blaze), facts(false, false, false, 1)).state())
                .isEqualTo(UnlockRule.State.CLAIMABLE);
    }

    @Test
    @DisplayName("a roll below the chance drops it, anything else does not")
    void roll() {
        Unlock wither = Unlock.drop(ActivityKind.BOSS, "WITHER", 0.01);
        assertThat(wither.drops(0.0)).isTrue();
        assertThat(wither.drops(0.0099)).isTrue();
        assertThat(wither.drops(0.01)).isFalse();
        assertThat(wither.drops(0.5)).isFalse();
        assertThat(Unlock.earn(ActivityKind.BOSS, "WITHER", 1).drops(0.0)).as("only a drop drops").isFalse();
    }

    @Test
    @DisplayName("a drop is kept apart from kills counted when the species was still earned, so old counts open nothing")
    void dropKeptApart() {
        assertThat(Unlock.drop(ActivityKind.MONSTER, "BLAZE", 0.001).keyFor("blaze", 0)).isEqualTo("blaze#dropped");
        assertThat(Unlock.earn(ActivityKind.MONSTER, "BLAZE", 10).keyFor("blaze", 0)).isEqualTo("blaze");
        assertThat(Unlock.earn(ActivityKind.MONSTER, "BLAZE", 10).keyFor("blaze", 2)).isEqualTo("blaze#2");
    }
}
