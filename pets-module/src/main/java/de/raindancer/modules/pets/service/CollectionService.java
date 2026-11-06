package de.raindancer.modules.pets.service;

import de.raindancer.core.social.economy.EconomyResult;
import de.raindancer.core.social.economy.Fees;
import de.raindancer.core.social.support.Supporters;
import de.raindancer.modules.pets.PetSettings;
import de.raindancer.modules.pets.model.Pet;
import de.raindancer.modules.pets.model.Species;
import de.raindancer.modules.pets.model.Temperament;
import de.raindancer.modules.pets.rules.DiscoveryRule;
import de.raindancer.modules.pets.rules.UnlockRule;
import de.raindancer.modules.pets.store.PetStore;
import de.raindancer.modules.pets.store.SpeciesCatalogue;
import org.bukkit.entity.Player;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Whose pets are whose: held in memory for everybody online, read when they join and written behind every change.
 * Taking, buying, granting and taking away go through here.
 */
public final class CollectionService implements IPetService {

    /** What one player has. */
    public record Holdings(List<Pet> pets, Map<String, Long> progress, Set<String> seen) {

        public Holdings {
            pets = List.copyOf(pets);
            progress = Map.copyOf(progress);
            seen = Set.copyOf(seen);
        }

        static final Holdings NONE = new Holdings(List.of(), Map.of(), Set.of());
    }

    /** How a take or a purchase went. */
    public enum Outcome {
        ADOPTED, ALREADY_OWNED, LOCKED, SUPPORTERS_ONLY, RANK_ONLY, NOT_FOR_SALE, TOO_POOR, NO_ECONOMY, NOT_LOADED
    }

    public record Adoption(Outcome outcome, Pet pet) {
    }

    public static final String SOURCE = "pets";

    private final PetStore store;
    private final SpeciesCatalogue catalogue;
    private final UnlockRule unlocks;
    private final DiscoveryRule discovery;
    private final Supporters supporters;
    private final Consumer<Runnable> async;
    private final Clock clock;
    private final Random random = new Random();
    private final Map<UUID, Holdings> held = new ConcurrentHashMap<>();
    private volatile PetSettings settings;

    public CollectionService(PetStore store, SpeciesCatalogue catalogue, UnlockRule unlocks, DiscoveryRule discovery,
                             Supporters supporters, Consumer<Runnable> async, Clock clock, PetSettings settings) {
        this.store = store;
        this.catalogue = catalogue;
        this.unlocks = unlocks;
        this.discovery = discovery;
        this.supporters = supporters;
        this.async = async;
        this.clock = clock;
        this.settings = settings;
    }

    @Override
    public void settings(PetSettings fresh) {
        settings = fresh;
    }

    // ------------------------------------------------------------------ holding

    /** Reads a player's pets off the main thread; the future completes there too. */
    public CompletableFuture<Holdings> load(UUID owner) {
        CompletableFuture<Holdings> done = new CompletableFuture<>();
        async.accept(() -> {
            try {
                Holdings read = new Holdings(store.of(owner), store.progress(owner), store.seen(owner));
                held.put(owner, read);
                done.complete(read);
            } catch (RuntimeException failed) {
                done.completeExceptionally(failed);
            }
        });
        return done;
    }

    /** What somebody has, held or read — for an admin looking at a player who is not online. */
    public CompletableFuture<List<Pet>> petsOf(UUID owner) {
        Holdings holdings = held.get(owner);
        if (holdings != null) {
            return CompletableFuture.completedFuture(holdings.pets());
        }
        CompletableFuture<List<Pet>> done = new CompletableFuture<>();
        async.accept(() -> done.complete(store.of(owner)));
        return done;
    }

    public void forget(UUID owner) {
        held.remove(owner);
    }

    public boolean loaded(UUID owner) {
        return held.containsKey(owner);
    }

    public Holdings of(UUID owner) {
        return held.getOrDefault(owner, Holdings.NONE);
    }

    public List<Pet> pets(UUID owner) {
        return of(owner).pets();
    }

    public Optional<Pet> pet(UUID owner, UUID id) {
        return pets(owner).stream().filter(pet -> pet.id().equals(id)).findFirst();
    }

    /** By its name, or else the first of a species — what a player types. */
    public Optional<Pet> find(UUID owner, String typed) {
        if (typed == null || typed.isBlank()) {
            return Optional.empty();
        }
        String wanted = typed.trim();
        List<Pet> pets = pets(owner);
        return pets.stream().filter(pet -> pet.name().equalsIgnoreCase(wanted)).findFirst()
                .or(() -> pets.stream().filter(pet -> pet.name().replace(' ', '_').equalsIgnoreCase(wanted)).findFirst())
                .or(() -> pets.stream().filter(pet -> pet.species().equalsIgnoreCase(wanted)).findFirst());
    }

    public boolean owns(UUID owner, String species) {
        return pets(owner).stream().anyMatch(pet -> pet.species().equals(species));
    }

    // ------------------------------------------------------------------ asking

    public boolean isSupporter(Player player) {
        return supporters.isSupporter(player.getUniqueId()) || player.hasPermission("rains.supporter");
    }

    public UnlockRule.Verdict verdict(Player player, Species species) {
        UUID owner = player.getUniqueId();
        Map<String, Long> done = of(owner).progress();
        List<Long> progress = new ArrayList<>();
        for (int part = 0; part < species.unlock().needs().size(); part++) {
            progress.add(done.getOrDefault(de.raindancer.modules.pets.model.Unlock.progressKey(species.id(), part), 0L));
        }
        return unlocks.judge(species, new UnlockRule.Facts(owns(owner, species.id()), isSupporter(player),
                UnlockRule.grants(granted(player), species.id()), progress));
    }

