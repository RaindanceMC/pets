package de.raindancer.modules.pets;

import de.raindancer.core.data.settings.Describe;
import de.raindancer.core.data.settings.In;
import de.raindancer.core.data.settings.Key;
import de.raindancer.core.data.settings.Range;
import de.raindancer.core.data.settings.Settings;
import de.raindancer.core.data.settings.Title;
import de.raindancer.core.data.settings.Topic;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** What an owner can decide about pets. The species themselves are in {@code pets.yml}. */
@Settings(id = "pets", topics = {
        @Topic(path = "pets", title = "Pets", icon = Material.BONE),
        @Topic(path = "pets/out", title = "Out with you", icon = Material.LEAD),
        @Topic(path = "pets/growing", title = "Growing", icon = Material.EXPERIENCE_BOTTLE),
})
public record PetSettings(

        @In("pets/out") @Title("Pets out at once") @Range(min = 1, max = 5)
        @Describe("How many of their pets a player may have out at the same time.")
        @Key("pets.out-at-once")
        int outAtOnce,

        @In("pets/out") @Title("Pets out at once, for supporters") @Range(min = 1, max = 5)
        @Describe("The same for the server's supporters — a thank-you. Never fewer than for everybody.")
        @Key("pets.out-at-once-supporters")
        int outAtOnceSupporters,

        @In("pets/out") @Title("Knocked out for (seconds)") @Range(min = 5, max = 3600)
        @Describe("A pet beaten in a fight is never lost: it is knocked out and can be called again after this long.")
        @Key("pets.knocked-out-seconds")
        int knockedOutSeconds,

        @In("pets/out") @Title("Pets can be ridden")
        @Describe("Whether right-clicking your own pet climbs on. Flying pets fly with you; jump climbs.")
        @Key("pets.riding")
        boolean riding,

        @In("pets/out") @Title("Catch up from (blocks)") @Range(min = 8, max = 64)
        @Describe("A pet further than this from its owner is brought to them.")
        @Key("pets.catch-up-blocks")
        int catchUpBlocks,

        @In("pets/out") @Title("Worlds without pets")
        @Describe("World names, separated by commas, where no pet may be out — a minigame's, say.")
        @Key("pets.worlds-without")
        String worldsWithout,

        @In("pets/growing") @Title("Highest level") @Range(min = 1, max = 100)
        @Describe("How far a pet can grow. Each level adds to its health and its bite.")
        @Key("pets.highest-level")
        int highestLevel,

        @In("pets/growing") @Title("Experience for a kill") @Range(min = 0, max = 1000)
        @Describe("What a pet earns for a monster it finished off. A boss is worth twenty-five times as much; "
                + "monsters from spawners nothing.")
        @Key("pets.kill-experience")
        int killExperience,

        @In("pets") @Title("Pet names may be coloured without the cosmetics plugin")
        @Describe("With Rain's Cosmetics, a pet's name may be painted exactly as its owner may paint their own. "
                + "Without it, this decides whether colours and gradients are open to everybody.")
        @Key("pets.colours-without-cosmetics")
        boolean coloursWithoutCosmetics,

        @In("pets") @Title("Seen from (blocks)") @Range(min = 4, max = 64)
        @Describe("Rarer species stay hidden in /pets until a player has seen one out with somebody this close.")
        @Key("pets.discovery-blocks")
        int discoveryBlocks,

        @In("pets") @Title("Owner's name behind a pet's")
        @Describe("Off, a pet shows only its own name; whoever right-clicks somebody else's pet is told whose it is.")
        @Key("pets.owner-in-name")
        boolean ownerInName,

        @In("pets") @Title("Pets squabble")
        @Describe("Fierce pets now and then square up to other players' pets — they huff and hop, nobody is hurt. "
                + "Off, no pet ever does; on, each player can still turn it off for their own (/pets squabbles).")
        @Key("pets.squabbles")
        boolean squabbles) {

    public static final PetSettings DEFAULTS = new PetSettings(1, 2, 120, true, 24, "", 30, 12, true, 16, false, true);

    public int outFor(boolean supporter) {
        int everybody = Math.max(1, Math.min(5, outAtOnce));
        return supporter ? Math.max(everybody, Math.min(5, outAtOnceSupporters)) : everybody;
    }

    public long knockedOutMillis() {
        return Math.max(5, Math.min(3600, knockedOutSeconds)) * 1000L;
    }

    public Set<String> worldsWithoutPets() {
        if (worldsWithout == null || worldsWithout.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(worldsWithout.split(",")).map(String::trim).filter(name -> !name.isEmpty())
                .map(name -> name.toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
    }

    public boolean petsAllowedIn(String world) {
        return world != null && !worldsWithoutPets().contains(world.toLowerCase(Locale.ROOT));
    }
}
