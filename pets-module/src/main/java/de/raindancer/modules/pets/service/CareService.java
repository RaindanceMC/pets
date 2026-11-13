package de.raindancer.modules.pets.service;

import de.raindancer.core.ui.choose.NameStyleGrants;
import de.raindancer.core.ui.choose.StyleGrants;
import de.raindancer.core.ui.messages.Messages;
import de.raindancer.core.ui.text.NameStyle;
import de.raindancer.modules.pets.PetSettings;
import de.raindancer.modules.pets.model.Mode;
import de.raindancer.modules.pets.model.OutPet;
import de.raindancer.modules.pets.model.Pet;
import de.raindancer.modules.pets.model.Species;
import de.raindancer.modules.pets.rules.PetNameRule;
import de.raindancer.modules.pets.util.PermissionNodes;
import org.bukkit.entity.Player;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

/** What a player does with their pets — call, send home, stay, follow, mode, name, colours — said the same from a command or a menu. */
public final class CareService implements IPetService {

    private final CollectionService collection;
    private final SummonService summoning;
    private final ExperienceService experience;
    private final PetNameRule names;
    private final Messages messages;
    private final Clock clock;
    private volatile PetSettings settings;

    public CareService(CollectionService collection, SummonService summoning, ExperienceService experience,
                       PetNameRule names, Messages messages, Clock clock, PetSettings settings) {
        this.collection = collection;
        this.summoning = summoning;
        this.experience = experience;
        this.names = names;
        this.messages = messages;
        this.clock = clock;
        this.settings = settings;
    }

    @Override
    public void settings(PetSettings fresh) {
        settings = fresh;
    }

    public void call(Player player, Pet pet) {
        SummonService.Called called = summoning.call(player, pet);
        switch (called) {
            case OUT -> messages.send(player, "pets.called", "name", pet.name());
            case ALREADY_OUT -> messages.send(player, "pets.already-out", "name", pet.name());
            case RESTING -> messages.send(player, "pets.resting", "name", pet.name(),
                    "seconds", Math.max(1, Duration.between(clock.instant(), pet.restingUntil()).toSeconds()));
            case TOO_MANY -> messages.send(player, "pets.too-many",
                    "count", settings.outFor(collection.isSupporter(player)));
            case NOT_HERE -> messages.send(player, "pets.not-here");
            case VANISHED -> messages.send(player, "pets.vanished");
            case UNKNOWN_SPECIES -> messages.send(player, "pets.species-gone", "name", pet.name());
            case FAILED -> messages.send(player, "pets.could-not-call", "name", pet.name());
        }
    }

    public void home(Player player, Pet pet) {
        Optional<OutPet> out = summoning.byPet(pet.id());
        if (out.isEmpty()) {
            messages.send(player, "pets.not-out", "name", pet.name());
            return;
        }
        summoning.dismiss(out.get());
        messages.send(player, "pets.sent-home", "name", pet.name());
    }

    public void stay(Player player, Pet pet, boolean staying) {
        Optional<OutPet> out = summoning.byPet(pet.id());
        if (out.isEmpty()) {
            messages.send(player, "pets.not-out", "name", pet.name());
            return;
        }
        summoning.stay(out.get(), staying);
        messages.send(player, staying ? "pets.staying" : "pets.following", "name", pet.name());
    }

    /** Mutes a pet for everybody, or lets it be heard again. */
    /** Hours between two resets of a pet's skill tree. */
    public static final int SKILL_RESET_HOURS = 24;

    /** Takes the next rank of a node in its tree. @return why not, if not */
    public java.util.Optional<de.raindancer.core.ui.tree.SkillTree.Refusal> learn(Player player, Pet pet, String node) {
        de.raindancer.modules.pets.model.Species species = collection.speciesOf(pet).orElse(null);
        if (species == null) {
            return java.util.Optional.of(de.raindancer.core.ui.tree.SkillTree.Refusal.NEEDS);
        }
        de.raindancer.modules.pets.rules.TreeRule.Learnt learnt = summoning.tree().learn(pet.skills(), node,
                pet.level(), species);
        if (learnt.refusal().isEmpty()) {
            experience.keep(pet.withSkills(learnt.ranks(), pet.skillsReset()));
            de.raindancer.modules.pets.model.Role before = summoning.tree().role(species, pet.skills());
            de.raindancer.modules.pets.model.Role now = summoning.tree().role(species, learnt.ranks());
            if (before != now) {
                messages.send(player, "pets.calling", "name", pet.name(), "role", now.title());
            }
        }
        return learnt.refusal();
    }

    /** Every skill point back, once a day. @return whether it happened */
    public boolean resetSkills(Player player, Pet pet, long now) {
        if (pet.skills().isEmpty() || !de.raindancer.modules.pets.rules.TreeRule.mayReset(pet.skillsReset(), now,
                SKILL_RESET_HOURS)) {
            return false;
        }
        experience.keep(pet.withSkills(java.util.Map.of(), now));
        messages.send(player, "pets.skills-reset", "name", pet.name());
        return true;
    }

    public void mute(Player player, Pet pet) {
        Pet changed = experience.keep(pet.withMuted(!pet.muted()));
        messages.send(player, changed.muted() ? "pets.muted" : "pets.unmuted", "name", pet.name());
    }

    /** Whether this player sees other players' pets — kept with them, on every server. */
    public static final de.raindancer.core.ui.profile.PlayerSwitch SEE_OTHERS =
            new de.raindancer.core.ui.profile.PlayerSwitch("rainspets", "see-others-pets", true);

