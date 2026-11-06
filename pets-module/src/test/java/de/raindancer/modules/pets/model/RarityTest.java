package de.raindancer.modules.pets.model;

import de.raindancer.core.ui.text.NameStyle;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("the name a pet wears before its owner paints it")
class RarityTest {

    @Test
    @DisplayName("every rarity above common has a gradient of the cosmetics presets; common stays plain")
    void gradients() {
        assertThat(Rarity.COMMON.nameStyle()).isEqualTo(NameStyle.NONE);
        for (Rarity rarity : Rarity.values()) {
            if (rarity != Rarity.COMMON) {
                assertThat(rarity.nameStyle().colours()).as(rarity.title()).hasSizeGreaterThanOrEqualTo(2);
            }
        }
        // the presets' own colours: Gold for legendary, Royal for mythic — both bold
        assertThat(Rarity.LEGENDARY.nameStyle().encodeColours()).isEqualToIgnoringCase("#f7971e,#ffd200");
        assertThat(Rarity.MYTHIC.nameStyle().encodeColours()).isEqualToIgnoringCase("#8e2de2,#4a00e0");
        assertThat(Rarity.LEGENDARY.nameStyle().decorations()).contains(TextDecoration.BOLD);
        assertThat(Rarity.MYTHIC.nameStyle().decorations()).contains(TextDecoration.BOLD);
        assertThat(Rarity.values()).extracting(rarity -> rarity.nameStyle().encode()).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("its owner's own paint wins over the rarity's")
    void ownPaintWins() {
        assertThat(Rarity.MYTHIC.styleFor("")).isEqualTo(Rarity.MYTHIC.nameStyle());
        assertThat(Rarity.MYTHIC.styleFor("#ff0000").encode()).isEqualToIgnoringCase("#ff0000");
    }
}
