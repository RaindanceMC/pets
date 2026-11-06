package de.raindancer.modules.pets.screen;

import de.raindancer.core.ui.menu.Menu;
import de.raindancer.core.ui.tree.LearningMenu;
import de.raindancer.core.ui.tree.SkillTree;
import de.raindancer.core.ui.tree.TreeNode;
import de.raindancer.modules.pets.PetServices;
import de.raindancer.modules.pets.model.Pet;
import de.raindancer.modules.pets.model.Role;
import de.raindancer.modules.pets.model.Species;
import de.raindancer.modules.pets.rules.TreeRule;
import de.raindancer.modules.pets.service.CareService;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** One pet's skill tree: Core's {@link LearningMenu}, answered by this pet. */
public final class PetTreeMenu extends LearningMenu implements IPetScreen {

    public PetTreeMenu(PetServices services, Player viewer, Menu parent, UUID pet) {
        super(viewer, services.brand(), parent, new Answers(services, viewer, pet));
    }

    /** What the tree screen asks, answered for one pet. */
    static final class Answers implements LearningMenu.Learner {

        private final PetServices services;
        private final Player viewer;
        private final UUID id;

        Answers(PetServices services, Player viewer, UUID id) {
            this.services = services;
            this.viewer = viewer;
            this.id = id;
        }

        private Optional<Pet> pet() {
            return services.collection().pet(viewer.getUniqueId(), id);
        }

        private Optional<Species> species() {
            return pet().flatMap(services.collection()::speciesOf);
        }

        private TreeRule rule() {
            return services.summoning().tree();
        }

        @Override
        public SkillTree tree() {
            return rule().tree();
        }

        @Override
        public String title() {
            return pet().map(Pet::name).orElse("Your pet") + "'s skills";
        }

        @Override
        public String blurb() {
            return species().map(species -> {
                Role role = rule().role(species, ranks());
                return role.title() + (role == species.role() ? " by nature" : " by calling") + " — " + role.blurb();
            }).orElse("");
        }

        @Override
        public Material icon() {
            return species().map(Species::icon).orElse(Material.BONE);
        }

        @Override
        public Map<String, Integer> ranks() {
            return pet().map(Pet::skills).orElse(Map.of());
        }

        @Override
        public int level() {
            return pet().map(Pet::level).orElse(1);
        }

        @Override
        public String levelWord() {
            return "pet level";
        }

        @Override
        public int points() {
            return rule().points(level());
        }

        @Override
        public int pointsLeft() {
            return species().map(species -> rule().left(level(), ranks(), species)).orElse(0);
        }

        @Override
        public String describe(String effect, double value) {
            return PetSkills.describe(effect, value);
        }

        @Override
        public Optional<SkillTree.Refusal> refusal(String node) {
            return species().flatMap(species -> rule().refusal(ranks(), node, level(), species));
        }

        @Override
        public Optional<SkillTree.Refusal> learn(String node) {
            return pet().map(pet -> services.care().learn(viewer, pet, node))
                    .orElse(Optional.of(SkillTree.Refusal.NEEDS));
        }

        @Override
        public long resetAgainAt() {
            return pet().map(pet -> TreeRule.resetAgainAt(pet.skillsReset(), CareService.SKILL_RESET_HOURS))
                    .orElse(0L);
        }

        @Override
        public boolean reset() {
            return pet().map(pet -> services.care().resetSkills(viewer, pet, System.currentTimeMillis()))
                    .orElse(false);
        }

        @Override
        public int cost(TreeNode node) {
            return species().map(species -> rule().cost(node, species)).orElse(node.cost());
        }

        @Override
        public List<String> extra(TreeNode node) {
            Optional<Species> species = species();
            if (species.isPresent() && node.id().equals(species.get().role().calling())) {
                return List.of("<green>Its own nature: free.");
            }
            if (node.id().startsWith("calling_")) {
                return List.of("<dark_gray>One calling at a time: a reset frees it again.");
            }
            return List.of();
        }
    }
}
