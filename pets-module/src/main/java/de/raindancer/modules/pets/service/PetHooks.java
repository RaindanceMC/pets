package de.raindancer.modules.pets.service;

import de.raindancer.core.ui.messages.Messages;
import de.raindancer.core.world.companion.Companion;
import de.raindancer.core.world.companion.CompanionHooks;
import de.raindancer.modules.pets.PetSettings;
import de.raindancer.modules.pets.model.OutPet;
import de.raindancer.modules.pets.model.Rarity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Clock;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

/** What a pet hears from Core about its companion, turned into growth, words and discoveries. */
public final class PetHooks implements IPetService {

    private final CollectionService collection;
    private final ExperienceService experience;
    private final Messages messages;
    private final Function<UUID, Player> online;
    private final Consumer<Player> openMenu;
    private final Clock clock;
    private volatile PetSettings settings;

    public PetHooks(CollectionService collection, ExperienceService experience, Messages messages,
                    Function<UUID, Player> online, Consumer<Player> openMenu, Clock clock, PetSettings settings) {
        this.collection = collection;
        this.experience = experience;
        this.messages = messages;
        this.online = online;
        this.openMenu = openMenu;
        this.clock = clock;
        this.settings = settings;
    }

    @Override
    public void settings(PetSettings fresh) {
        settings = fresh;
    }

    public CompanionHooks of(OutPet live) {
        return new CompanionHooks() {

            @Override
            public void killed(Companion companion, LivingEntity victim, boolean boss, boolean farmed) {
                experience.killed(live, boss, farmed);
            }

            @Override
            public void gone(Companion companion, Gone why) {
                Player owner = online.apply(live.pet().owner());
                if (owner == null) {
                    return;
                }
                switch (why) {
                    case BEATEN -> messages.send(owner, "pets.knocked-out", "name", live.pet().name(),
                            "seconds", settings.knockedOutMillis() / 1000);
                    case NOT_ALLOWED_HERE -> messages.send(owner, "pets.not-here-sent-home", "name", live.pet().name());
                    default -> {
                    }
                }
            }

            @Override
            public void ridden(Companion companion, double blocks) {
                experience.ridden(live, blocks);
            }

            @Override
            public void seen(Companion companion, Player viewer) {
                if (live.species().rarity() != Rarity.COMMON && collection.markSeen(viewer.getUniqueId(),
                        live.species().id())) {
                    messages.send(viewer, "pets.discovered", "species", live.species().title(),
                            "rarity", live.species().rarity().title());
                }
            }

            @Override
            public boolean fed(Companion companion, Player owner, ItemStack food) {
                return experience.fed(live, owner, clock.millis());
            }

            @Override
            public void inspected(Companion companion, Player viewer) {
                Player owner = online.apply(live.pet().owner());
                messages.send(viewer, "pets.whose", "name", live.pet().name(), "species", live.species().title(),
                        "owner", owner == null ? "?" : owner.getName());
            }

            @Override
            public boolean clicked(Companion companion, Player owner, boolean sneaking) {
                if (sneaking) {
                    openMenu.accept(owner);
                    return true;
                }
                return false;
            }
        };
    }
}
