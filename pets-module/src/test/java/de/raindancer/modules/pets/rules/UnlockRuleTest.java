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
}
