package de.raindancer.modules.pets.util;

import org.bukkit.Server;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("the nodes this module declares")
class PermissionNodesTest {

    @Test
    @DisplayName("registering makes pet cosmetics a child of rains.prime")
    void primeHoldsCosmetics() {
        Map<String, Permission> known = new HashMap<>();
        PluginManager manager = mock(PluginManager.class);
        when(manager.getPermission(anyString())).thenAnswer(call -> known.get(call.<String>getArgument(0)));
        doAnswer(call -> {
            Permission added = call.getArgument(0);
            known.put(added.getName(), added);
            return null;
        }).when(manager).addPermission(any(Permission.class));
        Server server = mock(Server.class);
        when(server.getPluginManager()).thenReturn(manager);

        when(manager.getPermissionSubscriptions(anyString())).thenReturn(java.util.Set.of());
        try (var bukkit = org.mockito.Mockito.mockStatic(org.bukkit.Bukkit.class)) {
            bukkit.when(org.bukkit.Bukkit::getServer).thenReturn(server);
            PermissionNodes.register(server, List.of());
            PermissionNodes.register(server, List.of());
        }

        assertThat(known.get("rains.prime").getChildren()).containsEntry(PermissionNodes.COSMETICS, true);
        assertThat(known.get(PermissionNodes.COSMETICS)).isNotNull();
    }
}
