package de.raindancer.modules.pets.service;

import de.raindancer.core.ui.messages.Messages;
import de.raindancer.core.world.activity.Activity;
import de.raindancer.modules.pets.PetSettings;
import de.raindancer.modules.pets.model.Species;
import de.raindancer.modules.pets.model.Unlock;
import org.bukkit.entity.Player;

/**
 * Earning a species by playing: whatever players do (Core's activities — farm-guarded once, for everybody) counts
 * towards the species that ask for it, and the moment one opens its owner hears so.
 */
public final class UnlockService implements IPetService {

    private final CollectionService collection;
    private final Messages messages;
    private volatile PetSettings settings;

    public UnlockService(CollectionService collection, Messages messages, PetSettings settings) {
        this.collection = collection;
        this.messages = messages;
        this.settings = settings;
    }

    @Override
    public void settings(PetSettings fresh) {
        settings = fresh;
    }

    public void did(Player player, Activity activity) {
        for (Species species : collection.catalogue().all()) {
            Unlock unlock = species.unlock();
            if (!unlock.counts(activity.kind(), activity.what()) || collection.owns(player.getUniqueId(), species.id())) {
                continue;
            }
            if (collection.addProgress(player.getUniqueId(), species, activity.amount())) {
                messages.send(player, "pets.earned", "species", species.title(),
                        "rarity", species.rarity().title());
            }
        }
    }
}
