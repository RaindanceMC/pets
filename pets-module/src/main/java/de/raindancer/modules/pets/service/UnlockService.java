package de.raindancer.modules.pets.service;

import de.raindancer.core.ui.messages.Messages;
import de.raindancer.core.world.activity.Activity;
import de.raindancer.modules.pets.PetSettings;
import de.raindancer.modules.pets.model.Species;
import de.raindancer.modules.pets.model.Unlock;
import org.bukkit.entity.Player;

/**
 * Earning a species by playing: whatever players do (Core's activities — farm-guarded once, for everybody) counts
 * towards the species that ask for it, and the moment one opens its owner hears so. A dropped species is rolled for
 * on every kill of its mob; a legendary or mythic one that falls is told to the whole server.
 */
public final class UnlockService implements IPetService {

    private final CollectionService collection;
    private final Messages messages;
    private final java.util.function.Supplier<? extends java.util.Collection<? extends Player>> online;
    private final java.util.function.DoubleSupplier roll;
    private volatile PetSettings settings;

    public UnlockService(CollectionService collection, Messages messages, PetSettings settings,
                         java.util.function.Supplier<? extends java.util.Collection<? extends Player>> online,
                         java.util.function.DoubleSupplier roll) {
        this.collection = collection;
        this.messages = messages;
        this.settings = settings;
        this.online = online;
        this.roll = roll;
    }

    @Override
    public void settings(PetSettings fresh) {
        settings = fresh;
    }

    public void did(Player player, Activity activity) {
        for (Species species : collection.catalogue().all()) {
            Unlock unlock = species.unlock();
            java.util.List<Integer> parts = unlock.matching(activity.kind(), activity.what());
            if (parts.isEmpty() || collection.owns(player.getUniqueId(), species.id())) {
                continue;
            }
            if (unlock.way() == Unlock.Way.DROP) {
                dropped(player, species, activity);
                continue;
            }
            boolean opened = false;
            for (int part : parts) {
                opened |= collection.addProgress(player, species, part, activity.amount());
            }
            if (opened) {
                messages.send(player, "pets.earned", "species", species.title(),
                        "rarity", species.rarity().title());
            }
        }
    }

    private void dropped(Player player, Species species, Activity activity) {
        if (collection.verdict(player, species).state() != de.raindancer.modules.pets.rules.UnlockRule.State.LOCKED) {
            return;
        }
        boolean fell = false;
        for (int kill = 0; kill < Math.max(1, activity.amount()) && !fell; kill++) {
            fell = species.unlock().drops(roll.getAsDouble());
        }
        if (!fell || !collection.addProgress(player, species, 0, 1)) {
            return;
        }
        messages.send(player, "pets.dropped", "species", species.title(), "rarity", species.rarity().title());
        if (species.rarity().ordinal() >= de.raindancer.modules.pets.model.Rarity.LEGENDARY.ordinal()) {
            for (Player other : online.get()) {
                if (!other.equals(player)) {
                    messages.send(other, "pets.dropped-for", "player", player.getName(),
                            "species", species.title(), "rarity", species.rarity().title());
                }
            }
        }
    }
}
