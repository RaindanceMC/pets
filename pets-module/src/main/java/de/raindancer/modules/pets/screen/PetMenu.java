package de.raindancer.modules.pets.screen;

import de.raindancer.core.ui.choose.StyleEditor;
import de.raindancer.core.ui.menu.Icons;
import de.raindancer.core.ui.menu.Menu;
import de.raindancer.core.ui.menu.MenuLayout;
import de.raindancer.core.ui.text.NameStyle;
import de.raindancer.modules.pets.PetServices;
import de.raindancer.modules.pets.model.Pet;
import de.raindancer.modules.pets.model.Species;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** One pet: call it or send it home, its mode, stay or follow, its name and the colours of its name. */
public final class PetMenu extends Menu implements IPetScreen {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final PetServices services;
    private final UUID petId;

    public PetMenu(PetServices services, Player viewer, Menu parent, UUID petId) {
        super(viewer, services.brand(), parent);
        this.services = services;
        this.petId = petId;
    }

    private Optional<Pet> pet() {
        return services.collection().pet(viewer.getUniqueId(), petId);
    }

    @Override
    protected Component title() {
        return MINI.deserialize("<dark_gray>" + pet().map(Pet::name).orElse("Your pet"));
    }

    @Override
    public String breadcrumb() {
        return pet().map(Pet::name).orElse("Your pet");
    }

    @Override
    protected void render() {
        Pet pet = pet().orElse(null);
        Species species = pet == null ? null : services.collection().speciesOf(pet).orElse(null);
        if (pet == null || species == null) {
            band(MenuLayout.RULES, 4, Icons.of(Material.BARRIER, "<red>Gone", "<gray>This pet is not yours any more."));
            return;
        }
        List<String> about = new ArrayList<>(PetLook.describe(pet, species, services.summoning().levels()));
        band(MenuLayout.WHO, 4, Icons.of(species.icon(), "<white>" + pet.name(), about));

        boolean out = services.summoning().isOut(pet.id());
        band(MenuLayout.RULES, 2, out
                        ? Icons.of(Material.OAK_DOOR, "<white>Send it home", "<gray>It comes when you call again.", "",
                        "<yellow>Click<gray> to send it.")
                        : Icons.of(Material.LEAD, "<white>Call it", "<gray>Out beside you.", "",
                        "<yellow>Click<gray> to call it."),
                click -> {
                    if (services.summoning().isOut(pet.id())) {
                        services.care().home(viewer, pet);
                    } else {
                        services.care().call(viewer, pet);
                    }
                    refresh();
                });
        boolean staying = services.summoning().byPet(pet.id()).map(live -> live.companion().staying()).orElse(false);
        band(MenuLayout.RULES, 4, out, staying
                        ? Icons.of(Material.FEATHER, "<white>Follow me", "<gray>It stays where it is now.", "",
                        "<yellow>Click<gray> to have it follow.")
                        : Icons.of(Material.ANVIL, "<white>Stay here", "<gray>It follows you now.", "",
                        "<yellow>Click<gray> to have it wait."),
                "It is not out", click -> {
                    services.care().stay(viewer, pet, !staying);
                    refresh();
                });
        band(MenuLayout.RULES, 6, Icons.of(Material.IRON_SWORD, "<white>Mode: " + pet.mode().title(),
                        "<gray>" + pet.mode().blurb(), "", "<yellow>Click<gray> for " + pet.mode().next().title() + "."),
                click -> {
                    services.care().mode(viewer, pet, pet.mode().next());
                    refresh();
                });
        band(MenuLayout.LAND, 7, pet.muted()
                        ? Icons.of(Material.NOTE_BLOCK, "<white>Let it be heard", "<gray>Quiet now.", "",
                        "<yellow>Click<gray> to unmute it.")
                        : Icons.of(Material.WHITE_WOOL, "<white>Mute it", "<gray>No sound from " + pet.name() + ",",
                        "<gray>for anybody.", "", "<yellow>Click<gray> to mute it."),
                click -> {
                    services.care().mute(viewer, pet);
                    refresh();
                });
        band(MenuLayout.RULES, 7, Icons.of(Material.SHIELD, "<white>Tactic: " + pet.tactic().title(),
                        "<gray>" + pet.tactic().blurb(), "", "<yellow>Click<gray> for " + pet.tactic().next().title() + "."),
                click -> {
                    services.care().tactic(viewer, pet, pet.tactic().next());
                    refresh();
                });
        int left = services.summoning().tree().left(pet.level(), pet.skills(), species);
        band(MenuLayout.WHO, 6, Icons.of(Material.ENCHANTED_BOOK, "<white>Skill tree",
                        "<gray>A point every level: its role, its strengths,", "<gray>its power when ridden.",
                        "<gray>Role now: <white>" + services.summoning().tree().role(species, pet.skills()).title(),
                        "<green>Points to spend: <white>" + left, "", "<yellow>Click<gray> to open it."),
                click -> new PetTreeMenu(services, viewer, this, pet.id()).open());
        band(MenuLayout.LAND, 3, Icons.of(Material.NAME_TAG, "<white>Rename it",
                        "<gray>Called " + pet.name() + " now.", "", "<yellow>Click<gray> to type its new name."),
                click -> rename(pet));
        boolean prime = services.care().mayDecorate(viewer);
        band(MenuLayout.LAND, 4, Icons.of(Material.BLAZE_POWDER, "<white>Particles",
                        "<gray>Any effect you may wear yourself,", "<gray>worn around " + pet.name() + ".", "",
                        prime ? "<yellow>Click<gray> to pick one." : "<gold>✦ A Prime perk<gray> — see /prime"),
                click -> {
                    if (prime) {
                        new PetParticleMenu(services, viewer, this, pet.id()).open();
                    } else {
                        services.care().primePerk(viewer);
                    }
                });
        band(MenuLayout.LAND, 5, Icons.of(Material.BRUSH, "<white>Colour its name",
                        "<gray>Painted like your own name can be:", "<gray>colours, gradients, bold and the rest.", "",
                        prime ? "<yellow>Click<gray> to paint it." : "<gold>✦ A Prime perk<gray> — see /prime"),
                click -> {
                    if (!prime) {
                        services.care().primePerk(viewer);
                        return;
                    }
                    StyleEditor.of(viewer, services.brand(), this)
                            .heading("Colours for " + pet.name())
                            .sample(() -> pet().map(Pet::name).orElse(pet.name()))
                            .current(() -> pet().map(now -> NameStyle.parse(now.style())).orElse(NameStyle.NONE))
                            .onChange(style -> pet().ifPresent(now -> services.care().style(now, style)))
                            .grants(services.care().styleGrantsOf(viewer))
                            .open();
                });
    }

    /** In a window rather than in chat: the player stays in the menus, and comes back to this page either way. */
    private void rename(Pet pet) {
        de.raindancer.core.ui.prompt.AnvilInput.open(viewer, "A name for " + pet.name(), pet.name(),
                de.raindancer.core.ui.prompt.Parsers.text(de.raindancer.modules.pets.rules.PetNameRule.LONGEST),
                answer -> {
                    pet().ifPresent(now -> services.care().rename(viewer, now, answer));
                    open();
                }, this::open);
    }

    @Override
    protected List<String> helpLines() {
        return List.of("<gray>Everything about one pet.",
                "<gray>Its nature changes a little with how you treat it:",
                "<gray>fighting together makes it bolder, riding livelier,",
                "<gray>treats gentler, exploring with you more curious.",
                "<gray>Stand about and it goes exploring nearby.");
    }
}
