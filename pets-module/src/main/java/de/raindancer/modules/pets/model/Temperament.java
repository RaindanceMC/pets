package de.raindancer.modules.pets.model;

import java.util.Random;

/**
 * A pet's nature: three traits it was born with, and how far its owner has bent each — never more than
 * {@link #MOST_NUDGE} either way, so a timid pet can be made bolder but never fearless.
 */
public record Temperament(int innateCourage, int innateEnergy, int innateTemper,
                          int nudgeCourage, int nudgeEnergy, int nudgeTemper, int innateCuriosity, int nudgeCuriosity) {

    /** How far an owner can bend one trait. */
    public static final int MOST_NUDGE = 30;
    /** How far the least and the most curious pet explores, in blocks. */
    public static final double LEAST_WANDER = 3;
    public static final double MOST_WANDER = 12;
    /** How much one trait at its extreme changes what it governs. */
    private static final double SWAY = 0.15;

    public enum Trait {
        /** Low: backs off when hurt. High: lasts longer. */
        COURAGE,
        /** Low: slow. High: quick. */
        ENERGY,
        /** Low: gentle, heals more. High: hits harder. */
        TEMPER,
        /** Low: stays close. High: explores far and often while you stand about. */
        CURIOSITY
    }

    public Temperament {
        innateCourage = clamp(innateCourage, 100);
        innateEnergy = clamp(innateEnergy, 100);
        innateTemper = clamp(innateTemper, 100);
        nudgeCourage = clamp(nudgeCourage, MOST_NUDGE);
        nudgeEnergy = clamp(nudgeEnergy, MOST_NUDGE);
        nudgeTemper = clamp(nudgeTemper, MOST_NUDGE);
        innateCuriosity = clamp(innateCuriosity, 100);
        nudgeCuriosity = clamp(nudgeCuriosity, MOST_NUDGE);
    }

    public static Temperament of(Personality personality) {
        return new Temperament(personality.courage(), personality.energy(), personality.temper(), 0, 0, 0,
                personality.curiosity(), 0);
    }

    /** Born near one personality, picked at random, with some scatter of its own. */
    public static Temperament born(Random random) {
        Personality[] all = Personality.values();
        Personality around = all[random.nextInt(all.length)];
        return new Temperament(around.courage() + scatter(random), around.energy() + scatter(random),
                around.temper() + scatter(random), 0, 0, 0, around.curiosity() + scatter(random), 0);
    }

    private static int scatter(Random random) {
        return random.nextInt(31) - 15;
    }

    public int courage() {
        return clamp(innateCourage + nudgeCourage, 100);
    }

    public int energy() {
        return clamp(innateEnergy + nudgeEnergy, 100);
    }

    public int temper() {
        return clamp(innateTemper + nudgeTemper, 100);
    }

    public int curiosity() {
        return clamp(innateCuriosity + nudgeCuriosity, 100);
    }

    public Personality personality() {
        return Personality.nearest(courage(), energy(), temper(), curiosity());
    }

    /** How far it explores while its owner stands about. */
    public double wanderRadius() {
        return LEAST_WANDER + (MOST_WANDER - LEAST_WANDER) * (curiosity() + 100) / 200.0;
    }

    /** How eagerly, 0.05 to 1. */
    public double wanderEagerness() {
        return Math.max(0.05, (curiosity() + 100) / 200.0);
    }

    public Temperament nudge(Trait trait, int by) {
        return switch (trait) {
            case COURAGE -> new Temperament(innateCourage, innateEnergy, innateTemper, nudgeCourage + by,
                    nudgeEnergy, nudgeTemper, innateCuriosity, nudgeCuriosity);
            case ENERGY -> new Temperament(innateCourage, innateEnergy, innateTemper, nudgeCourage,
                    nudgeEnergy + by, nudgeTemper, innateCuriosity, nudgeCuriosity);
            case TEMPER -> new Temperament(innateCourage, innateEnergy, innateTemper, nudgeCourage, nudgeEnergy,
                    nudgeTemper + by, innateCuriosity, nudgeCuriosity);
            case CURIOSITY -> new Temperament(innateCourage, innateEnergy, innateTemper, nudgeCourage, nudgeEnergy,
                    nudgeTemper, innateCuriosity, nudgeCuriosity + by);
        };
    }

    /** What an admin set: that nature exactly, the training forgotten. */
    public Temperament becoming(Personality personality) {
        return of(personality);
    }

    public double damageFactor() {
        return 1 + SWAY * temper() / 100.0;
    }

    public double healFactor() {
        return 1 - SWAY * temper() / 100.0;
    }

    public double speedFactor() {
        return 1 + SWAY * energy() / 100.0;
    }

    public double healthFactor() {
        return 1 + SWAY * courage() / 100.0;
    }

    /** Below this share of its health a pet backs off to its owner; 0 for one that never does. */
    public double fleesBelow() {
        return courage() >= 0 ? 0 : -courage() / 100.0 * 0.4;
    }

    private static int clamp(int value, int bound) {
        return Math.max(-bound, Math.min(bound, value));
    }
}
