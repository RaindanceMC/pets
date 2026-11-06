package de.raindancer.modules.pets.store;

import de.raindancer.core.world.activity.ActivityKind;
import de.raindancer.modules.pets.model.Requirement;
import de.raindancer.modules.pets.model.Role;
import de.raindancer.modules.pets.model.Species;
import de.raindancer.modules.pets.model.Unlock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("the species a server offers")
class SpeciesCatalogueTest {

    private static InputStream shipped() {
        return SpeciesCatalogue.class.getResourceAsStream("/de/raindancer/modules/pets/pets.yml");
    }

    @Test
    @DisplayName("the shipped catalogue reads whole: every role, a free starter, and pets kept for supporters")
    void shippedCatalogue() {
        List<String> problems = new ArrayList<>();
        List<Species> all = SpeciesCatalogue.read(shipped(), problems::add);

        assertThat(problems).isEmpty();
        assertThat(all).hasSizeGreaterThanOrEqualTo(12);
        EnumSet<Role> roles = EnumSet.noneOf(Role.class);
        all.forEach(species -> roles.add(species.role()));
        assertThat(roles).containsExactlyInAnyOrder(Role.values());
        assertThat(all).anyMatch(species -> species.unlock().way() == Unlock.Way.FREE);
        assertThat(all).filteredOn(species -> species.unlock().way() == Unlock.Way.SUPPORTER).hasSizeGreaterThanOrEqualTo(4);
        assertThat(all).anyMatch(species -> species.unlock().way() == Unlock.Way.EARN);
        assertThat(all).anyMatch(species -> species.unlock().way() == Unlock.Way.BUY);
        assertThat(all).filteredOn(species -> species.unlock().way() == Unlock.Way.BUY)
                .allMatch(species -> species.rarity() == de.raindancer.modules.pets.model.Rarity.COMMON);
        assertThat(all).extracting(Species::id).doesNotHaveDuplicates();
        assertThat(all).allMatch(species -> species.size() <= Species.LARGEST && species.size() >= Species.SMALLEST);
        double parrot = all.stream().filter(s -> s.id().equals("parrot")).findFirst().orElseThrow().size();
        double wither = all.stream().filter(s -> s.id().equals("wither")).findFirst().orElseThrow().size();
        assertThat(parrot).isLessThan(wither);
        EnumSet<de.raindancer.modules.pets.model.Rarity> rarities = EnumSet.noneOf(de.raindancer.modules.pets.model.Rarity.class);
        all.forEach(species -> rarities.add(species.rarity()));
        assertThat(rarities).containsExactlyInAnyOrder(de.raindancer.modules.pets.model.Rarity.values());
        // the client will not shrink an ender dragon, so there is no dragon pet
        assertThat(all).extracting(Species::id).contains("wither", "warden").doesNotContain("ender_dragon");
        assertThat(all).filteredOn(Species::flies).hasSizeGreaterThanOrEqualTo(5);
        assertThat(all).filteredOn(species -> species.rarity() == de.raindancer.modules.pets.model.Rarity.MYTHIC)
                .extracting(Species::id).contains("wither", "warden");
    }

    @Test
    @DisplayName("the rarer a pet, the more it takes: several things at once, and big numbers")
    void rarerIsHarder() {
        List<Species> all = SpeciesCatalogue.read(shipped(), problem -> { });
        for (Species species : all) {
            if (species.unlock().way() != Unlock.Way.EARN) {
                continue;
            }
            List<Requirement> needs = species.unlock().needs();
            long monsters = needs.stream().filter(need -> need.activity() == ActivityKind.MONSTER && need.what().isEmpty())
                    .mapToLong(Requirement::count).sum();
            switch (species.rarity()) {
                case RARE -> assertThat(needs).as(species.id()).hasSizeGreaterThanOrEqualTo(2);
                case EPIC -> assertThat(needs).as(species.id()).hasSizeGreaterThanOrEqualTo(3);
                case LEGENDARY -> {
                    assertThat(needs).as(species.id()).hasSizeGreaterThanOrEqualTo(5);
                    assertThat(needs).as(species.id() + " asks for a boss")
                            .anyMatch(need -> need.activity() == ActivityKind.BOSS);
                    assertThat(monsters).as(species.id() + " monsters of any kind").isGreaterThanOrEqualTo(4000);
                }
                case MYTHIC -> {
                    assertThat(needs).as(species.id()).hasSizeGreaterThanOrEqualTo(8);
                    for (String boss : List.of("WITHER", "ENDER_DRAGON", "WARDEN", "ELDER_GUARDIAN")) {
                        assertThat(needs).as(species.id() + " needs " + boss + " three times")
                                .anyMatch(need -> need.activity() == ActivityKind.BOSS && need.what().equals(boss)
                                        && need.count() >= 3);
                    }
                    assertThat(monsters).as(species.id() + " monsters of any kind").isGreaterThanOrEqualTo(15000);
                }
                default -> { }
            }
        }
    }

