package de.raindancer.modules.pets.service;

import de.raindancer.core.testkit.TestDatabases;
import de.raindancer.modules.pets.PetSettings;
import de.raindancer.modules.pets.model.Pet;
import de.raindancer.modules.pets.model.Personality;
import de.raindancer.modules.pets.model.Temperament;
import de.raindancer.modules.pets.rules.DiscoveryRule;
import de.raindancer.modules.pets.rules.UnlockRule;
import de.raindancer.modules.pets.store.PetDatabase;
import de.raindancer.modules.pets.store.PetStore;
import de.raindancer.modules.pets.store.PetStoreTest;
import de.raindancer.modules.pets.store.SpeciesCatalogue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("reading a player's pets when they join")
class CollectionLoadingTest {

    private static final UUID ALICE = UUID.nameUUIDFromBytes("alice".getBytes());

    @Test
    @DisplayName("a database that does not answer is no pets for the session: nothing is held, and the next try reads them")
    void failureIsNotCached(@TempDir Path folder) {
        AtomicBoolean down = new AtomicBoolean();
        var database = PetStoreTest.flaky(TestDatabases.open(folder.resolve("pets.db"), PetDatabase.SCHEMA), down);
        PetStore store = new PetStore(database);
        Pet rex = Pet.adopt(ALICE, "wolf", "Rex", Temperament.of(Personality.CHEEKY), Instant.ofEpochSecond(1_000));
        store.save(rex);
        CollectionService collection = new CollectionService(store,
                new SpeciesCatalogue(folder.resolve("pets.yml"), () -> null), new UnlockRule(), new DiscoveryRule(),
                null, Runnable::run, Clock.systemUTC(), PetSettings.DEFAULTS);

        down.set(true);
        assertThat(collection.load(ALICE)).isCompletedExceptionally();
        assertThat(collection.loaded(ALICE)).as("a failure is not 'loaded'").isFalse();

        down.set(false);
        assertThat(collection.load(ALICE)).isCompleted();
        assertThat(collection.loaded(ALICE)).isTrue();
        assertThat(collection.pets(ALICE)).containsExactly(rex);
    }
}
