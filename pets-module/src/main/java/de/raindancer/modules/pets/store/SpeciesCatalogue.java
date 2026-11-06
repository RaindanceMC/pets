package de.raindancer.modules.pets.store;

import de.raindancer.core.world.activity.ActivityKind;
import de.raindancer.modules.pets.model.Rarity;
import de.raindancer.modules.pets.model.Role;
import de.raindancer.modules.pets.model.Species;
import de.raindancer.modules.pets.model.Unlock;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The species a server offers: {@code pets.yml} in the data folder, written from the shipped one the first time and
 * the owner's own after that. A broken entry is named and skipped; the rest still read.
 *
 * <pre>
 * species:
 *   fox:
 *     title: Fox
 *     mob: FOX
 *     role: fighter          # fighter | guard | ranged | healer
 *     rarity: uncommon       # common … mythic
 *     icon: SWEET_BERRIES
 *     health: 16             # half hearts at level one
 *     damage: 3
 *     speed: 0.32            # blocks per tick
 *     size: 0.35             # player heights, 0.15 to 0.6; always smaller than the wild mob
 *     flies: false
 *     baby: true
 *     unlock: earn forage 150   # free | earn &lt;activity&gt; [thing] &lt;count&gt; [+ more parts] | drop monster|boss &lt;mob&gt; 1/&lt;n&gt; | buy &lt;price&gt; | permission | supporter
 *     blurb: Quick and sly.
 *     missile: SNOWBALL      # a ranged pet's: SNOWBALL, LLAMA_SPIT, SMALL_FIREBALL, WITHER_SKULL, WIND_CHARGE, ARROW,
 *                            # SHULKER_BULLET, DRAGON_FIREBALL — none of them breaks or burns anything
 * </pre>
 */
public final class SpeciesCatalogue {

    private final Path file;
    private final Supplier<InputStream> shipped;
    private volatile List<Species> species = List.of();

    public SpeciesCatalogue(Path file, Supplier<InputStream> shipped) {
        this.file = file;
        this.shipped = shipped;
    }

