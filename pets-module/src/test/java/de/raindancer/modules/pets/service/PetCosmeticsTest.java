package de.raindancer.modules.pets.service;

import de.raindancer.core.ui.choose.NameStyleGrants;
import de.raindancer.core.ui.choose.ParticleGrants;
import de.raindancer.core.ui.choose.StyleGrants;
import de.raindancer.core.ui.effect.ParticleShape;
import de.raindancer.core.ui.messages.Messages;
import de.raindancer.modules.pets.PetSettings;
import de.raindancer.modules.pets.model.Personality;
import de.raindancer.modules.pets.model.Pet;
import de.raindancer.modules.pets.model.Temperament;
import de.raindancer.modules.pets.rules.PetNameRule;
import de.raindancer.modules.pets.util.PermissionNodes;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("pet cosmetics are a Prime perk")
class PetCosmeticsTest {

    private static final UUID ALICE = UUID.nameUUIDFromBytes("alice".getBytes());

    private Messages messages;
    private ExperienceService experience;
    private CareService care;
    private Object particleHandle;
    private Object styleHandle;
    private Pet rex;

    @BeforeEach
    void setUp() {
        messages = mock(Messages.class);
        experience = mock(ExperienceService.class);
        care = new CareService(mock(CollectionService.class), mock(SummonService.class), experience, new PetNameRule(),
                messages, Clock.systemUTC(), PetSettings.DEFAULTS);
        particleHandle = ParticleGrants.provide(player -> List.of("FLAME", "HEART"));
        styleHandle = NameStyleGrants.provide(player -> StyleGrants.ALL);
        rex = Pet.adopt(ALICE, "wolf", "Rex", Temperament.of(Personality.CHEEKY), Instant.ofEpochSecond(1_000));
    }

    @AfterEach
    void tearDown() {
        ParticleGrants.retract(particleHandle);
        NameStyleGrants.retract(styleHandle);
    }

    private static Player player(String... nodes) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(ALICE);
        for (String node : nodes) {
            when(player.hasPermission(node)).thenReturn(true);
        }
        return player;
    }

    @Test
    @DisplayName("the node is rainspets.cosmetics, and Prime holds it")
    void node() {
        assertThat(PermissionNodes.COSMETICS).isEqualTo("rainspets.cosmetics");
        var declared = PermissionNodes.declared(List.of()).stream()
                .filter(each -> each.getName().equals(PermissionNodes.COSMETICS)).findFirst().orElseThrow();
        assertThat(declared.getDefault()).isEqualTo(org.bukkit.permissions.PermissionDefault.FALSE);
    }

    @Test
    @DisplayName("without Prime there are no particles to pick, even for somebody who may wear them themselves")
    void noParticlesToPick() {
        assertThat(care.particlesOf(player())).isEmpty();
        assertThat(care.particlesOf(player(PermissionNodes.COSMETICS))).containsExactly("FLAME", "HEART");
    }

    @Test
    @DisplayName("without Prime nothing may be painted on a name")
    void noPaint() {
        assertThat(care.styleGrantsOf(player())).isSameAs(StyleGrants.NOTHING);
        assertThat(care.styleGrantsOf(player(PermissionNodes.COSMETICS))).isSameAs(StyleGrants.ALL);
    }

    @Test
    @DisplayName("a particle is refused without Prime, with the Prime-perk message, and the pet is left as it is")
    void particleRefused() {
        Player player = player();
        assertThat(care.particle(player, rex, "FLAME", ParticleShape.values()[0])).isFalse();
        verify(messages).send(player, "pets.prime-perk");
        verifyNoInteractions(experience);
    }

    @Test
    @DisplayName("taking a particle off needs no Prime: it is never refused")
    void takingOffIsFree() {
        Player player = player();
        when(experience.keep(any())).thenReturn(rex);
        assertThat(care.particle(player, rex, "", ParticleShape.values()[0])).isTrue();
    }

    @Test
    @DisplayName("a typed style is refused without Prime, with the Prime-perk message")
    void styleRefused() {
        Player player = player();
        assertThat(care.style(player, rex, List.of("#8932B8", "bold"))).isFalse();
        verify(messages).send(player, "pets.prime-perk");
        verify(experience, never()).keep(any());
    }

    @Test
    @DisplayName("with Prime a particle and a style go on the pet")
    void primeMay() {
        Player player = player(PermissionNodes.COSMETICS);
        when(experience.keep(any())).thenReturn(rex);
        assertThat(care.particle(player, rex, "FLAME", ParticleShape.values()[0])).isTrue();
        assertThat(care.style(player, rex, List.of("#8932B8", "bold"))).isTrue();
    }

    @Test
    @DisplayName("staff with Cosmetics' perks node may decorate too, as with the other cosmetics")
    void staffMay() {
        assertThat(care.mayDecorate(player("rainscosmetics.perks"))).isTrue();
        assertThat(care.mayDecorate(player())).isFalse();
    }
}
