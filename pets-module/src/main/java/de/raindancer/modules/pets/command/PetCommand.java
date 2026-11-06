package de.raindancer.modules.pets.command;

import de.raindancer.modules.pets.PetServices;
import de.raindancer.modules.pets.model.Mode;
import de.raindancer.modules.pets.model.Personality;
import de.raindancer.modules.pets.model.Pet;
import de.raindancer.modules.pets.model.Species;
import de.raindancer.modules.pets.service.CollectionService;
import de.raindancer.modules.pets.util.PermissionNodes;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * {@code /pets} — the menu with nothing after it; the quick things typed: {@code call}, {@code home}, {@code stay},
 * {@code follow}, {@code mode}, {@code rename}, {@code colours}; {@code species}; and for staff {@code admin give},
 * {@code take}, {@code personality}, {@code list}.
 */
public final class PetCommand implements IPetCommand {

    private static final List<String> OWN = List.of("call", "home", "stay", "follow", "mode", "rename", "colours",
            "particle", "mute", "species", "hideothers");
    private static final List<String> ADMIN = List.of("give", "take", "personality", "list");

    private final Supplier<PetServices> services;

    public PetCommand(Supplier<PetServices> services) {
        this.services = services;
    }

    @Override
    public void execute(@NotNull CommandSourceStack source, String @NotNull [] args) {
        PetServices live = services.get();
        CommandSender sender = source.getSender();
        if (args.length > 0 && args[0].equalsIgnoreCase("admin")) {
            admin(live, sender, Arrays.copyOfRange(args, 1, args.length));
            return;
        }
        if (!(sender instanceof Player player)) {
            live.messages().send(sender, "pets.only-a-player");
            return;
        }
        if (args.length == 0) {
            live.screens().collection(player);
            return;
        }
        String verb = args[0].toLowerCase(Locale.ROOT);
        if (verb.equals("species")) {
            live.screens().species(player);
            return;
        }
        if (verb.equals("hideothers")) {
            live.care().toggleOthers(player);
            return;
        }
        if (!OWN.contains(verb)) {
            live.messages().send(player, "pets.usage");
            return;
        }
        if (args.length < 2) {
            live.messages().send(player, "pets.which-pet");
            return;
        }
        if (verb.equals("home") && args[1].equalsIgnoreCase("all")) {
            live.summoning().dismissAll(player.getUniqueId());
            live.messages().send(player, "pets.all-home");
            return;
        }
        Optional<Pet> found = live.collection().find(player.getUniqueId(), args[1]);
        if (found.isEmpty()) {
            live.messages().send(player, "pets.no-such-pet", "name", args[1]);
            return;
        }
        Pet pet = found.get();
        switch (verb) {
            case "call" -> live.care().call(player, pet);
            case "home" -> live.care().home(player, pet);
            case "stay" -> live.care().stay(player, pet, true);
            case "follow" -> live.care().stay(player, pet, false);
            case "mode" -> {
                Optional<Mode> mode = args.length > 2 ? Mode.parse(args[2]) : Optional.of(pet.mode().next());
                if (mode.isEmpty()) {
                    live.messages().send(player, "pets.bad-mode");
                    return;
                }
                live.care().mode(player, pet, mode.get());
            }
            case "rename" -> {
                if (args.length < 3) {
                    live.messages().send(player, "pets.bad-name", "longest", 20);
                    return;
                }
                live.care().rename(player, pet, String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
            }
            case "mute" -> live.care().mute(player, pet);
            case "particle" -> {
                if (args.length == 2) {
                    live.screens().particles(player, pet);
                    return;
                }
                String particle = args.length > 2 && !args[2].equalsIgnoreCase("off")
                        ? args[2].toUpperCase(Locale.ROOT) : "";
                de.raindancer.core.ui.effect.ParticleShape shape = args.length > 3
                        ? de.raindancer.modules.pets.service.SummonService.shape(args[3].toUpperCase(Locale.ROOT))
                        : de.raindancer.modules.pets.service.SummonService.shape(pet.particleShape());
                live.care().particle(player, pet, particle, shape);
            }
            case "colours" -> {
                if (args.length > 2) {
                    live.care().style(player, pet, List.of(Arrays.copyOfRange(args, 2, args.length)));
                } else {
                    live.screens().pet(player, pet);
                }
            }
            default -> live.messages().send(player, "pets.usage");
        }
    }

    // ------------------------------------------------------------------ staff

    private void admin(PetServices live, CommandSender sender, String[] args) {
        if (!sender.hasPermission(PermissionNodes.ADMIN)) {
            live.messages().send(sender, "pets.no-permission");
            return;
        }
        if (args.length < 2) {
            live.messages().send(sender, "pets.admin-usage");
            return;
        }
        OfflinePlayer target = live.server().getOfflinePlayerIfCached(args[1]);
        if (target == null) {
            live.messages().send(sender, "pets.unknown-player", "player", args[1]);
            return;
        }
        UUID owner = target.getUniqueId();
        String who = target.getName() == null ? args[1] : target.getName();
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "give" -> {
                Optional<Species> species = args.length > 2 ? live.collection().catalogue().find(args[2])
                        : Optional.empty();
                if (species.isEmpty()) {
                    live.messages().send(sender, "pets.unknown-species", "species", args.length > 2 ? args[2] : "");
                    return;
                }
                String name = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : null;
                if (name != null && live.names().clean(name).isEmpty()) {
                    live.messages().send(sender, "pets.bad-name", "longest", 20);
                    return;
                }
                Pet given = live.collection().grant(owner, species.get(),
                        name == null ? null : live.names().clean(name).orElseThrow());
                live.messages().send(sender, "pets.admin-given", "player", who, "name", given.name(),
                        "species", species.get().title());
                if (target.getPlayer() != null) {
                    live.messages().send(target.getPlayer(), "pets.gift", "name", given.name(),
                            "species", species.get().title());
                }
            }
            case "take" -> withPet(live, sender, owner, who, args, pet -> {
                live.summoning().byPet(pet.id()).ifPresent(live.summoning()::dismiss);
                live.collection().take(pet);
                live.messages().send(sender, "pets.admin-taken", "player", who, "name", pet.name());
            });
            case "personality" -> withPet(live, sender, owner, who, args, pet -> {
                Optional<Personality> personality = args.length > 3 ? Personality.parse(args[3]) : Optional.empty();
                if (personality.isEmpty()) {
                    live.messages().send(sender, "pets.bad-personality", "all", Arrays.stream(Personality.values())
                            .map(each -> each.name().toLowerCase(Locale.ROOT)).toList().toString());
                    return;
                }
                Pet changed = live.experience().setPersonality(pet, personality.get());
                live.messages().send(sender, "pets.admin-personality", "player", who, "name", changed.name(),
                        "personality", personality.get().title());
            });
            case "list" -> live.collection().petsOf(owner).thenAccept(pets -> {
                if (pets.isEmpty()) {
                    live.messages().send(sender, "pets.admin-none", "player", who);
                    return;
                }
                live.messages().send(sender, "pets.admin-list-head", "player", who, "count", pets.size());
                for (Pet pet : pets) {
                    live.messages().send(sender, "pets.admin-list-line", "name", pet.name(), "species", pet.species(),
                            "level", pet.level(), "personality", pet.personality().title());
                }
            });
            default -> live.messages().send(sender, "pets.admin-usage");
        }
    }

