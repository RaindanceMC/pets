package de.raindancer.modules.pets.screen;

import de.raindancer.modules.pets.model.Levels;
import de.raindancer.modules.pets.model.Pet;
import de.raindancer.modules.pets.model.Rarity;
import de.raindancer.modules.pets.model.Species;
import de.raindancer.modules.pets.model.Temperament;

import java.util.ArrayList;
import java.util.List;

/** The lines a pet is described with, shared by the screens. */
final class PetLook {

    private PetLook() {
    }

    static String rarity(Rarity rarity) {
        return "<color:" + rarity.colour() + ">" + rarity.title();
    }

    static List<String> describe(Pet pet, Species species, Levels levels) {
        List<String> lore = new ArrayList<>();
        lore.add(rarity(species.rarity()) + " <dark_gray>· <gray>" + species.title() + " <dark_gray>· <gray>"
                + species.role().title());
        long next = levels.toNext(pet.level());
        lore.add("<gray>Level <white>" + pet.level() + (pet.level() >= levels.maxLevel() ? " <gold>(highest)"
                : " <dark_gray>" + bar(pet.xp(), next) + " <gray>" + pet.xp() + "/" + next));
        lore.add("<gray>Personality: <white>" + pet.personality().title() + " <dark_gray>— "
                + pet.personality().blurb());
        Temperament nature = pet.temperament();
        lore.add("<dark_gray>Courage " + nature.courage() + " · Energy " + nature.energy() + " · Temper "
                + nature.temper());
        lore.add("<gray>Mode: <white>" + pet.mode().title());
        return lore;
    }

    static String bar(long have, long need) {
        int filled = need <= 0 ? 10 : (int) Math.min(10, have * 10 / need);
        return "<green>" + "|".repeat(filled) + "<dark_gray>" + "|".repeat(10 - filled);
    }
}
