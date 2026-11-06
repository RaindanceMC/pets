package de.raindancer.modules.pets.store;

import de.raindancer.core.data.sql.Database;
import de.raindancer.modules.pets.model.Mode;
import de.raindancer.modules.pets.model.Pet;
import de.raindancer.modules.pets.model.Tactic;
import de.raindancer.modules.pets.model.Temperament;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Every player's pets, and how far each player is towards earning a species. Disk work: call off the main thread. */
public final class PetStore {

    private final Database database;

    public PetStore(Database database) {
        this.database = database;
    }

    public List<Pet> of(UUID owner) {
        return database.read(connection -> {
            List<Pet> pets = new ArrayList<>();
            try (PreparedStatement query = connection.prepareStatement(
                    "SELECT id, species, name, level, xp, mode, out_now, resting_until, adopted, courage, energy, "
                            + "temper, nudge_courage, nudge_energy, nudge_temper, style, particle, particle_shape, curiosity, nudge_curiosity, muted, tactic FROM pets WHERE owner = ? "
                            + "ORDER BY adopted, id")) {
                query.setString(1, owner.toString());
                try (ResultSet rows = query.executeQuery()) {
                    while (rows.next()) {
                        pets.add(new Pet(UUID.fromString(rows.getString(1)), owner, rows.getString(2),
                                rows.getString(3), rows.getInt(4), rows.getLong(5),
                                Mode.parse(rows.getString(6)).orElse(Mode.DEFENSIVE), rows.getInt(7) != 0,
                                Instant.ofEpochMilli(rows.getLong(8)), Instant.ofEpochMilli(rows.getLong(9)),
                                new Temperament(rows.getInt(10), rows.getInt(11), rows.getInt(12), rows.getInt(13),
                                        rows.getInt(14), rows.getInt(15), rows.getInt(19), rows.getInt(20)), rows.getString(16),
                                rows.getString(17), rows.getString(18), rows.getInt(21) != 0,
                                Tactic.parse(rows.getString(22)).orElse(Tactic.BALANCED)));
                    }
                }
            }
            return pets;
        }).orElse(List.of());
    }

    public boolean save(Pet pet) {
        return database.write(connection -> {
            try (PreparedStatement row = connection.prepareStatement(
                    "INSERT INTO pets (id, owner, species, name, level, xp, mode, out_now, resting_until, adopted, "
                            + "courage, energy, temper, nudge_courage, nudge_energy, nudge_temper, style, particle, particle_shape, curiosity, nudge_curiosity, muted, tactic) "
                            + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET "
                            + "owner = excluded.owner, species = excluded.species, name = excluded.name, "
                            + "level = excluded.level, xp = excluded.xp, mode = excluded.mode, "
                            + "out_now = excluded.out_now, resting_until = excluded.resting_until, "
                            + "courage = excluded.courage, energy = excluded.energy, temper = excluded.temper, "
                            + "nudge_courage = excluded.nudge_courage, nudge_energy = excluded.nudge_energy, "
                            + "nudge_temper = excluded.nudge_temper, style = excluded.style, particle = excluded.particle, "
                            + "particle_shape = excluded.particle_shape, curiosity = excluded.curiosity, "
                            + "nudge_curiosity = excluded.nudge_curiosity, muted = excluded.muted, tactic = excluded.tactic")) {
                Temperament nature = pet.temperament();
                row.setString(1, pet.id().toString());
                row.setString(2, pet.owner().toString());
                row.setString(3, pet.species());
                row.setString(4, pet.name());
                row.setInt(5, pet.level());
                row.setLong(6, pet.xp());
                row.setString(7, pet.mode().name());
                row.setInt(8, pet.out() ? 1 : 0);
                row.setLong(9, pet.restingUntil().toEpochMilli());
                row.setLong(10, pet.adopted().toEpochMilli());
                row.setInt(11, nature.innateCourage());
                row.setInt(12, nature.innateEnergy());
                row.setInt(13, nature.innateTemper());
                row.setInt(14, nature.nudgeCourage());
                row.setInt(15, nature.nudgeEnergy());
                row.setInt(16, nature.nudgeTemper());
                row.setString(17, pet.style());
                row.setString(18, pet.particle());
                row.setString(19, pet.particleShape());
                row.setInt(20, nature.innateCuriosity());
                row.setInt(21, nature.nudgeCuriosity());
                row.setInt(22, pet.muted() ? 1 : 0);
                row.setString(23, pet.tactic().name());
                row.executeUpdate();
            }
        });
    }

    /** Takes a pet away for good — an admin's {@code /pets admin take}. */
    public boolean remove(UUID pet) {
        return database.write(connection -> {
            try (PreparedStatement row = connection.prepareStatement("DELETE FROM pets WHERE id = ?")) {
                row.setString(1, pet.toString());
                row.executeUpdate();
            }
        });
    }

    /** The species a player has seen out with somebody. */
    public java.util.Set<String> seen(UUID owner) {
        return database.read(connection -> {
            java.util.Set<String> seen = new java.util.HashSet<>();
            try (PreparedStatement query = connection.prepareStatement("SELECT species FROM seen WHERE owner = ?")) {
                query.setString(1, owner.toString());
                try (ResultSet rows = query.executeQuery()) {
                    while (rows.next()) {
                        seen.add(rows.getString(1));
                    }
                }
            }
            return seen;
        }).orElse(java.util.Set.of());
    }

    public boolean markSeen(UUID owner, String species) {
        return database.write(connection -> {
            try (PreparedStatement row = connection.prepareStatement(
                    "INSERT INTO seen (owner, species) VALUES (?,?) ON CONFLICT(owner, species) DO NOTHING")) {
                row.setString(1, owner.toString());
                row.setString(2, species);
                row.executeUpdate();
            }
        });
    }

    /** Done so far towards each species being earned. */
    public Map<String, Long> progress(UUID owner) {
        return database.read(connection -> {
            Map<String, Long> done = new HashMap<>();
            try (PreparedStatement query = connection.prepareStatement(
                    "SELECT species, done FROM progress WHERE owner = ?")) {
                query.setString(1, owner.toString());
                try (ResultSet rows = query.executeQuery()) {
                    while (rows.next()) {
                        done.put(rows.getString(1), rows.getLong(2));
                    }
                }
            }
            return done;
        }).orElse(Map.of());
    }

    public boolean addProgress(UUID owner, String species, long amount) {
        return database.write(connection -> {
            try (PreparedStatement row = connection.prepareStatement(
                    "INSERT INTO progress (owner, species, done) VALUES (?,?,?) ON CONFLICT(owner, species) "
                            + "DO UPDATE SET done = progress.done + excluded.done")) {
                row.setString(1, owner.toString());
                row.setString(2, species);
                row.setLong(3, amount);
                row.executeUpdate();
            }
        });
    }
}
