package de.raindancer.modules.pets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("pet settings, read back safely")
class PetSettingsTest {

    @Test
    @DisplayName("supporters may have at least as many out as everybody")
    void outAtOnce() {
        assertThat(PetSettings.DEFAULTS.outFor(false)).isEqualTo(1);
        assertThat(PetSettings.DEFAULTS.outFor(true)).isEqualTo(2);
        PetSettings odd = new PetSettings(3, 1, 120, true, 24, "", 30, 12, true, 16, false, true);
        assertThat(odd.outFor(true)).isEqualTo(3);
        assertThat(new PetSettings(0, 0, 1, true, 24, "", 30, 12, true, 16, false, true).outFor(false)).isEqualTo(1);
    }

    @Test
    @DisplayName("worlds without pets are matched however they were typed")
    void worlds() {
        PetSettings settings = new PetSettings(1, 2, 120, true, 24, " CTF_Arena , lobby,", 30, 12, true, 16, false, true);
        assertThat(settings.petsAllowedIn("ctf_arena")).isFalse();
        assertThat(settings.petsAllowedIn("Lobby")).isFalse();
        assertThat(settings.petsAllowedIn("world")).isTrue();
        assertThat(PetSettings.DEFAULTS.petsAllowedIn("world")).isTrue();
    }

    @Test
    @DisplayName("a knock-out lasts at least a few seconds")
    void knockOut() {
        assertThat(new PetSettings(1, 2, 0, true, 24, "", 30, 12, true, 16, false, true).knockedOutMillis()).isEqualTo(5000);
    }
}
