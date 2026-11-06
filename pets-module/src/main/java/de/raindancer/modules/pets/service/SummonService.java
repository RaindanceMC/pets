package de.raindancer.modules.pets.service;

import de.raindancer.core.ui.text.Gradients;
import de.raindancer.core.ui.text.NameStyle;
import de.raindancer.core.world.companion.Companion;
import de.raindancer.core.world.companion.CompanionHooks;
import de.raindancer.core.world.companion.CompanionService;
import de.raindancer.core.world.companion.CompanionSpec;
import de.raindancer.core.world.companion.Fighting;
import de.raindancer.modules.pets.PetSettings;
import de.raindancer.modules.pets.model.Levels;
import de.raindancer.modules.pets.model.OutPet;
import de.raindancer.modules.pets.model.Pet;
import de.raindancer.modules.pets.model.Species;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Pets out in the world, as Core's companions: what each one is made as (its species, level and nature), calling it,
 * sending it home, knocking it out. Core drives and protects the mob; nothing here touches one.
 */
public final class SummonService implements IPetService {

    public enum Called {
        OUT, ALREADY_OUT, RESTING, TOO_MANY, NOT_HERE, VANISHED, UNKNOWN_SPECIES, FAILED
    }

    private final Plugin plugin;
    private final CompanionService companions;
    private final CollectionService collection;
    private final Clock clock;
    private final Map<UUID, OutPet> byPet = new ConcurrentHashMap<>();
    private final Map<UUID, OutPet> byCompanion = new ConcurrentHashMap<>();
    private volatile PetSettings settings;
    private volatile Levels levels;
    private final de.raindancer.modules.pets.rules.TreeRule tree = new de.raindancer.modules.pets.rules.TreeRule(
            de.raindancer.modules.pets.rules.TreeRule.shipped());
    /** Owners who turned squabbling off for their own pets. */
    private final java.util.Set<UUID> calm = ConcurrentHashMap.newKeySet();
    private volatile Function<OutPet, CompanionHooks> hooks = out -> new CompanionHooks() { };

    public SummonService(Plugin plugin, CompanionService companions, CollectionService collection, Clock clock,
                         PetSettings settings) {
        this.plugin = plugin;
        this.companions = companions;
        this.collection = collection;
        this.clock = clock;
        settings(settings);
    }

    @Override
    public void settings(PetSettings fresh) {
        settings = fresh;
        levels = new Levels(fresh.highestLevel());
    }

    /** What every pet tells — {@link PetHooks}, handed in once it exists. */
    public void tellThrough(Function<OutPet, CompanionHooks> hooks) {
        this.hooks = hooks;
    }

    public Levels levels() {
        return levels;
    }

    public PetSettings config() {
        return settings;
    }

    public CompanionService companions() {
        return companions;
    }

    public Plugin plugin() {
        return plugin;
    }

    /** Whether this owner's pets may square up to other people's — their own switch. */
    public void squabbles(UUID owner, boolean on) {
        if (on) {
            calm.remove(owner);
        } else {
            calm.add(owner);
        }
    }

    // ------------------------------------------------------------------ asking

    public List<OutPet> of(UUID owner) {
        return byPet.values().stream().filter(out -> out.pet().owner().equals(owner)).toList();
    }

    public Optional<OutPet> byPet(UUID pet) {
        return Optional.ofNullable(byPet.get(pet));
    }

    public Optional<OutPet> byCompanion(Companion companion) {
        return Optional.ofNullable(byCompanion.get(companion.id()));
    }

    public boolean isOut(UUID pet) {
        return byPet.containsKey(pet);
    }

    // ------------------------------------------------------------------ making

