package de.raindancer.modules.pets.screen;

import de.raindancer.core.ui.choose.ParticleCatalogue;
import de.raindancer.core.ui.choose.ParticleChooser;
import de.raindancer.core.ui.effect.ParticleShape;
import de.raindancer.core.ui.menu.Icons;
import de.raindancer.core.ui.menu.Menu;
import de.raindancer.core.ui.menu.MenuLayout;
import de.raindancer.modules.pets.PetServices;
import de.raindancer.modules.pets.model.Pet;
import de.raindancer.modules.pets.service.CareService;
import de.raindancer.modules.pets.service.SummonService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A pet's particle effect, laid out like the cosmetics page: Core's particle chooser (groups, a preview on a left
 * click) holding only what the owner may wear themselves, what it wears now, taking it off, and the shapes that fit
 * a small body.
 */
public final class PetParticleMenu extends Menu implements IPetScreen {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final PetServices services;
    private final UUID petId;

    public PetParticleMenu(PetServices services, Player viewer, Menu parent, UUID petId) {
        super(viewer, services.brand(), parent);
        this.services = services;
        this.petId = petId;
    }

    private Optional<Pet> pet() {
        return services.collection().pet(viewer.getUniqueId(), petId);
    }

    @Override
    protected Component title() {
        return MINI.deserialize("<dark_gray>Particles");
    }

    @Override
    public String breadcrumb() {
        return "Particles";
    }

    @Override
    protected void render() {
        Pet pet = pet().orElse(null);
        if (pet == null) {
            band(MenuLayout.RULES, 4, Icons.of(Material.BARRIER, "<red>Gone", "<gray>This pet is not yours any more."));
            return;
        }
        List<String> mine = services.care().particlesOf(viewer);
        ParticleShape shape = SummonService.shape(pet.particleShape());
        boolean wearing = !pet.particle().isEmpty();

        band(MenuLayout.WHO, 2, !mine.isEmpty(),
                Icons.of(Material.BLAZE_POWDER, "<green>Pick a particle",
                        "<gray>Any effect you may wear yourself.", "",
                        "<dark_gray>Left-click one to see it, right-click to put it on."),
                "You may not wear any particles yet — see /cosmetics",
                click -> new ParticleChooser(viewer, services.brand(), this, "Pick a particle",
                        particle -> pet().ifPresent(now -> services.care().particle(viewer, now, particle,
                                SummonService.shape(now.particleShape()))),
                        new ParticleCatalogue(() -> services.care().particlesOf(viewer))).open());

        band(MenuLayout.WHO, 4, wearing
                ? Icons.of(icon(pet.particle()), "<white>" + ParticleCatalogue.readable(pet.particle()),
                        "<gray>" + shape.title() + ".", "<dark_gray>Worn around " + pet.name() + ".")
                : Icons.of(Material.GLASS_BOTTLE, "<gray>No particle", "<dark_gray>Pick one on the left."));

        band(MenuLayout.WHO, 6, wearing,
                Icons.of(Material.BUCKET, "<white>Take it off", "<gray>No particle on " + pet.name() + " any more."),
                "Not wearing one",
                click -> {
                    pet().ifPresent(now -> services.care().particle(viewer, now, "", shape));
                    refresh();
                });

        int column = 2;
        for (ParticleShape each : CareService.shapes()) {
            boolean on = each == shape && wearing;
            band(MenuLayout.RULES, column, wearing,
                    Icons.of(on ? Material.LIME_DYE : Material.GRAY_DYE, (on ? "<green>" : "<white>") + each.title(),
                            on ? "<green>Drawn like this now." : "<gray>Click to draw it like this."),
                    "Pick a particle first",
                    click -> {
                        pet().ifPresent(now -> services.care().particle(viewer, now, now.particle(), each));
                        refresh();
                    });
            column++;
        }
    }

    private static Material icon(String particle) {
        Material material = Material.matchMaterial(ParticleCatalogue.iconFor(particle));
        return material == null ? Material.GLASS : material;
    }

    @Override
    protected List<String> helpLines() {
        return List.of("<gray>The particles you may wear yourself,", "<gray>worn around your pet.",
                "<gray>Wings are for players: a pet wears", "<gray>its effect closer to its body.");
    }
}
