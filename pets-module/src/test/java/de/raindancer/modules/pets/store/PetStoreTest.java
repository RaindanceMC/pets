package de.raindancer.modules.pets.store;

import de.raindancer.core.data.sql.Database;
import de.raindancer.core.testkit.TestDatabases;
import de.raindancer.modules.pets.model.Mode;
import de.raindancer.modules.pets.model.Personality;
import de.raindancer.modules.pets.model.Pet;
import de.raindancer.modules.pets.model.Temperament;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("where pets are kept")
class PetStoreTest {

    private static final UUID ALICE = UUID.nameUUIDFromBytes("alice".getBytes());
    private static final UUID BOB = UUID.nameUUIDFromBytes("bob".getBytes());

    @Test
    @DisplayName("a pet comes back as it was saved, and only to its owner")
    void roundTrip(@TempDir Path folder) {
        PetStore store = new PetStore(TestDatabases.open(folder.resolve("pets.db"), PetDatabase.SCHEMA));
        Pet rex = Pet.adopt(ALICE, "wolf", "Rex", Temperament.of(Personality.CHEEKY), Instant.ofEpochSecond(1_000));

        assertThat(store.save(rex)).isTrue();
        assertThat(store.of(BOB)).isEmpty();
        assertThat(store.of(ALICE)).containsExactly(rex);

        Pet grown = rex.withProgress(7, 42).withMode(Mode.AGGRESSIVE).withName("Rex II").withOut(true)
                .withRestingUntil(Instant.ofEpochSecond(2_000))
                .withTemperament(rex.temperament().nudge(Temperament.Trait.ENERGY, 12))
                .withStyle("#ff0000,#00ff00|bold")
                .withParticle("TRIAL_SPAWNER_DETECTION", "HALO");
        assertThat(store.save(grown)).isTrue();
        assertThat(store.of(ALICE)).containsExactly(grown);
    }

    @Test
    @DisplayName("progress towards earning a species adds up per player")
    void progress(@TempDir Path folder) {
        Database database = TestDatabases.open(folder.resolve("pets.db"), PetDatabase.SCHEMA);
        PetStore store = new PetStore(database);

        assertThat(store.progress(ALICE)).isEmpty();
        store.addProgress(ALICE, "fox", 5);
        store.addProgress(ALICE, "fox", 7);
        store.addProgress(ALICE, "cat", 1);
        store.addProgress(BOB, "fox", 100);

        assertThat(store.progress(ALICE)).containsEntry("fox", 12L).containsEntry("cat", 1L).hasSize(2);
    }

    @Test
    @DisplayName("a pet taken away is gone, and only that one")
    void remove(@TempDir Path folder) {
        PetStore store = new PetStore(TestDatabases.open(folder.resolve("pets.db"), PetDatabase.SCHEMA));
        Pet rex = Pet.adopt(ALICE, "wolf", "Rex", Temperament.of(Personality.BRAVE), Instant.ofEpochSecond(1_000));
        Pet tom = Pet.adopt(ALICE, "cat", "Tom", Temperament.of(Personality.LAZY), Instant.ofEpochSecond(1_001));
        store.save(rex);
        store.save(tom);

        assertThat(store.remove(rex.id())).isTrue();
        assertThat(store.of(ALICE)).containsExactly(tom);
    }

    @Test
    @DisplayName("a species once seen stays seen, per player")
    void seen(@TempDir Path folder) {
        PetStore store = new PetStore(TestDatabases.open(folder.resolve("pets.db"), PetDatabase.SCHEMA));
        assertThat(store.seen(ALICE)).isEmpty();
        store.markSeen(ALICE, "warden");
        store.markSeen(ALICE, "warden");
        store.markSeen(ALICE, "allay");
        store.markSeen(BOB, "wither");
        assertThat(store.seen(ALICE)).containsExactlyInAnyOrder("warden", "allay");
    }
}
