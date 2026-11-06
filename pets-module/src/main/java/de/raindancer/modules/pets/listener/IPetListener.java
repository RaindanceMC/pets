package de.raindancer.modules.pets.listener;

import org.bukkit.event.Listener;

import java.util.UUID;

/** A listener of this module: holds {@code PetServices}, and forgets a player who left. */
public interface IPetListener extends Listener {

    void forget(UUID player);
}