    /** Writes the shipped catalogue if there is none yet, then reads it. Disk work. */
    public void load(Consumer<String> problems) {
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                try (InputStream in = shipped.get()) {
                    Files.copy(in, file);
                }
            }
            try (InputStream in = Files.newInputStream(file)) {
                species = read(in, problems);
            }
        } catch (IOException unreadable) {
            problems.accept("pets.yml could not be read (" + unreadable.getMessage() + "), using the shipped species");
            try (InputStream in = shipped.get()) {
                species = read(in, problems);
            } catch (IOException alsoUnreadable) {
                throw new UncheckedIOException(alsoUnreadable);
            }
        }
    }

    public List<Species> all() {
        return species;
    }

    public Optional<Species> find(String id) {
        if (id == null) {
            return Optional.empty();
        }
        String key = id.trim().toLowerCase(Locale.ROOT);
        return species.stream().filter(each -> each.id().equals(key)).findFirst();
    }

    public static List<Species> read(InputStream in, Consumer<String> problems) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        ConfigurationSection all = yaml.getConfigurationSection("species");
        List<Species> read = new ArrayList<>();
        if (all == null) {
            problems.accept("pets.yml has no species: section");
            return read;
        }
        for (String rawId : all.getKeys(false)) {
            ConfigurationSection entry = all.getConfigurationSection(rawId);
            String id = rawId.trim().toLowerCase(Locale.ROOT);
            if (entry == null) {
                problems.accept(id + ": not a section");
                continue;
            }
            try {
                read.add(species(id, entry));
            } catch (IllegalArgumentException broken) {
                problems.accept(id + ": " + broken.getMessage());
            }
        }
        return List.copyOf(read);
    }

    private static Species species(String id, ConfigurationSection entry) {
        String mob = entry.getString("mob", "").trim().toUpperCase(Locale.ROOT);
        EntityType type;
        try {
            type = EntityType.valueOf(mob);
        } catch (IllegalArgumentException unknown) {
            throw new IllegalArgumentException("mob '" + mob + "' is not one");
        }
        if (type.getEntityClass() == null || !Mob.class.isAssignableFrom(type.getEntityClass())) {
            throw new IllegalArgumentException("mob '" + mob + "' cannot be a pet");
        }
        Role role = Role.parse(entry.getString("role"))
                .orElseThrow(() -> new IllegalArgumentException("role '" + entry.getString("role")
                        + "' is not fighter, guard, ranged or healer"));
        Rarity rarity = Rarity.parse(entry.getString("rarity", "common"))
                .orElseThrow(() -> new IllegalArgumentException("rarity '" + entry.getString("rarity")
                        + "' is not common, uncommon, rare, epic, legendary or mythic"));
        Unlock unlock = unlock(entry.getString("unlock", "free"));
        if (unlock.way() == Unlock.Way.BUY && rarity != Rarity.COMMON) {
            throw new IllegalArgumentException("only common pets can be bought — a " + rarity.title().toLowerCase(Locale.ROOT)
                    + " one is earned, granted or kept for supporters");
        }
        Material icon = Material.matchMaterial(entry.getString("icon", "BONE"));
        if (icon == null || !icon.isItem()) {
            icon = Material.BONE;
        }
        return new Species(id, entry.getString("title", id), mob, role, rarity, icon,
                positive(entry.getDouble("health", 20), "health"), Math.max(0, entry.getDouble("damage", 3)),
                Math.max(0.05, Math.min(1.5, entry.getDouble("speed", 0.3))),
                entry.getDouble("size", 0.4), entry.getBoolean("flies", false),
                entry.getBoolean("baby", false), unlock,
                entry.getString("blurb", ""), entry.getString("missile", "SNOWBALL").trim().toUpperCase(Locale.ROOT));
    }

    private static double positive(double value, String what) {
        if (value <= 0) {
            throw new IllegalArgumentException(what + " has to be more than nothing");
        }
        return value;
    }

    private static de.raindancer.modules.pets.model.Requirement requirement(String[] words) {
        if (words.length < 2) {
            throw new IllegalArgumentException("each part of earn is what and how often: boss wither 3");
        }
        ActivityKind kind;
        try {
            kind = ActivityKind.valueOf(words[0].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            throw new IllegalArgumentException("'" + words[0] + "' is not something players do");
        }
        long count;
        try {
            count = Long.parseLong(words[words.length - 1]);
        } catch (NumberFormatException notANumber) {
            throw new IllegalArgumentException("each part of earn ends in a number: boss wither 3");
        }
        return new de.raindancer.modules.pets.model.Requirement(kind, words.length >= 3 ? words[1] : "", count);
    }

    static Unlock unlock(String written) {
        String[] words = written.trim().split("\\s+");
        String way = words[0].toLowerCase(Locale.ROOT);
        switch (way) {
            case "free" -> {
                return Unlock.free();
            }
            case "permission", "rank" -> {
                return Unlock.permission();
            }
            case "supporter", "supporters" -> {
                return Unlock.supporter();
            }
            case "buy" -> {
                if (words.length < 2) {
                    throw new IllegalArgumentException("buy needs a price: buy 2.5k");
                }
                return Unlock.buy(words[1]);
            }
            case "earn" -> {
                String rest = written.trim().substring(4).trim();
                if (rest.isEmpty()) {
                    throw new IllegalArgumentException("earn needs what and how often: earn forage 150");
                }
                java.util.List<de.raindancer.modules.pets.model.Requirement> needs = new java.util.ArrayList<>();
                for (String part : rest.split("\\+")) {
                    needs.add(requirement(part.trim().split("\\s+")));
                }
                return Unlock.earnAll(needs);
            }
            case "drop" -> {
                return drop(words);
            }
            default -> throw new IllegalArgumentException("unlock '" + written
                    + "' is not free, earn …, drop …, buy …, permission or supporter");
        }
    }

    /** {@code drop monster blaze 1/800}, {@code drop boss wither 0.5%}. */
    private static Unlock drop(String[] words) {
        if (words.length != 4) {
            throw new IllegalArgumentException("drop needs a monster or boss, which one and how rarely: drop monster blaze 1/800");
        }
        de.raindancer.core.world.activity.ActivityKind kind = switch (words[1].toLowerCase(Locale.ROOT)) {
            case "monster" -> de.raindancer.core.world.activity.ActivityKind.MONSTER;
            case "boss" -> de.raindancer.core.world.activity.ActivityKind.BOSS;
            default -> throw new IllegalArgumentException("only a monster or a boss drops a pet, not " + words[1]);
        };
        double chance;
        try {
            String written = words[3];
            if (written.endsWith("%")) {
                chance = Double.parseDouble(written.substring(0, written.length() - 1)) / 100;
            } else if (written.contains("/")) {
                String[] split = written.split("/", 2);
                chance = Double.parseDouble(split[0]) / Double.parseDouble(split[1]);
            } else {
                chance = Double.parseDouble(written);
            }
        } catch (NumberFormatException notANumber) {
            throw new IllegalArgumentException("a drop's chance is written 1/800 or 0.125%, not " + words[3]);
        }
        if (!(chance > 0 && chance <= 1)) {
            throw new IllegalArgumentException("a drop's chance lies above 0 and at most 1 in 1, not " + words[3]);
        }
        return Unlock.drop(kind, words[2], chance);
    }
}
