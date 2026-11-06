package de.raindancer.modules.pets.model;

import java.time.Instant;
import java.util.UUID;

/**
 * One pet a player has.
 *
 * @param out          whether it was out when last saved — it comes back with its owner
 * @param restingUntil knocked out until then; {@link Instant#EPOCH} when awake
 * @param style        how its name is painted, as Core's {@code NameStyle.encode()}; empty for plain
 * @param particle     the particle effect it wears, empty for none, and its shape ({@code ParticleShape} name)
 */
public record Pet(UUID id, UUID owner, String species, String name, int level, long xp, Mode mode, boolean out,
                  Instant restingUntil, Instant adopted, Temperament temperament, String style, String particle,
                  String particleShape) {

    public static Pet adopt(UUID owner, String species, String name, Temperament temperament, Instant now) {
        return new Pet(UUID.randomUUID(), owner, species, name, 1, 0, Mode.DEFENSIVE, false, Instant.EPOCH, now,
                temperament, "", "", "AURA");
    }

    public Personality personality() {
        return temperament.personality();
    }

    public Pet withStyle(String style) {
        return new Pet(id, owner, species, name, level, xp, mode, out, restingUntil, adopted, temperament,
                style == null ? "" : style, particle, particleShape);
    }

    public Pet withParticle(String particle, String shape) {
        return new Pet(id, owner, species, name, level, xp, mode, out, restingUntil, adopted, temperament, style,
                particle == null ? "" : particle, shape == null ? "AURA" : shape);
    }

    public Pet withTemperament(Temperament temperament) {
        return new Pet(id, owner, species, name, level, xp, mode, out, restingUntil, adopted, temperament, style, particle, particleShape);
    }

    public boolean restingAt(Instant now) {
        return restingUntil.isAfter(now);
    }

    public Pet withProgress(int level, long xp) {
        return new Pet(id, owner, species, name, level, xp, mode, out, restingUntil, adopted, temperament, style, particle, particleShape);
    }

    public Pet withMode(Mode mode) {
        return new Pet(id, owner, species, name, level, xp, mode, out, restingUntil, adopted, temperament, style, particle, particleShape);
    }

    public Pet withName(String name) {
        return new Pet(id, owner, species, name, level, xp, mode, out, restingUntil, adopted, temperament, style, particle, particleShape);
    }

    public Pet withOut(boolean out) {
        return new Pet(id, owner, species, name, level, xp, mode, out, restingUntil, adopted, temperament, style, particle, particleShape);
    }

    public Pet withRestingUntil(Instant until) {
        return new Pet(id, owner, species, name, level, xp, mode, out, until, adopted, temperament, style, particle, particleShape);
    }
}
