package de.raindancer.modules.pets.rules;

import de.raindancer.core.ui.tree.SkillTree;
import de.raindancer.core.ui.tree.TreeNode;
import de.raindancer.modules.pets.model.Role;
import de.raindancer.modules.pets.model.Species;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A pet's skill tree: a point every level, spent on its calling — its own species' role for nothing, another for
 * points — and on that role's branch, Bond and Steed.
 */
public final class TreeRule implements IPetRule {

    /** Every effect a node may carry; anything else in the tree is a mistake. */
    public static final Set<String> EFFECTS = Set.of("health", "damage", "speed", "attack_speed", "regen", "heal",
            "xp", "ability_power", "ability_haste", "lifesteal", "toughness", "thorns", "collect", "spot",
            "aura_speed", "aura_haste", "aura_strength", "aura_resistance", "aura_regeneration");

    private final SkillTree tree;

    public TreeRule(SkillTree tree) {
        this.tree = tree;
    }

    public static SkillTree shipped() {
        return SkillTree.resource(TreeRule.class, "/de/raindancer/modules/pets/tree.yml",
                effect -> EFFECTS.contains(effect) || effect.startsWith("calling_"));
    }

    public SkillTree tree() {
        return tree;
    }

    public int points(int level) {
        return Math.max(0, level);
    }

    /** What a node costs this pet: its own species' calling nothing. */
    public int cost(TreeNode node, Species species) {
        return node.id().equals(species.role().calling()) ? 0 : node.cost();
    }

    public int spent(Map<String, Integer> ranks, Species species) {
        int spent = 0;
        for (var entry : ranks.entrySet()) {
            Optional<TreeNode> node = tree.node(entry.getKey());
            if (node.isPresent() && entry.getValue() != null && entry.getValue() > 0) {
                spent += cost(node.get(), species) * Math.min(entry.getValue(), node.get().ranks());
            }
        }
        return spent;
    }

    public int left(int level, Map<String, Integer> ranks, Species species) {
        return points(level) - spent(ranks, species);
    }

    /** The calling it took, or its species' own. */
    public Role role(Species species, Map<String, Integer> ranks) {
        for (Role role : Role.values()) {
            if (ranks.getOrDefault(role.calling(), 0) > 0) {
                return role;
            }
        }
        return species.role();
    }

    public Optional<SkillTree.Refusal> refusal(Map<String, Integer> ranks, String id, int level, Species species) {
        Optional<TreeNode> node = tree.node(id);
        if (node.isEmpty()) {
            return Optional.of(SkillTree.Refusal.NEEDS);
        }
        // the tree measures a node at its own cost; a free calling is measured with that cost handed back
        int left = left(level, ranks, species) + node.get().cost() - cost(node.get(), species);
        return tree.refusal(ranks, node.get(), level, left);
    }

    /** The ranks after taking the next rank of a node, or why not. */
    public record Learnt(Optional<SkillTree.Refusal> refusal, Map<String, Integer> ranks) {
    }

    public Learnt learn(Map<String, Integer> ranks, String id, int level, Species species) {
        Optional<SkillTree.Refusal> refused = refusal(ranks, id, level, species);
        if (refused.isPresent()) {
            return new Learnt(refused, Map.copyOf(ranks));
        }
        Map<String, Integer> next = new HashMap<>(ranks);
        next.merge(id, 1, Integer::sum);
        return new Learnt(Optional.empty(), Map.copyOf(next));
    }

    /** What a pet's ranks add up to. */
    public record Boosts(Map<String, Double> sums) {

        /** A percentage effect as a factor: 18 → 1.18. */
        public double factor(String effect) {
            return Math.round((1 + sums.getOrDefault(effect, 0.0) / 100) * 1e9) / 1e9;
        }

        /** An effect as it is written: blocks, levels. */
        public double plain(String effect) {
            return sums.getOrDefault(effect, 0.0);
        }

        /** A percentage effect as a share: 12 → 0.12. */
        public double share(String effect) {
            return sums.getOrDefault(effect, 0.0) / 100;
        }
    }

    public Boosts boosts(Map<String, Integer> ranks) {
        return new Boosts(tree.effects(ranks));
    }

    public static boolean mayReset(long lastReset, long now, int hours) {
        return now >= resetAgainAt(lastReset, hours);
    }

    public static long resetAgainAt(long lastReset, int hours) {
        return lastReset <= 0 ? 0 : lastReset + hours * 3_600_000L;
    }

    @Override
    public String describe() {
        return "what a pet's skill points buy";
    }
}
