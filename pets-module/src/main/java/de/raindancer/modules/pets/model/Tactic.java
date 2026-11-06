package de.raindancer.modules.pets.model;

import java.util.Locale;
import java.util.Optional;

/** How a pet goes about a fight — its owner's choice. */
public enum Tactic {

    BALANCED("Balanced", "Circles its foe and darts in to bite"),
    RELENTLESS("Relentless", "Presses in and never lets go"),
    HIT_AND_RUN("Hit and run", "Bites, then backs off out of reach"),
    CAUTIOUS("Cautious", "Fights only close to you, comes back early when hurt");

    private final String title;
    private final String blurb;

    Tactic(String title, String blurb) {
        this.title = title;
        this.blurb = blurb;
    }

    public String title() {
        return title;
    }

    public String blurb() {
        return blurb;
    }

    public de.raindancer.core.world.companion.Tactic core() {
        return de.raindancer.core.world.companion.Tactic.valueOf(name());
    }

    public Tactic next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static Optional<Tactic> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(raw.trim().toUpperCase(Locale.ROOT).replace('-', '_')));
        } catch (IllegalArgumentException unknown) {
            return Optional.empty();
        }
    }
}
