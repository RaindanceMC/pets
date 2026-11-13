package de.raindancer.modules.pets.service;

import de.raindancer.core.social.support.Supporters;
import de.raindancer.core.testkit.TestDatabases;
import de.raindancer.modules.pets.PetModule;
import de.raindancer.modules.pets.PetSettings;
import de.raindancer.modules.pets.model.Species;
import de.raindancer.modules.pets.rules.DiscoveryRule;
import de.raindancer.modules.pets.rules.UnlockRule;
import de.raindancer.modules.pets.store.PetDatabase;
import de.raindancer.modules.pets.store.PetStore;
import de.raindancer.modules.pets.store.SpeciesCatalogue;
import de.raindancer.modules.pets.util.PermissionNodes;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("creators and the supporters' species")
class CreatorSpeciesTest {

    private static final UUID ALICE = UUID.nameUUIDFromBytes("alice".getBytes());

    private CollectionService collection;
    private Supporters supporters;
    private Species thankYou;

    @BeforeEach
    void setUp(@TempDir Path folder) {
        List<Species> shipped = SpeciesCatalogue.read(PetModule.class.getResourceAsStream("pets.yml"), problem -> { });
        thankYou = shipped.stream().filter(Species::supportersOnly).filter(each -> each.rarity() != de.raindancer.modules.pets.model.Rarity.COMMON)
                .findFirst().orElseThrow();
        supporters = mock(Supporters.class);
        collection = new CollectionService(new PetStore(TestDatabases.open(folder.resolve("pets.db"), PetDatabase.SCHEMA)),
                new SpeciesCatalogue(folder.resolve("pets.yml"), () -> PetModule.class.getResourceAsStream("pets.yml")),
                new UnlockRule(), new DiscoveryRule(), supporters, Runnable::run, Clock.systemUTC(), PetSettings.DEFAULTS);
        collection.load(ALICE);
    }

    private Player alice(String... nodes) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(ALICE);
        for (String node : nodes) {
            when(player.hasPermission(node)).thenReturn(true);
        }
        return player;
    }

    @Test
    @DisplayName("the node is rainspets.creator")
    void node() {
        assertThat(PermissionNodes.CREATOR).isEqualTo("rainspets.creator");
    }

    @Test
    @DisplayName("somebody who is neither supporter nor creator is told the species is for supporters")
    void stranger() {
        Player player = alice();
        assertThat(collection.verdict(player, thankYou).state()).isEqualTo(UnlockRule.State.SUPPORTERS_ONLY);
        assertThat(collection.claim(player, thankYou).outcome()).isEqualTo(CollectionService.Outcome.SUPPORTERS_ONLY);
        assertThat(collection.knows(player, thankYou)).isFalse();
    }

    @Test
    @DisplayName("a creator may take them, knows them and has them listed as open")
    void creatorTakes() {
        Player player = alice(PermissionNodes.CREATOR);
        assertThat(collection.verdict(player, thankYou).state()).isEqualTo(UnlockRule.State.CLAIMABLE);
        assertThat(collection.knows(player, thankYou)).isTrue();
        CollectionService.Adoption adoption = collection.claim(player, thankYou);
        assertThat(adoption.outcome()).isEqualTo(CollectionService.Outcome.ADOPTED);
        assertThat(collection.owns(ALICE, thankYou.id())).isTrue();
        assertThat(collection.verdict(player, thankYou).state()).isEqualTo(UnlockRule.State.OWNED);
    }

    @Test
    @DisplayName("a creator does not get the supporters' extra pets out at once")
    void notTheCount() {
        Player player = alice(PermissionNodes.CREATOR);
        assertThat(collection.isSupporter(player)).isFalse();
    }

    @Test
    @DisplayName("a coffee supporter still takes them, and counts as a supporter")
    void supporterStillWorks() {
        when(supporters.isSupporter(ALICE)).thenReturn(true);
        Player player = alice();
        assertThat(collection.claim(player, thankYou).outcome()).isEqualTo(CollectionService.Outcome.ADOPTED);
        assertThat(collection.isSupporter(player)).isTrue();
    }

    @Test
    @DisplayName("Prime members take them too, and do not get the extra pets out at once for it")
    void primeTakes() {
        Player player = alice("rains.prime");
        assertThat(collection.verdict(player, thankYou).state()).isEqualTo(UnlockRule.State.CLAIMABLE);
        assertThat(collection.knows(player, thankYou)).isTrue();
        assertThat(collection.claim(player, thankYou).outcome()).isEqualTo(CollectionService.Outcome.ADOPTED);
        assertThat(collection.isSupporter(player)).isFalse();
    }
}