    /** The nodes this player was granted, as written — see {@link UnlockRule#grants}. */
    private static java.util.Set<String> granted(Player player) {
        java.util.Set<String> held = new java.util.HashSet<>();
        for (org.bukkit.permissions.PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            if (info.getValue() && info.getPermission().startsWith("pets.species.")) {
                held.add(info.getPermission());
            }
        }
        return held;
    }

    public boolean knows(Player player, Species species) {
        UnlockRule.State state = verdict(player, species).state();
        boolean open = state == UnlockRule.State.CLAIMABLE || state == UnlockRule.State.OWNED;
        return discovery.known(species.rarity(), new DiscoveryRule.Facts(
                of(player.getUniqueId()).seen().contains(species.id()), owns(player.getUniqueId(), species.id()), open));
    }

    // ------------------------------------------------------------------ changing

    public Adoption claim(Player player, Species species) {
        if (!loaded(player.getUniqueId())) {
            return new Adoption(Outcome.NOT_LOADED, null);
        }
        UnlockRule.Verdict verdict = verdict(player, species);
        return switch (verdict.state()) {
            case OWNED -> new Adoption(Outcome.ALREADY_OWNED, null);
            case CLAIMABLE -> new Adoption(Outcome.ADOPTED, adopt(player.getUniqueId(), species, species.title()));
            case BUYABLE -> buy(player, species);
            case LOCKED -> new Adoption(Outcome.LOCKED, null);
            case SUPPORTERS_ONLY -> new Adoption(Outcome.SUPPORTERS_ONLY, null);
            case RANK_ONLY -> new Adoption(Outcome.RANK_ONLY, null);
        };
    }

    public Adoption buy(Player player, Species species) {
        UnlockRule.Verdict verdict = verdict(player, species);
        if (verdict.state() == UnlockRule.State.OWNED) {
            return new Adoption(Outcome.ALREADY_OWNED, null);
        }
        if (verdict.state() != UnlockRule.State.BUYABLE) {
            return claim(player, species);
        }
        EconomyResult paid = Fees.charge(player.getUniqueId(), verdict.price(), "Pet: " + species.title(), SOURCE);
        if (!paid.succeeded()) {
            return new Adoption(paid.outcome() == EconomyResult.Outcome.NOT_ENOUGH ? Outcome.TOO_POOR : Outcome.NO_ECONOMY,
                    null);
        }
        return new Adoption(Outcome.ADOPTED, adopt(player.getUniqueId(), species, species.title()));
    }

    /** An admin's gift: owned at once, whatever the way. Works for somebody offline, too. */
    public Pet grant(UUID owner, Species species, String name) {
        return adopt(owner, species, name == null || name.isBlank() ? species.title() : name);
    }

    /** An admin taking a pet away for good. */
    public void take(Pet pet) {
        held.computeIfPresent(pet.owner(), (who, holdings) -> new Holdings(
                holdings.pets().stream().filter(each -> !each.id().equals(pet.id())).toList(),
                holdings.progress(), holdings.seen()));
        async.accept(() -> store.remove(pet.id()));
    }

    private Pet adopt(UUID owner, Species species, String name) {
        Pet pet = Pet.adopt(owner, species.id(), name, Temperament.born(random), clock.instant());
        held.computeIfPresent(owner, (who, holdings) -> {
            List<Pet> pets = new ArrayList<>(holdings.pets());
            pets.add(pet);
            return new Holdings(pets, holdings.progress(), holdings.seen());
        });
        async.accept(() -> store.save(pet));
        return pet;
    }

    /** Keeps a changed pet and writes it behind. */
    public Pet update(Pet pet) {
        held.computeIfPresent(pet.owner(), (who, holdings) -> new Holdings(
                holdings.pets().stream().map(each -> each.id().equals(pet.id()) ? pet : each).toList(),
                holdings.progress(), holdings.seen()));
        async.accept(() -> store.save(pet));
        return pet;
    }

    /**
     * Counts something a player did towards one part of earning a species.
     *
     * @return whether this was the time the whole species opened
     */
    public boolean addProgress(Player player, Species species, int part, long amount) {
        UUID owner = player.getUniqueId();
        if (amount <= 0 || !loaded(owner)) {
            return false;
        }
        boolean before = verdict(player, species).state() == UnlockRule.State.CLAIMABLE;
        String key = de.raindancer.modules.pets.model.Unlock.progressKey(species.id(), part);
        held.computeIfPresent(owner, (who, holdings) -> {
            Map<String, Long> progress = new HashMap<>(holdings.progress());
            progress.merge(key, amount, Long::sum);
            return new Holdings(holdings.pets(), progress, holdings.seen());
        });
        async.accept(() -> store.addProgress(owner, key, amount));
        return !before && verdict(player, species).state() == UnlockRule.State.CLAIMABLE;
    }

    /** @return whether it was new to them */
    public boolean markSeen(UUID owner, String species) {
        Holdings holdings = held.get(owner);
        if (holdings == null || holdings.seen().contains(species)) {
            return false;
        }
        held.computeIfPresent(owner, (who, now) -> {
            Set<String> seen = new HashSet<>(now.seen());
            seen.add(species);
            return new Holdings(now.pets(), now.progress(), seen);
        });
        async.accept(() -> store.markSeen(owner, species));
        return true;
    }

    public SpeciesCatalogue catalogue() {
        return catalogue;
    }

    public Optional<Species> speciesOf(Pet pet) {
        return catalogue.find(pet.species());
    }

    public static String key(String typed) {
        return typed == null ? "" : typed.trim().toLowerCase(Locale.ROOT);
    }
}
