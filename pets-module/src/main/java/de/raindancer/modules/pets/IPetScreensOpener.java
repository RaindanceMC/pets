package de.raindancer.modules.pets;

import de.raindancer.modules.pets.model.Pet;
import org.bukkit.entity.Player;

/** Opening the screens, so nothing but the module knows the menus exist. */
public interface IPetScreensOpener {

    void collection(Player viewer);

    void species(Player viewer);

    void pet(Player viewer, Pet pet);

    void particles(Player viewer, Pet pet);

    void skills(Player viewer, Pet pet);
}
