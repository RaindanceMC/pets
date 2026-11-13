package de.raindancer.modules.pets.util;

import de.raindancer.modules.pets.model.Species;
import de.raindancer.core.social.support.PrimePermission;
import org.bukkit.Server;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;

import java.util.ArrayList;
import java.util.List;

/**
 * What this module asks about somebody. Registered before anything asks: an unregistered node is "operators only",
 * and an undeclared {@code pets.species.<id>} would hand every operator every pet.
 */
public final class PermissionNodes {

    public static final String USE = "pets.use";
    public static final String ADMIN = "pets.admin";
    /** Particles and name colours on a pet: a Prime perk, so {@code rains.prime} holds it. */
    public static final String COSMETICS = "rainspets.cosmetics";

    /** Cosmetics' node for staff (seeded into every staff group); looked at, not declared here. */
    public static final String STAFF_PERKS = "rainscosmetics.perks";

    private PermissionNodes() {
    }

    public static List<Permission> declared(List<Species> species) {
        List<Permission> nodes = new ArrayList<>();
        nodes.add(new Permission(USE, "Have pets, call them, ride them", PermissionDefault.TRUE));
        nodes.add(new Permission(ADMIN, "Give and take pets, set their personality", PermissionDefault.OP));
        nodes.add(new Permission(COSMETICS, "Put particles and name colours on a pet — a Prime perk",
                PermissionDefault.FALSE));
        for (Species each : species) {
            nodes.add(new Permission(each.permission(), "Have the " + each.title() + " whatever its way",
                    PermissionDefault.FALSE));
        }
        return nodes;
    }

    /** @return how many were added */
    public static int register(Server server, List<Species> species) {
        int added = 0;
        for (Permission permission : declared(species)) {
            if (server.getPluginManager().getPermission(permission.getName()) != null) {
                continue;
            }
            try {
                server.getPluginManager().addPermission(permission);
                added++;
            } catch (IllegalArgumentException alreadyThere) {
                // registered by another copy in between
            }
        }
        primeHoldsCosmetics(server.getPluginManager());
        return added;
    }

    /** Idempotent: a perk is Prime's when its node is a child of {@code rains.prime}. */
    static void primeHoldsCosmetics(PluginManager manager) {
        Permission prime = PrimePermission.permission(manager);
        prime.getChildren().put(COSMETICS, true);
        prime.recalculatePermissibles();
    }
}
