package de.raindancer.modules.pets.store;

import de.raindancer.core.data.sql.Schema;

/** The shape of {@code pets.db}. Steps are only ever appended to — see {@link Schema}. */
public final class PetDatabase {

    public static final String NAME = "pets";

    public static final Schema SCHEMA = Schema.of(
            "CREATE TABLE pets (id TEXT PRIMARY KEY, owner TEXT NOT NULL, species TEXT NOT NULL, name TEXT NOT NULL, "
                    + "level INTEGER NOT NULL DEFAULT 1, xp BIGINT NOT NULL DEFAULT 0, "
                    + "mode TEXT NOT NULL DEFAULT 'DEFENSIVE', out_now INTEGER NOT NULL DEFAULT 0, "
                    + "resting_until BIGINT NOT NULL DEFAULT 0, adopted BIGINT NOT NULL DEFAULT 0, "
                    + "courage INTEGER NOT NULL DEFAULT 0, energy INTEGER NOT NULL DEFAULT 0, "
                    + "temper INTEGER NOT NULL DEFAULT 0, nudge_courage INTEGER NOT NULL DEFAULT 0, "
                    + "nudge_energy INTEGER NOT NULL DEFAULT 0, nudge_temper INTEGER NOT NULL DEFAULT 0)",
            "CREATE INDEX pets_owner ON pets (owner)",
            "CREATE TABLE progress (owner TEXT NOT NULL, species TEXT NOT NULL, done BIGINT NOT NULL DEFAULT 0, "
                    + "PRIMARY KEY (owner, species))",
            "ALTER TABLE pets ADD COLUMN style TEXT NOT NULL DEFAULT ''",
            "CREATE TABLE seen (owner TEXT NOT NULL, species TEXT NOT NULL, PRIMARY KEY (owner, species))",
            "ALTER TABLE pets ADD COLUMN particle TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE pets ADD COLUMN particle_shape TEXT NOT NULL DEFAULT 'AURA'",
            "ALTER TABLE pets ADD COLUMN curiosity INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE pets ADD COLUMN nudge_curiosity INTEGER NOT NULL DEFAULT 0");

    private PetDatabase() {
    }
}