    private void withPet(PetServices live, CommandSender sender, UUID owner, String who, String[] args,
                         java.util.function.Consumer<Pet> then) {
        if (args.length < 3) {
            live.messages().send(sender, "pets.admin-usage");
            return;
        }
        String typed = args[2];
        live.collection().petsOf(owner).thenAccept(pets -> {
            Optional<Pet> pet = pets.stream().filter(each -> each.name().equalsIgnoreCase(typed)
                            || each.name().replace(' ', '_').equalsIgnoreCase(typed)).findFirst()
                    .or(() -> pets.stream().filter(each -> each.species().equalsIgnoreCase(typed)).findFirst());
            if (pet.isEmpty()) {
                live.messages().send(sender, "pets.admin-no-such-pet", "player", who, "name", typed);
                return;
            }
            then.accept(pet.get());
        });
    }

    // ------------------------------------------------------------------ completing

    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack source, String @NotNull [] args) {
        PetServices live = services.get();
        CommandSender sender = source.getSender();
        String typed = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        List<String> options = new ArrayList<>();
        boolean admin = sender.hasPermission(PermissionNodes.ADMIN);
        if (args.length <= 1) {
            options.addAll(OWN);
            if (admin) {
                options.add("admin");
            }
        } else if (args[0].equalsIgnoreCase("admin") && admin) {
            if (args.length == 2) {
                options.addAll(ADMIN);
            } else if (args.length == 3) {
                live.server().getOnlinePlayers().forEach(player -> options.add(player.getName()));
            } else if (args.length == 4 && args[1].equalsIgnoreCase("give")) {
                live.collection().catalogue().all().forEach(species -> options.add(species.id()));
            } else if (args.length == 5 && args[1].equalsIgnoreCase("personality")) {
                Stream.of(Personality.values()).forEach(each -> options.add(each.name().toLowerCase(Locale.ROOT)));
            }
        } else if (args.length == 2 && sender instanceof Player player && OWN.contains(args[0].toLowerCase(Locale.ROOT))) {
            live.collection().pets(player.getUniqueId()).forEach(pet -> options.add(pet.name().replace(' ', '_')));
            if (args[0].equalsIgnoreCase("home")) {
                options.add("all");
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("mode")) {
            Stream.of(Mode.values()).forEach(mode -> options.add(mode.name().toLowerCase(Locale.ROOT)));
        }
        return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(typed)).limit(50)
                .toList();
    }

    @Override
    public @NotNull String permission() {
        return PermissionNodes.USE;
    }

    @Override
    public String describe() {
        return "your pets, and staff's tools for them";
    }

    static String key(String typed) {
        return CollectionService.key(typed);
    }
}