    /** What Core makes for this pet: its species, grown to its level, bent by its nature and its skill tree. */
    public CompanionSpec spec(Pet pet, Species species, String ownerName) {
        de.raindancer.modules.pets.rules.TreeRule.Boosts skill = tree.boosts(pet.skills());
        de.raindancer.modules.pets.model.Role role = tree.role(species, pet.skills());
        Fighting fighting = role.fighting();
        double speedFactor = pet.temperament().speedFactor();
        double strength = fighting == Fighting.HEALER
                ? pet.temperament().healFactor() * skill.factor("heal")
                : pet.temperament().damageFactor() * skill.factor("damage");
        EntityType missile;
        try {
            missile = EntityType.valueOf(species.missile());
        } catch (IllegalArgumentException unknown) {
            missile = EntityType.SNOWBALL;
        }
        double health = levels.health(species.health(), pet.level()) * pet.temperament().healthFactor()
                * skill.factor("health");
        double scouting = role == de.raindancer.modules.pets.model.Role.SCOUT ? SCOUT_SPEED : 1;
        return CompanionSpec.of(EntityType.valueOf(species.entityType()))
                .height(species.size())
                .flies(species.flies())
                .baby(species.baby())
                .fighting(fighting)
                .stance(pet.mode().stance())
                .health(health)
                .damage(levels.damage(species.damage(), pet.level()) * strength)
                .speed(species.speed() * speedFactor * skill.factor("speed") * scouting)
                .fleesBelow(pet.temperament().fleesBelow())
                .missile(missile)
                .rideable(settings.riding())
                .attackTicks((int) Math.round((fighting == Fighting.RANGED ? 30 : 20) / speedFactor
                        / skill.factor("attack_speed")))
                .name(name(pet, species.rarity(), settings.ownerInName() ? ownerName : ""))
                .particle(pet.particle(), shape(pet.particleShape()))
                .wander(pet.temperament().wanderRadius(), pet.temperament().wanderEagerness())
                .silent(pet.muted())
                .ability(species.ability())
                .abilityPower(skill.factor("ability_power"))
                .abilityHaste(skill.factor("ability_haste"))
                .tactic(pet.tactic().core())
                .grumpiness(settings.squabbles() && !calm.contains(pet.owner()) ? pet.temperament().grumpiness() : 0)
                .regenPerSecond(levels.regenPerSecond(health, pet.level()) * skill.factor("regen"))
                .lifesteal(skill.share("lifesteal"))
                .toughness(skill.share("toughness"))
                .thorns(skill.share("thorns"))
                .collectRadius((role == de.raindancer.modules.pets.model.Role.GATHERER ? GATHER_REACH : 0)
                        + skill.plain("collect"))
                .spotRadius((role == de.raindancer.modules.pets.model.Role.SCOUT ? SCOUT_REACH : 0) + skill.plain("spot"))
                .auras(auras(role, skill));
    }

    /** A gatherer's reach, a scout's sight and speed, before any skill. */
    static final double GATHER_REACH = 5;
    static final double SCOUT_REACH = 16;
    static final double SCOUT_SPEED = 1.1;

    /** The effects a pet keeps on its owner: a support's Speed I to begin with, every aura_ node a level more. */
    static java.util.Map<String, Integer> auras(de.raindancer.modules.pets.model.Role role,
                                                de.raindancer.modules.pets.rules.TreeRule.Boosts skill) {
        java.util.Map<String, Integer> levels = new java.util.TreeMap<>();
        if (role == de.raindancer.modules.pets.model.Role.SUPPORT) {
            levels.put("speed", 1);
        }
        skill.sums().forEach((effect, value) -> {
            if (effect.startsWith("aura_") && value > 0) {
                levels.merge(effect.substring(5), (int) Math.round(value), Integer::sum);
            }
        });
        java.util.Map<String, Integer> amplifiers = new java.util.TreeMap<>();
        levels.forEach((effect, level) -> amplifiers.put(effect, Math.min(3, level) - 1));
        return amplifiers;
    }

    public de.raindancer.modules.pets.rules.TreeRule tree() {
        return tree;
    }

    public static de.raindancer.core.ui.effect.ParticleShape shape(String name) {
        try {
            return de.raindancer.core.ui.effect.ParticleShape.valueOf(name);
        } catch (IllegalArgumentException | NullPointerException unknown) {
            return de.raindancer.core.ui.effect.ParticleShape.AURA;
        }
    }

    /** Its name, painted as its owner chose — or in its rarity's gradient — and whose it is. */
    public static Component name(Pet pet, de.raindancer.modules.pets.model.Rarity rarity, String ownerName) {
        Component name = Gradients.styled(pet.name(), rarity.styleFor(pet.style()));
        return ownerName == null || ownerName.isEmpty() ? name
                : name.append(Component.text(" (" + ownerName + ")", NamedTextColor.GRAY));
    }

    // ------------------------------------------------------------------ calling

