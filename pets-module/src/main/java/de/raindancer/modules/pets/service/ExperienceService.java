package de.raindancer.modules.pets.service;

import de.raindancer.core.ui.messages.Messages;
import de.raindancer.modules.pets.PetSettings;
import de.raindancer.modules.pets.model.Levels;
import de.raindancer.modules.pets.model.OutPet;
import de.raindancer.modules.pets.model.Personality;
import de.raindancer.modules.pets.model.Pet;
import de.raindancer.modules.pets.model.Temperament;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.UUID;
import java.util.function.Function;

/**
 * How pets grow and change: experience for a kill, and the little nudges a life with their owner gives their nature —
 * fighting together makes one bolder and wilder, riding livelier, being fed gentler. Never more than
 * {@link Temperament#MOST_NUDGE} from how it was born.
 */
public final class ExperienceService implements IPetService {

    /** A boss is worth this many ordinary kills. */
    public static final int BOSS_TIMES = 25;
    /** Blocks ridden for one step of energy. */
    public static final double RIDE_PER_NUDGE = 200;
    /** Between two treats that count, in milliseconds. */
    public static final long TREAT_EVERY = 30_000;

    private final CollectionService collection;
    private final SummonService summoning;
    private final Messages messages;
    private final Function<UUID, Player> online;
    private final Random random = new Random();
    private volatile PetSettings settings;

    public ExperienceService(CollectionService collection, SummonService summoning, Messages messages,
                             Function<UUID, Player> online, PetSettings settings) {
        this.collection = collection;
        this.summoning = summoning;
        this.messages = messages;
        this.online = online;
        this.settings = settings;
    }

    @Override
    public void settings(PetSettings fresh) {
        settings = fresh;
    }

    /** It finished something off. */
    public void killed(OutPet live, boolean boss, boolean farmed) {
        Pet pet = current(live);
        int gained = farmed ? 0 : settings.killExperience() * (boss ? BOSS_TIMES : 1);
        Levels.Progress progress = summoning.levels().gain(pet.level(), pet.xp(), gained);
        Pet grown = pet.withProgress(progress.level(), progress.xp());
        Temperament nature = grown.temperament();
        if (random.nextInt(3) == 0) {
            nature = nature.nudge(Temperament.Trait.COURAGE, 1);
        }
        if (random.nextInt(3) == 0) {
            nature = nature.nudge(Temperament.Trait.TEMPER, 1);
        }
        Pet changed = changeNature(grown, nature);
        Player owner = online.apply(pet.owner());
        if (progress.levelsGained() > 0 && owner != null) {
            messages.send(owner, "pets.level-up", "name", changed.name(), "level", changed.level());
        }
        if (progress.levelsGained() > 0 || !changed.equals(pet)) {
            keep(changed);
        }
    }

    /** Its rider took it further. */
    public void ridden(OutPet live, double blocks) {
        if (live.ride(blocks) < RIDE_PER_NUDGE) {
            return;
        }
        live.resetRide();
        Pet pet = current(live);
        keep(changeNature(pet, pet.temperament().nudge(Temperament.Trait.ENERGY, 1)));
    }

    /** Its owner found somewhere new: every pet out with them grows a little more curious. */
    public void explored(java.util.List<OutPet> out) {
        for (OutPet live : out) {
            Pet pet = current(live);
            keep(changeNature(pet, pet.temperament().nudge(Temperament.Trait.CURIOSITY, 1)));
        }
    }

    /** Its owner gave it a treat. @return whether it ate */
    public boolean fed(OutPet live, Player owner, long now) {
        if (now - live.lastFed() < TREAT_EVERY) {
            messages.send(owner, "pets.not-hungry", "name", live.pet().name());
            return false;
        }
        live.fedAt(now);
        summoning.heal(live, 6);
        Pet pet = current(live);
        keep(changeNature(pet, pet.temperament().nudge(Temperament.Trait.TEMPER, -2)));
        messages.send(owner, "pets.fed", "name", pet.name());
        return true;
    }

    /** An admin's choice: exactly that nature, the training forgotten. */
    public Pet setPersonality(Pet pet, Personality personality) {
        return keep(pet.withTemperament(pet.temperament().becoming(personality)));
    }

    private Pet changeNature(Pet pet, Temperament nature) {
        Personality before = pet.personality();
        Pet changed = pet.withTemperament(nature);
        Personality after = changed.personality();
        Player owner = online.apply(pet.owner());
        if (before != after && owner != null) {
            messages.send(owner, "pets.personality-changed", "name", pet.name(), "personality", after.title());
        }
        return changed;
    }

    private Pet current(OutPet live) {
        return collection.pet(live.pet().owner(), live.pet().id()).orElse(live.pet());
    }

    /** Keeps it and puts it on the companion that is out. */
    public Pet keep(Pet pet) {
        collection.update(pet);
        Player owner = online.apply(pet.owner());
        summoning.refresh(pet, owner == null ? "" : owner.getName());
        return pet;
    }
}
