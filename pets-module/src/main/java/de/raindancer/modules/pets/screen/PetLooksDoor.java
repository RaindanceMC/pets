package de.raindancer.modules.pets.screen;

import de.raindancer.core.ui.cosmetic.CosmeticDoor;
import de.raindancer.core.ui.menu.Icons;
import de.raindancer.core.ui.menu.Menu;
import de.raindancer.modules.pets.PetServices;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Pets' particles and name colours as a door on the server's cosmetics page. They belong to one pet each, so the
 * door leads to the pets, and each pet's page has its particles and colours.
 */
public final class PetLooksDoor implements CosmeticDoor {

    private final Supplier<PetServices> services;

    public PetLooksDoor(Supplier<PetServices> services) {
        this.services = services;
    }

    @Override
    public String id() {
        return "pets:looks";
    }

    @Override
    public ItemStack icon(Player viewer) {
        return Icons.of(Material.BONE, "<white>Pet looks",
                "<gray>Particles and a coloured name", "<gray>for each of your pets.", "",
                "<dark_gray>Click, pick a pet, then Particles", "<dark_gray>or Colour its name.");
    }

    @Override
    public Optional<String> locked(Player viewer) {
        return services.get().care().mayDecorate(viewer) ? Optional.empty() : Optional.of("A Prime perk. See /prime");
    }

    @Override
    public void open(Player viewer, Menu parent) {
        new PetCollectionMenu(services.get(), viewer, parent).open();
    }
}
