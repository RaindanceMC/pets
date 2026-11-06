package de.raindancer.modules.pets.model;

import java.time.Instant;
import java.util.UUID;

/**
 * One pet a player has.
 *
 * @param out          whether it was out when last saved — it comes back with its owner
 * @param restingUntil knocked out until then; {@link Instant#EPOCH} when awake
 * @param style        how its name is painted, as Core's {@code NameStyle.encode()}; empty for plain
 * @param muted        makes no sound — its owner muted it
 * @param particle     the particle effect it wears, empty for none, and its shape ({@code ParticleShape} name)
 * @param tactic       how it goes about a fight
 * @param skills       the ranks it took in the skill tree, by node
 * @param skillsReset  when its tree was last reset, epoch milliseconds; 0 never
 */
public record Pet(UUID id, UUID owner, String species, String name, int level, long xp, Mode mode, boolean out,
                  Instant restingUntil, Instant adopted, Temperament temperament, String style, String particle,
                  String particleShape, boolean muted, Tactic tactic,
                  java.util.Map<String, Integer> skills, long skillsReset) {

    public Pet {
        tactic = tactic == null ? Tactic.BALANCED : tactic;
        skills = skills == null ? java.util.Map.of() : java.util.Map.copyOf(skills);
    }

    /** The shape before the skill tree. */
    public Pet(UUID id, UUID owner, String species, String name, int level, long xp, Mode mode, boolean out,
               Instant restingUntil, Instant adopted, Temperament temperament, String style, String particle,
               String particleShape, boolean muted, Tactic tactic) {
        this(id, owner, species, name, level, xp, mode, out, restingUntil, adopted, temperament, style, particle,
                particleShape, muted, tactic, java.util.Map.of(), 0);
    }

    public Pet withSkills(java.util.Map<String, Integer> skills, long reset) {
        return new Pet(id, owner, species, name, level, xp, mode, out, restingUntil, adopted, temperament, style,
                particle, particleShape, muted, tactic, skills, reset);
    }

    /** The ranks as kept in the database: {@code node:rank,node:rank}. */
    public String encodeSkills() {
        return String.join(",", new java.util.TreeMap<>(skills).entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue()).toList());
    }

    public static java.util.Map<String, Integer> decodeSkills(String encoded) {
        java.util.Map<String, Integer> read = new java.util.HashMap<>();
        if (encoded == null || encoded.isBlank()) {
            return read;
        }
        for (String part : encoded.split(",")) {
            String[] pair = part.trim().split(":", 2);
            try {
                if (pair.length == 2 && !pair[0].isBlank() && Integer.parseInt(pair[1].trim()) > 0) {
                    read.put(pair[0].trim(), Integer.parseInt(pair[1].trim()));
                }
            } catch (NumberFormatException unreadable) {
                // a part that is not node:rank is dropped, the rest kept
            }
        }
        return read;
    }

    public static Pet adopt(UUID owner, String species, String name, Temperament temperament, Instant now) {
        return new Pet(UUID.randomUUID(), owner, species, name, 1, 0, Mode.DEFENSIVE, false, Instant.EPOCH, now,
                temperament, "", "", "AURA", false, Tactic.BALANCED);
    }

    public Personality personality() {
        return temperament.personality();
    }

    public Pet withStyle(String style) {
        return new Pet(id, owner, species, name, level, xp, mode, out, restingUntil, adopted, temperament,
                style == null ? "" : style, particle, particleShape, muted, tactic, skills, skillsReset);
    }

    public Pet withParticle(String particle, String shape) {
        return new Pet(id, owner, species, name, level, xp, mode, out, restingUntil, adopted, temperament, style,
                particle == null ? "" : particle, shape == null ? "AURA" : shape, muted, tactic, skills, skillsReset);
    }

    public Pet withMuted(boolean muted) {
        return new Pet(id, owner, species, name, level, xp, mode, out, restingUntil, adopted, temperament, style,
                particle, particleShape, muted, tactic, skills, skillsReset);
    }

    public Pet withTemperament(Temperament temperament) {
        return new Pet(id, owner, species, name, level, xp, mode, out, restingUntil, adopted, temperament, style, particle, particleShape, muted, tactic, skills, skillsReset);
    }

    public boolean restingAt(Instant now) {
        return restingUntil.isAfter(now);
    }

    public Pet withProgress(int level, long xp) {
        return new Pet(id, owner, species, name, level, xp, mode, out, restingUntil, adopted, temperament, style, particle, particleShape, muted, tactic, skills, skillsReset);
    }

    public Pet withMode(Mode mode) {
        return new Pet(id, owner, species, name, level, xp, mode, out, restingUntil, adopted, temperament, style, particle, particleShape, muted, tactic, skills, skillsReset);
    }

    public Pet withName(String name) {
        return new Pet(id, owner, species, name, level, xp, mode, out, restingUntil, adopted, temperament, style, particle, particleShape, muted, tactic, skills, skillsReset);
    }

    public Pet withOut(boolean out) {
        return new Pet(id, owner, species, name, level, xp, mode, out, restingUntil, adopted, temperament, style, particle, particleShape, muted, tactic, skills, skillsReset);
    }

    public Pet withRestingUntil(Instant until) {
        return new Pet(id, owner, species, name, level, xp, mode, out, until, adopted, temperament, style, particle, particleShape, muted, tactic, skills, skillsReset);
    }

    public Pet withTactic(Tactic tactic) {
        return new Pet(id, owner, species, name, level, xp, mode, out, restingUntil, adopted, temperament, style, particle, particleShape, muted, tactic, skills, skillsReset);
    }
}
