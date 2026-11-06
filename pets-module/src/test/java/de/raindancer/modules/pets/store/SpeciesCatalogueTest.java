package de.raindancer.modules.pets.store;

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
        Species mythicWither = all.stream().filter(s -> s.id().equals("wither")).findFirst().orElseThrow();
        assertThat(mythicWither.unlock().needs()).extracting(de.raindancer.modules.pets.model.Requirement::what)
                .contains("ENDER_DRAGON", "WITHER", "WARDEN", "ELDER_GUARDIAN");
        for (Species mythic : all.stream().filter(s -> s.rarity() == de.raindancer.modules.pets.model.Rarity.MYTHIC).toList()) {
            assertThat(mythic.unlock().needs()).as(mythic.id()).hasSizeGreaterThanOrEqualTo(7);
        }
        assertThat(all).filteredOn(Species::flies).hasSizeGreaterThanOrEqualTo(5);
        assertThat(all).filteredOn(species -> species.rarity() == de.raindancer.modules.pets.model.Rarity.MYTHIC)
                .extracting(Species::id).contains("wither", "warden");
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
