package de.raindancer.modules.pets.util;

import de.raindancer.modules.pets.model.Species;
import org.bukkit.Server;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import java.util.ArrayList;
import java.util.List;

/**
 * What this module asks about somebody. Registered before anything asks: an unregistered node is "operators only",
 * and an undeclared {@code pets.species.<id>} would hand every operator every pet.
 */
public final class PermissionNodes {

    public static final String USE = "pets.use";
    public static final String ADMIN = "pets.admin";

    private PermissionNodes() {
    }

    public static List<Permission> declared(List<Species> species) {
        List<Permission> nodes = new ArrayList<>();
        nodes.add(new Permission(USE, "Have pets, call them, ride them", PermissionDefault.TRUE));
        nodes.add(new Permission(ADMIN, "Give and take pets, set their personality", PermissionDefault.OP));
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
        return added;
    }
}