    /** Calls a pet out beside its owner. On the owner's thread. */
    public Called call(Player owner, Pet pet) {
        if (isOut(pet.id())) {
            return Called.ALREADY_OUT;
        }
        Species species = collection.speciesOf(pet).orElse(null);
        if (species == null) {
            return Called.UNKNOWN_SPECIES;
        }
        if (pet.restingAt(clock.instant())) {
            return Called.RESTING;
        }
        if (!settings.petsAllowedIn(owner.getWorld().getName())) {
            return Called.NOT_HERE;
        }
        if (of(owner.getUniqueId()).size() >= settings.outFor(collection.isSupporter(owner))) {
            return Called.TOO_MANY;
        }
        OutPet[] made = new OutPet[1];
        CompanionHooks relay = new Relay(made);
        CompanionService.Summoned summoned = companions.summon(plugin, owner, spec(pet, species, owner.getName()),
                relay);
        if (summoned.get().isEmpty()) {
            return summoned.refusal() == CompanionService.Refusal.VANISHED ? Called.VANISHED : Called.FAILED;
        }
        Pet out = collection.update(pet.withOut(true));
        OutPet live = new OutPet(out, species, summoned.companion());
        made[0] = live;
        byPet.put(out.id(), live);
        byCompanion.put(summoned.companion().id(), live);
        return Called.OUT;
    }

    /** Hands Core's news on to {@link PetHooks}, once the pet is known. */
    private final class Relay implements CompanionHooks {

        private final OutPet[] out;

        Relay(OutPet[] out) {
            this.out = out;
        }

        private Optional<CompanionHooks> to() {
            return Optional.ofNullable(out[0]).map(hooks);
        }

        @Override
        public void killed(Companion companion, org.bukkit.entity.LivingEntity victim, boolean boss, boolean farmed) {
            to().ifPresent(next -> next.killed(companion, victim, boss, farmed));
        }

        @Override
        public void gone(Companion companion, Gone why) {
            OutPet live = out[0];
            if (live != null) {
                went(live, why);
            }
            to().ifPresent(next -> next.gone(companion, why));
        }

        @Override
        public void ridden(Companion companion, double blocks) {
            to().ifPresent(next -> next.ridden(companion, blocks));
        }

        @Override
        public void seen(Companion companion, Player viewer) {
            to().ifPresent(next -> next.seen(companion, viewer));
        }

        @Override
        public boolean fed(Companion companion, Player owner, org.bukkit.inventory.ItemStack food) {
            return to().map(next -> next.fed(companion, owner, food)).orElse(false);
        }

        @Override
        public boolean clicked(Companion companion, Player owner, boolean sneaking) {
            return to().map(next -> next.clicked(companion, owner, sneaking)).orElse(false);
        }

        @Override
        public boolean allowedIn(Companion companion, String world) {
            return settings.petsAllowedIn(world);
        }
    }

    /** What its leaving means for the pet: out still, home, or resting. */
    private void went(OutPet live, CompanionHooks.Gone why) {
        if (why == CompanionHooks.Gone.OWNER_VANISHED) {
            return;
        }
        byPet.remove(live.pet().id());
        byCompanion.remove(live.companion().id());
        Pet now = collection.pet(live.pet().owner(), live.pet().id()).orElse(live.pet());
        switch (why) {
            case DISMISSED, NOT_ALLOWED_HERE -> collection.update(now.withOut(false));
            case BEATEN -> collection.update(now.withOut(false)
                    .withRestingUntil(clock.instant().plusMillis(settings.knockedOutMillis())));
            default -> {
                // its owner left, or the server stopped: it comes back with them
            }
        }
    }

    /** A changed pet — a level, a name, a nature — put on the companion that is out. */
    public void refresh(Pet pet, String ownerName) {
        OutPet live = byPet.get(pet.id());
        if (live == null) {
            return;
        }
        live.pet(pet);
        companions.update(live.companion(), spec(pet, live.species(), ownerName));
    }

    public void heal(OutPet live, double amount) {
        companions.heal(live.companion(), amount);
    }

    // ------------------------------------------------------------------ sending home

    public void dismiss(OutPet live) {
        companions.dismiss(live.companion());
    }

    public void dismissAll(UUID owner) {
        of(owner).forEach(this::dismiss);
    }

    public void stay(OutPet live, boolean on) {
        live.companion().stay(on);
    }

    /** When a knocked-out pet may be called again. */
    public Instant restingUntil(Pet pet) {
        return pet.restingUntil();
    }
}
