package de.raindancer.modules.pets.listener;

import de.raindancer.core.platform.util.Scheduling;
import de.raindancer.modules.pets.PetServices;
import de.raindancer.modules.pets.model.Pet;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * A player's pets read when they join — and whatever was out when they left comes back with them — and let go of
 * when they leave. The pets themselves going with their owner is Core's.
 */
public final class PetSessionListener implements IPetListener {

    private static final int RETRIES = 8;
    private static final long RETRY_SECONDS = 15;

    private final PetServices services;

    public PetSessionListener(PetServices services) {
        this.services = services;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        services.summoning().squabbles(player.getUniqueId(),
                de.raindancer.modules.pets.service.CareService.SQUABBLES.isOn(player));
        if (!de.raindancer.modules.pets.service.CareService.SEE_OTHERS.isOn(player)) {
            services.summoning().companions().seeOthers(player, false);
        }
        load(player, 1);
    }

    /** A read that fails is tried again — and told once — rather than taken for "no pets". */
    private void load(Player player, int attempt) {
        services.collection().load(player.getUniqueId()).whenComplete((holdings, failure) -> {
            if (failure != null) {
                retry(player, attempt);
                return;
            }
            Scheduling.entityLater(services.plugin(), player, 20, () -> {
                if (!player.isOnline()) {
                    return;
                }
                for (Pet pet : holdings.pets()) {
                    if (pet.out()) {
                        services.summoning().call(player, pet);
                    }
                }
            });
        });
    }

    private void retry(Player player, int attempt) {
        if (!player.isOnline() || services.collection().loaded(player.getUniqueId())) {
            return;
        }
        if (attempt == 1) {
            Scheduling.entity(services.plugin(), player, () -> services.messages().send(player, "pets.load-failed"));
        }
        if (attempt >= RETRIES) {
            Scheduling.entity(services.plugin(), player, () -> services.messages().send(player, "pets.load-given-up"));
            return;
        }
        Scheduling.asyncLater(services.plugin(), RETRY_SECONDS, java.util.concurrent.TimeUnit.SECONDS,
                () -> load(player, attempt + 1));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        forget(event.getPlayer().getUniqueId());
    }

    @Override
    public void forget(UUID player) {
        services.collection().forget(player);
    }
}
