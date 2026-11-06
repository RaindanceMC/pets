package de.raindancer.modules.pets.service;

import de.raindancer.modules.pets.PetSettings;

/** Does, and decides as little as possible: asks a rule. Holds a settings snapshot, swapped on every reload. */
public interface IPetService {

    void settings(PetSettings fresh);
}
