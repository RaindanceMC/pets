package de.raindancer.modules.pets.rules;

import de.raindancer.core.ui.tree.SkillTree;
import de.raindancer.modules.pets.model.Rarity;
import de.raindancer.modules.pets.model.Role;
import de.raindancer.modules.pets.model.Species;
import de.raindancer.modules.pets.model.Unlock;
import org.bukkit.Material;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("a pet's skill tree: a point a level, its calling, its role")
class TreeRuleTest {

    private final TreeRule rule = new TreeRule(TreeRule.shipped());

    private static Species wolf() {
        return new Species("wolf", "Wolf", "WOLF", Role.FIGHTER, Rarity.COMMON, Material.BONE, 20, 4, 0.34, 0.32,
                false, true, Unlock.free(), "", "SNOWBALL");
    }

    @Test
    @DisplayName("the shipped tree reads whole: a branch for every role and two for any pet, each with a mastery")
    void shipped() {
        SkillTree tree = TreeRule.shipped();
        for (Role role : Role.values()) {
            assertThat(tree.node(role.calling())).as(role.calling()).isPresent();
            assertThat(tree.masteries(role.name().toLowerCase())).as(role.name()).isNotEmpty();
        }
        assertThat(tree.branch("bond")).isPresent();
        assertThat(tree.branch("steed")).isPresent();
        assertThat(tree.nodes()).hasSizeGreaterThanOrEqualTo(50);
    }

    @Test
    @DisplayName("a point a level")
    void points() {
        assertThat(rule.points(1)).isEqualTo(1);
        assertThat(rule.points(30)).isEqualTo(30);
        assertThat(rule.left(5, Map.of(), wolf())).isEqualTo(5);
    }

    @Test
    @DisplayName("its own calling is free; another costs points and makes it that role")
    void callings() {
        Map<String, Integer> ranks = new HashMap<>();
        assertThat(rule.role(wolf(), ranks)).isEqualTo(Role.FIGHTER);
        TreeRule.Learnt own = rule.learn(ranks, "calling_fighter", 1, wolf());
        assertThat(own.refusal()).isEmpty();
        assertThat(rule.left(1, own.ranks(), wolf())).as("free").isEqualTo(1);

        Map<String, Integer> none = new HashMap<>();
        assertThat(rule.learn(none, "calling_guard", 2, wolf()).refusal()).as("three points, two to hand")
                .contains(SkillTree.Refusal.NO_POINTS);
        TreeRule.Learnt guard = rule.learn(none, "calling_guard", 5, wolf());
        assertThat(guard.refusal()).isEmpty();
        assertThat(rule.role(wolf(), guard.ranks())).isEqualTo(Role.GUARD);
        assertThat(rule.left(5, guard.ranks(), wolf())).isEqualTo(2);
        assertThat(rule.learn(guard.ranks(), "calling_fighter", 30, wolf()).refusal()).as("one calling at a time")
                .contains(SkillTree.Refusal.BARRED);
    }

    @Test
    @DisplayName("a role's nodes need its calling; bond and steed need nothing")
    void needs() {
        assertThat(rule.learn(Map.of(), "thick_hide", 10, wolf()).refusal()).contains(SkillTree.Refusal.NEEDS);
        assertThat(rule.learn(Map.of(), "loyal_heart", 1, wolf()).refusal()).isEmpty();
    }

    @Test
    @DisplayName("what the ranks add up to, for the companion")
    void effects() {
        Map<String, Integer> ranks = Map.of("calling_fighter", 1, "sharp_teeth", 3, "loyal_heart", 2);
        TreeRule.Boosts boosts = rule.boosts(ranks);
        assertThat(boosts.factor("damage")).isEqualTo(1.18);
        assertThat(boosts.factor("health")).isEqualTo(1.10);
        assertThat(boosts.factor("speed")).isEqualTo(1);
        assertThat(boosts.plain("collect")).isZero();
    }

    @Test
    @DisplayName("a reset waits its hours")
    void reset() {
        long hour = 3_600_000L;
        assertThat(TreeRule.mayReset(0, 10 * hour, 24)).isTrue();
        assertThat(TreeRule.mayReset(5 * hour, 10 * hour, 24)).isFalse();
        assertThat(TreeRule.resetAgainAt(5 * hour, 24)).isEqualTo(29 * hour);
    }
}
