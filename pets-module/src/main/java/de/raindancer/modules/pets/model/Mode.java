package de.raindancer.modules.pets.model;

import java.util.Locale;
import java.util.Optional;

/** How ready a pet is to fight. */
public enum Mode {

    /** Joins every fight its owner starts and goes for monsters nearby. */
    AGGRESSIVE("Aggressive", "Joins your fights and hunts monsters near you"),
    /** Only goes for what hurt its owner. */
    DEFENSIVE("Defensive", "Goes for whatever hurts you"),
    /** Never fights. */
    PASSIVE("Passive", "Never fights");

    private final String title;
    private final String blurb;

    Mode(String title, String blurb) {
        this.title = title;
        this.blurb = blurb;
    }

    public String title() {
        return title;
    }

    public String blurb() {
        return blurb;
    }

    public de.raindancer.core.world.companion.Stance stance() {
        return de.raindancer.core.world.companion.Stance.valueOf(name());
    }

    public Mode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static Optional<Mode> parse(String raw) {
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