    @Test
    @DisplayName("drop: a mob that lets it fall, and how rarely — 1/800 or 0.125%")
    void dropWritten() {
        Unlock blaze = SpeciesCatalogue.unlock("drop monster blaze 1/800");
        assertThat(blaze.way()).isEqualTo(Unlock.Way.DROP);
        assertThat(blaze.what()).isEqualTo("BLAZE");
        assertThat(blaze.activity()).isEqualTo(ActivityKind.MONSTER);
        assertThat(blaze.chance()).isEqualTo(1.0 / 800);
        assertThat(SpeciesCatalogue.unlock("drop boss wither 0.5%").chance()).isEqualTo(0.005);
        for (String broken : List.of("drop", "drop monster blaze", "drop monster blaze 0", "drop monster blaze 2",
                "drop monster blaze 1/0", "drop fish salmon 1/10", "drop monster blaze lots")) {
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> SpeciesCatalogue.unlock(broken)).as(broken)
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("legendary and mythic pets are only ever dropped (or given): a mythic from a boss, once in twenty kills or rarer")
    void rarestDrop() {
        List<Species> all = SpeciesCatalogue.read(shipped(), problem -> { });
        for (Species species : all) {
            Unlock.Way way = species.unlock().way();
            if (species.rarity() == de.raindancer.modules.pets.model.Rarity.LEGENDARY
                    || species.rarity() == de.raindancer.modules.pets.model.Rarity.MYTHIC) {
                assertThat(way).as(species.id()).isIn(Unlock.Way.DROP, Unlock.Way.SUPPORTER, Unlock.Way.PERMISSION);
            }
            if (species.rarity() == de.raindancer.modules.pets.model.Rarity.MYTHIC && way == Unlock.Way.DROP) {
                assertThat(species.unlock().activity()).as(species.id()).isEqualTo(ActivityKind.BOSS);
                assertThat(species.unlock().chance()).as(species.id()).isLessThanOrEqualTo(0.05);
            }
            if (way == Unlock.Way.DROP) {
                assertThat(species.unlock().what()).as(species.id() + " drops from its own kind")
                        .isEqualTo(species.entityType());
                assertThat(species.unlock().chance()).as(species.id()).isLessThanOrEqualTo(0.05);
            }
        }
    }

    @Test
    @DisplayName("every shipped species is a mob the server can make")
    void mobs() {
        for (Species species : SpeciesCatalogue.read(shipped(), problem -> { })) {
            org.bukkit.entity.EntityType type = org.bukkit.entity.EntityType.valueOf(species.entityType());
            assertThat(type.getEntityClass()).as(species.id()).isNotNull();
            assertThat(org.bukkit.entity.Mob.class.isAssignableFrom(type.getEntityClass())).as(species.id()).isTrue();
        }
    }

    @Test
    @DisplayName("a broken entry is named and skipped, and the rest still read")
    void broken(@TempDir Path folder) throws IOException {
        Path file = folder.resolve("pets.yml");
        Files.writeString(file, """
                species:
                  wolf:
                    title: Wolf
                    mob: WOLF
                    role: fighter
                    icon: BONE
                    health: 20
                    damage: 4
                    unlock: free
                  dragon:
                    title: Dragon
                    mob: NOT_A_MOB
                    role: fighter
                    unlock: free
                  cat:
                    title: Cat
                    mob: CAT
                    role: nonsense
                    unlock: free
                  fox:
                    title: Fox
                    mob: FOX
                    role: fighter
                    rarity: rare
                    flies: true
                    size: 0.5
                    unlock: earn boss ender_dragon 150
                  bee:
                    title: Bee
                    mob: BEE
                    role: fighter
                    unlock: buy 1.5k
                  wither:
                    title: Wither
                    mob: WITHER
                    role: ranged
                    rarity: mythic
                    unlock: earn boss wither 3 + raid 1 + boss elder_guardian 1
                  panda:
                    title: Panda
                    mob: PANDA
                    role: guard
                    rarity: epic
                    unlock: buy 9k
                """);
        List<String> problems = new ArrayList<>();
        List<Species> read;
        try (InputStream in = Files.newInputStream(file)) {
            read = SpeciesCatalogue.read(in, problems::add);
        }

        assertThat(read).extracting(Species::id).containsExactly("wolf", "fox", "bee", "wither");
        assertThat(read.get(3).unlock().needs()).hasSize(3);
        assertThat(read.get(3).unlock().needs().get(1).activity()).isEqualTo(de.raindancer.core.world.activity.ActivityKind.RAID);
        assertThat(read.get(3).unlock().needs().get(2).what()).isEqualTo("ELDER_GUARDIAN");
        assertThat(problems).hasSize(3);
        assertThat(problems.get(2)).contains("panda").contains("only common pets can be bought");
        assertThat(problems.get(0)).contains("dragon");
        assertThat(problems.get(1)).contains("cat");
        assertThat(read.get(1).unlock().count()).isEqualTo(150);
        assertThat(read.get(1).unlock().what()).isEqualTo("ENDER_DRAGON");
        assertThat(read.get(1).rarity()).isEqualTo(de.raindancer.modules.pets.model.Rarity.RARE);
        assertThat(read.get(1).flies()).isTrue();
        assertThat(read.get(1).size()).isEqualTo(0.5);
        assertThat(read.get(0).rarity()).isEqualTo(de.raindancer.modules.pets.model.Rarity.COMMON);
        assertThat(read.get(2).unlock().price()).isEqualTo("1.5k");
    }

    @Test
    @DisplayName("the owner's own copy is written once and then read, not overwritten")
    void ownCopy(@TempDir Path folder) throws IOException {
        SpeciesCatalogue catalogue = new SpeciesCatalogue(folder.resolve("pets.yml"), SpeciesCatalogueTest::shipped);
        catalogue.load(problem -> { });
        assertThat(folder.resolve("pets.yml")).exists();
        int shippedCount = catalogue.all().size();

        Files.writeString(folder.resolve("pets.yml"), """
                species:
                  wolf:
                    title: Good Boy
                    mob: WOLF
                    role: guard
                    unlock: free
                """);
        catalogue.load(problem -> { });
        assertThat(catalogue.all()).hasSize(1);
        assertThat(catalogue.find("wolf")).get().extracting(Species::title).isEqualTo("Good Boy");
        assertThat(catalogue.find("WOLF")).isPresent();
        assertThat(shippedCount).isGreaterThan(1);
    }
}
