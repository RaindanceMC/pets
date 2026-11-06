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

    private final PetServices services;

    public PetSessionListener(PetServices services) {
        this.services = services;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!de.raindancer.modules.pets.service.CareService.SEE_OTHERS.isOn(player)) {
            services.summoning().companions().seeOthers(player, false);
        }
        services.collection().load(player.getUniqueId()).thenAccept(holdings ->
                Scheduling.entityLater(services.plugin(), player, 20, () -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    for (Pet pet : holdings.pets()) {
                        if (pet.out()) {
                            services.summoning().call(player, pet);
                        }
                    }
                }));
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
