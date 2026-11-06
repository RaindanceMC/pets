package de.raindancer.modules.pets.screen;

import de.raindancer.core.ui.effect.ParticleShape;
import de.raindancer.core.ui.menu.Icons;
import de.raindancer.core.ui.menu.Menu;
import de.raindancer.core.ui.menu.PaginatedMenu;
import de.raindancer.modules.pets.PetServices;
import de.raindancer.modules.pets.model.Pet;
import de.raindancer.modules.pets.service.CareService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** A particle effect for one pet — any the owner may wear themselves — and the shape it is worn in. */
public final class PetParticleMenu extends PaginatedMenu<String> implements IPetScreen {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final String NONE = "";

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
        return MINI.deserialize("<dark_gray>Particles for " + pet().map(Pet::name).orElse("your pet"));
    }

    @Override
    public String breadcrumb() {
        return "Particles";
    }

    @Override
    protected List<String> entries() {
        List<String> all = new ArrayList<>();
        all.add(NONE);
        all.addAll(services.care().particlesOf(viewer));
        return all;
    }

    @Override
    protected ItemStack emptyIcon() {
        return Icons.of(Material.BARRIER, "<gray>No particles", "<gray>You may not wear any yet — see /cosmetics.");
    }

    @Override
    protected ItemStack icon(String particle) {
        boolean worn = pet().map(now -> now.particle().equals(particle)).orElse(false);
        if (particle.isEmpty()) {
            return Icons.of(Material.GLASS_BOTTLE, "<white>None", "<gray>No particles on it.", "",
                    worn ? "<green>Worn now." : "<yellow>Click<gray> to take them off.");
        }
        return Icons.of(worn ? Material.GLOWSTONE_DUST : Material.BLAZE_POWDER, "<white>" + readable(particle),
                "<gray>One of the effects you may wear.", "",
                worn ? "<green>Worn now." : "<yellow>Click<gray> to put it on.");
    }

    private static String readable(String particle) {
        String words = particle.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }

    @Override
    protected void onClick(String particle, InventoryClickEvent event) {
        pet().ifPresentOrElse(now -> services.care().particle(viewer, now, particle,
                        de.raindancer.modules.pets.service.SummonService.shape(now.particleShape())),
                () -> services.messages().send(viewer, "pets.no-such-pet", "name", "?"));
        refresh();
    }

    @Override
    protected void decorate() {
        super.decorate();
        ParticleShape shape = pet().map(now -> de.raindancer.modules.pets.service.SummonService.shape(now.particleShape()))
                .orElse(ParticleShape.AURA);
        List<ParticleShape> shapes = CareService.shapes();
        ParticleShape next = shapes.get((shapes.indexOf(shape) + 1) % shapes.size());
        toolbar(4, Icons.of(Material.FIREWORK_STAR, "<white>Shape: " + shape.title(),
                        "<gray>Wings are for players; a pet", "<gray>wears its effect closer.", "",
                        "<yellow>Click<gray> for " + next.title() + "."),
                click -> {
                    pet().ifPresent(now -> services.care().particle(viewer, now, now.particle(), next));
                    refresh();
                });
    }
}