    public void toggleOthers(Player player) {
        boolean see = SEE_OTHERS.toggle(player);
        summoning.companions().seeOthers(player, see);
        messages.send(player, see ? "pets.others-shown" : "pets.others-hidden");
    }

    /** Whether this player's pets square up to other people's — kept with them, on every server. */
    public static final de.raindancer.core.ui.profile.PlayerSwitch SQUABBLES =
            new de.raindancer.core.ui.profile.PlayerSwitch("rainspets", "squabbles", true);

    public void toggleSquabbles(Player player) {
        boolean on = SQUABBLES.toggle(player);
        summoning.squabbles(player.getUniqueId(), on);
        summoning.of(player.getUniqueId()).forEach(live -> summoning.refresh(live.pet(), player.getName()));
        messages.send(player, on ? "pets.squabbles-on" : "pets.squabbles-off");
    }

    public void mode(Player player, Pet pet, Mode mode) {
        experience.keep(pet.withMode(mode));
        messages.send(player, "pets.mode-set", "name", pet.name(), "mode", mode.title());
    }

    public void tactic(Player player, Pet pet, de.raindancer.modules.pets.model.Tactic tactic) {
        experience.keep(pet.withTactic(tactic));
        messages.send(player, "pets.tactic-set", "name", pet.name(), "tactic", tactic.title());
    }

    public boolean rename(Player player, Pet pet, String typed) {
        Optional<String> name = names.clean(typed);
        if (name.isEmpty()) {
            messages.send(player, "pets.bad-name", "longest", PetNameRule.LONGEST);
            return false;
        }
        experience.keep(pet.withName(name.get()));
        messages.send(player, "pets.renamed", "old", pet.name(), "name", name.get());
        return true;
    }

    /** Typed like {@code #8932B8 #1D1D21 bold animated}; refused as painting one's own name would be. */
    public boolean style(Player player, Pet pet, java.util.List<String> words) {
        de.raindancer.core.ui.text.TypedStyles.Read read = de.raindancer.core.ui.text.TypedStyles.read(words, null);
        if (read.style().isEmpty()) {
            messages.send(player, "pets.bad-style", "detail", read.problem());
            return false;
        }
        if (!mayDecorate(player)) {
            primePerk(player);
            return false;
        }
        if (!styleGrantsOf(player).allows(read.style().get())) {
            messages.send(player, "pets.style-not-yours");
            return false;
        }
        style(pet, read.style().get());
        messages.send(player, "pets.styled", "name", pet.name());
        return true;
    }

    public void style(Pet pet, NameStyle style) {
        experience.keep(pet.withStyle(style.isEmpty() ? "" : style.encode()));
    }

    public void primePerk(Player player) {
        messages.send(player, "pets.prime-perk");
    }

    /** Particles and name colours on a pet are a Prime perk. */
    public boolean mayDecorate(Player player) {
        return player.hasPermission(PermissionNodes.COSMETICS);
    }

    /** The particle effects this player may put on a pet: the ones they may wear themselves. */
    public java.util.List<String> particlesOf(Player player) {
        if (!mayDecorate(player)) {
            return java.util.List.of();
        }
        return de.raindancer.core.ui.choose.ParticleGrants.of(player).orElse(java.util.List.of());
    }

    /** Shapes that fit a small body — wings are the size of a player. */
    public static java.util.List<de.raindancer.core.ui.effect.ParticleShape> shapes() {
        return java.util.Arrays.stream(de.raindancer.core.ui.effect.ParticleShape.values())
                .filter(de.raindancer.core.world.companion.CompanionSpec::fitsSmallBody).toList();
    }

    public boolean particle(Player player, Pet pet, String particle, de.raindancer.core.ui.effect.ParticleShape shape) {
        if (!particle.isEmpty() && !mayDecorate(player)) {
            primePerk(player);
            return false;
        }
        if (!particle.isEmpty() && !particlesOf(player).contains(particle)) {
            messages.send(player, "pets.particle-not-yours");
            return false;
        }
        experience.keep(pet.withParticle(particle, shape.name()));
        messages.send(player, particle.isEmpty() ? "pets.particle-off" : "pets.particle-on", "name", pet.name());
        return true;
    }

    /** What this player may paint their pet's name with: what they may paint their own with. */
    public StyleGrants styleGrantsOf(Player player) {
        if (!mayDecorate(player)) {
            return StyleGrants.NOTHING;
        }
        return NameStyleGrants.of(player).orElse(settings.coloursWithoutCosmetics()
                ? StyleGrants.ALL : StyleGrants.NOTHING);
    }

    public void adopted(Player player, CollectionService.Adoption adoption, Species species) {
        switch (adoption.outcome()) {
            case ADOPTED -> messages.send(player, "pets.adopted", "species", species.title(),
                    "rarity", species.rarity().title());
            case ALREADY_OWNED -> messages.send(player, "pets.already-owned", "species", species.title());
            case LOCKED -> messages.send(player, "pets.locked", "species", species.title());
            case SUPPORTERS_ONLY -> messages.send(player, "pets.supporters-only", "species", species.title());
            case RANK_ONLY -> messages.send(player, "pets.rank-only", "species", species.title());
            case NOT_FOR_SALE -> messages.send(player, "pets.locked", "species", species.title());
            case TOO_POOR -> messages.send(player, "pets.too-poor", "species", species.title());
            case NO_ECONOMY -> messages.send(player, "pets.no-economy");
            case NOT_LOADED -> messages.send(player, "pets.not-loaded");
        }
    }
}
