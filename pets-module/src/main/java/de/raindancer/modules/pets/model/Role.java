package de.raindancer.modules.pets.model;

import java.util.Locale;
import java.util.Optional;

/** What a pet does in a fight. */
public enum Role {

    /** Bites whatever it is set on. */
    FIGHTER("Fighter", "Goes for the throat"),
    /** Tough, and draws the monsters that go for its owner onto itself. */
    GUARD("Guard", "Takes the hits meant for you"),
    /** Keeps its distance and throws. */
    RANGED("Ranged", "Fights from afar"),
    /** Mends its owner and itself. */
    HEALER("Healer", "Keeps you standing"),
    /** Fetches loose drops and experience to its owner. */
    GATHERER("Gatherer", "Brings you what lies about"),
    /** Keeps its owner quicker, stronger, tougher. */
    SUPPORT("Support", "Sings you on"),
    /** Fast, and marks the monsters near its owner for their eyes. */
    SCOUT("Scout", "Sees them coming");

    private final String title;
    private final String blurb;

    Role(String title, String blurb) {
        this.title = title;
        this.blurb = blurb;
    }

    /** How Core's companions fight for this role. */
    public de.raindancer.core.world.companion.Fighting fighting() {
        return switch (this) {
            case FIGHTER -> de.raindancer.core.world.companion.Fighting.MELEE;
            case GUARD -> de.raindancer.core.world.companion.Fighting.GUARD;
            case RANGED -> de.raindancer.core.world.companion.Fighting.RANGED;
            case HEALER -> de.raindancer.core.world.companion.Fighting.HEALER;
            case GATHERER, SUPPORT, SCOUT -> de.raindancer.core.world.companion.Fighting.MELEE;
        };
    }

    /** Its node in the skill tree: taking it makes a pet of any species follow this role. */
    public String calling() {
        return "calling_" + name().toLowerCase(Locale.ROOT);
    }

    public String title() {
        return title;
    }

    public String blurb() {
        return blurb;
    }

    public static Optional<Role> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(raw.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException unknown) {
            return Optional.empty();
        }
    }
}
