package de.raindancer.modules.pets.model;

import java.util.Locale;
import java.util.Optional;

/**
 * The name for where a pet's three traits sit: the archetype nearest to them. Courage, energy and temper each run
 * from −100 to 100.
 */
public enum Personality {

    BALANCED("Balanced", "Takes things as they come", 0, 0, 0),
    BRAVE("Brave", "Never backs down", 70, 10, 10),
    TIMID("Timid", "Jumps at shadows, backs off when hurt", -70, -10, -10),
    PLAYFUL("Playful", "Bounces everywhere, quick on its feet", 0, 70, -30),
    LAZY("Lazy", "Would rather nap", 0, -70, 0),
    FIERCE("Fierce", "Bites hard, asks later", 30, 20, 70),
    GENTLE("Gentle", "Soft-hearted; mends more than it bites", -10, 0, -70),
    CHEEKY("Cheeky", "Quick, cocky and a little mean", -20, 50, 50),
    STOIC("Stoic", "Steady, slow and hard to shake", 40, -40, -20);

    private final String title;
    private final String blurb;
    private final int courage;
    private final int energy;
    private final int temper;

    Personality(String title, String blurb, int courage, int energy, int temper) {
        this.title = title;
        this.blurb = blurb;
        this.courage = courage;
        this.energy = energy;
        this.temper = temper;
    }

    public String title() {
        return title;
    }

    public String blurb() {
        return blurb;
    }

    public int courage() {
        return courage;
    }

    public int energy() {
        return energy;
    }

    public int temper() {
        return temper;
    }

    /** The one nearest to these traits. */
    public static Personality nearest(int courage, int energy, int temper) {
        Personality best = BALANCED;
        long bestDistance = Long.MAX_VALUE;
        for (Personality each : values()) {
            long dc = courage - each.courage;
            long de = energy - each.energy;
            long dt = temper - each.temper;
            long distance = dc * dc + de * de + dt * dt;
            if (distance < bestDistance) {
                best = each;
                bestDistance = distance;
            }
        }
        return best;
    }

    public static Optional<Personality> parse(String raw) {
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
