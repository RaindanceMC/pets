package de.raindancer.modules.pets.service;

import de.raindancer.modules.pets.model.Personality;
import de.raindancer.modules.pets.model.Pet;
import de.raindancer.modules.pets.model.Rarity;
import de.raindancer.modules.pets.model.Temperament;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("the name a pet wears over its head")
class PetNameTagTest {

    private static final Pet REX = Pet.adopt(UUID.randomUUID(), "wolf", "Rex", Temperament.of(Personality.CHEEKY),
            Instant.EPOCH).withProgress(7, 0);

    private static String plain(net.kyori.adventure.text.Component name) {
        return PlainTextComponentSerializer.plainText().serialize(name);
    }

    @Test
    @DisplayName("its name and its level")
    void level() {
        assertThat(plain(SummonService.name(REX, Rarity.COMMON, ""))).isEqualTo("Rex Lv 7");
        assertThat(plain(SummonService.name(REX.withProgress(1, 0), Rarity.COMMON, null))).isEqualTo("Rex Lv 1");
    }

    @Test
    @DisplayName("and whose it is behind, where the server shows that")
    void owner() {
        assertThat(plain(SummonService.name(REX, Rarity.COMMON, "Tom"))).isEqualTo("Rex Lv 7 (Tom)");
    }
}
