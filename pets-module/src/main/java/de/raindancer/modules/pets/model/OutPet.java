package de.raindancer.modules.pets.model;

import de.raindancer.core.world.companion.Companion;

/** A pet out in the world right now, and the companion Core drives for it. */
public final class OutPet {

    private volatile Pet pet;
    private final Species species;
    private final Companion companion;
    private volatile double ridden;
    private volatile long lastFed;

    public OutPet(Pet pet, Species species, Companion companion) {
        this.pet = pet;
        this.species = species;
        this.companion = companion;
    }

    public Pet pet() {
        return pet;
    }

    public void pet(Pet changed) {
        pet = changed;
    }

    public Species species() {
        return species;
    }

    public Companion companion() {
        return companion;
    }

    /** Adds blocks ridden; @return the total since the last {@link #resetRide} */
    public double ride(double blocks) {
        ridden += blocks;
        return ridden;
    }

    public void resetRide() {
        ridden = 0;
    }

    public long lastFed() {
        return lastFed;
    }

    public void fedAt(long millis) {
        lastFed = millis;
    }
}
