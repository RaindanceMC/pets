package de.raindancer.modules.pets;

import de.raindancer.core.ui.preference.Preference;
import de.raindancer.modules.pets.service.CareService;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("pets on /preferences")
class PetPreferencesTest {

    private final CareService care = mock(CareService.class);
    private final Player player = mock(Player.class);
    private final PersistentDataContainer data = mock(PersistentDataContainer.class);

    PetPreferencesTest() {
        when(player.getPersistentDataContainer()).thenReturn(data);
    }

    private void stored(boolean on) {
        when(data.get(any(NamespacedKey.class), eq(PersistentDataType.BOOLEAN))).thenReturn(on);
    }

    @Test
    @DisplayName("both are switches of group Pets with their own ids")
    void built() {
        List<Preference> all = PetPreferences.build(care, () -> PetSettings.DEFAULTS);
        assertThat(all).extracting(Preference::id).containsExactly("pets:see-others", "pets:squabbles");
        assertThat(all).extracting(Preference::group).containsOnly("Pets");
        assertThat(all).allSatisfy(each -> assertThat(each.options()).containsExactly("On", "Off"));
    }

    @Test
    @DisplayName("seeing others reads the player's switch and sets it through the care service")
    void seeOthers() {
        Preference see = PetPreferences.build(care, () -> PetSettings.DEFAULTS).get(0);
        stored(false);
        assertThat(see.state(player)).isEqualTo("Off");
        stored(true);
        assertThat(see.state(player)).isEqualTo("On");
        see.choose().accept(player, 1);
        verify(care).setOthers(player, false);
        see.choose().accept(player, 0);
        verify(care).setOthers(player, true);
    }

    @Test
    @DisplayName("squabbles go through the care service too")
    void squabbles() {
        Preference squabbles = PetPreferences.build(care, () -> PetSettings.DEFAULTS).get(1);
        squabbles.choose().accept(player, 1);
        verify(care).setSquabbles(player, false);
        stored(true);
        assertThat(squabbles.state(player)).isEqualTo("On");
    }

    @Test
    @DisplayName("squabbles are not offered while the server has them off")
    void offeredOnlyWhenOn() {
        PetSettings off = new PetSettings(1, 2, 120, true, 24, "", 30, 12, true, 16, false, false);
        assertThat(PetPreferences.build(care, () -> off).get(1).shown().test(player)).isFalse();
        assertThat(PetPreferences.build(care, () -> PetSettings.DEFAULTS).get(1).shown().test(player)).isTrue();
        assertThat(PetPreferences.build(care, () -> off).get(0).shown().test(player)).isTrue();
    }
}
