package de.raindancer.modules.pets.screen;

import de.raindancer.core.ui.menu.Icons;
import de.raindancer.core.ui.menu.Menu;
import de.raindancer.core.ui.menu.PaginatedMenu;
import de.raindancer.modules.pets.PetServices;
import de.raindancer.modules.pets.model.Pet;
import de.raindancer.modules.pets.model.Species;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Every pet a player has: what bare {@code /pets} opens. Click calls or sends home; right click opens the pet. */
public final class PetCollectionMenu extends PaginatedMenu<Pet> implements IPetScreen {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final PetServices services;

    public PetCollectionMenu(PetServices services, Player viewer, Menu parent) {
        super(viewer, services.brand(), parent);
        this.services = services;
    }

    @Override
    protected Component title() {
        return MINI.deserialize("<dark_gray>Your pets");
    }

    @Override
    public String breadcrumb() {
        return "Your pets";
    }

    @Override
    protected List<Pet> entries() {
        List<Pet> pets = new ArrayList<>(services.collection().pets(viewer.getUniqueId()));
        pets.sort(Comparator.comparing((Pet pet) -> services.collection().speciesOf(pet)
                .map(Species::rarity).orElse(null), Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Pet::name));
        return pets;
    }

    @Override
    protected ItemStack emptyIcon() {
        return Icons.of(Material.LEAD, "<gray>No pets yet",
                "<gray>Every species, and how to get it,", "<gray>is behind the egg below.", "",
                "<yellow>Click<gray> to look at them.");
    }

    @Override
    protected void emptyAction(InventoryClickEvent event) {
        new SpeciesMenu(services, viewer, this).open();
    }

    @Override
    protected ItemStack icon(Pet pet) {
        Species species = services.collection().speciesOf(pet).orElse(null);
        if (species == null) {
            return Icons.of(Material.BARRIER, "<red>" + pet.name(), "<gray>Its species is no longer offered here.");
        }
        List<String> lore = new ArrayList<>(PetLook.describe(pet, species, services.summoning().levels()));
        lore.add("");
        boolean out = services.summoning().isOut(pet.id());
        if (out) {
            lore.add("<green>Out with you.");
            lore.add("<yellow>Click<gray> to send it home.");
        } else if (pet.restingAt(Instant.now())) {
            lore.add("<red>Knocked out — back in "
                    + Math.max(1, Duration.between(Instant.now(), pet.restingUntil()).toSeconds()) + "s.");
        } else {
            lore.add("<yellow>Click<gray> to call it.");
        }
        lore.add("<yellow>Right click<gray> for its name, colours and mode.");
        return Icons.of(species.icon(), "<white>" + pet.name(), lore);
    }

    @Override
    protected void onClick(Pet pet, InventoryClickEvent event) {
        if (event.isRightClick()) {
            new PetMenu(services, viewer, this, pet.id()).open();
            return;
        }
        if (services.summoning().isOut(pet.id())) {
            services.care().home(viewer, pet);
        } else {
            services.care().call(viewer, pet);
        }
        refresh();
    }

    @Override
    protected void decorate() {
        super.decorate();
        toolbar(4, Icons.of(Material.TURTLE_EGG, "<white>Every species",
                        "<gray>What there is, how rare,", "<gray>and how to get each one.", "",
                        "<yellow>Click<gray> to look."),
                click -> new SpeciesMenu(services, viewer, this).open());
        boolean seeing = de.raindancer.modules.pets.service.CareService.SEE_OTHERS.isOn(viewer);
        toolbar(6, Icons.of(seeing ? Material.ENDER_EYE : Material.ENDER_PEARL,
                        seeing ? "<white>Hide other players' pets" : "<white>Show other players' pets",
                        seeing ? "<gray>Theirs out of your sight and ears." : "<gray>You see only your own now.", "",
                        "<yellow>Click<gray> to switch."),
                click -> {
                    services.care().toggleOthers(viewer);
                    refresh();
                });
    }

    @Override
    protected List<String> helpLines() {
        return List.of("<gray>Click a pet to call it or send it home.",
                "<gray>Right click for its name, colours and mode.",
                "<gray>Right-click your pet in the world to ride it;",
                "<gray>sneak and right-click for this menu.",
                "<gray>Hold food and right-click it for a treat.");
    }
}
