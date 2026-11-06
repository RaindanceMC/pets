package de.raindancer.modules.pets;

import de.raindancer.modules.api.ModuleCommand;
import de.raindancer.modules.pets.command.PetCommand;
import de.raindancer.modules.pets.util.PermissionNodes;

import java.util.List;

/** What this module declares at bootstrap — before the module exists — and how it is filled in later. */
public final class PetCommands {

    private static volatile PetServices services;

    private PetCommands() {
    }

    public static List<ModuleCommand> declared() {
        return List.of(ModuleCommand.of("pets", "Your pets: call them, send them home, name them, ride them",
                        new PetCommand(PetCommands::require))
                .aliased("pet")
                .taking("with nothing — your pets", "call|home|stay|follow|mode|rename|colours|particle <pet>",
                        "species — every species and how to get it",
                        "admin give|take|personality|list <player> …")
                .needing(PermissionNodes.USE));
    }

    static void ready(PetServices live) {
        services = live;
    }

    static void stopped() {
        services = null;
    }

    private static PetServices require() {
        PetServices live = services;
        if (live == null) {
            throw new IllegalStateException("the pets module is not running");
        }
        return live;
    }
}
