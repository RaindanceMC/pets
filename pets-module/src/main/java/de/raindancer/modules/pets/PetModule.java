package de.raindancer.modules.pets;

import de.raindancer.core.data.settings.SettingsStore;
import de.raindancer.core.platform.log.LogChannel;
import de.raindancer.core.platform.util.Scheduling;
import de.raindancer.core.world.activity.Activities;
import de.raindancer.modules.api.FlexModule;
import de.raindancer.modules.api.ModuleCommand;
import de.raindancer.modules.api.ModuleContext;
import de.raindancer.modules.api.ModuleInfo;
import de.raindancer.modules.pets.listener.PetSessionListener;
import de.raindancer.modules.pets.model.Pet;
import de.raindancer.modules.pets.rules.DiscoveryRule;
import de.raindancer.modules.pets.rules.PetNameRule;
import de.raindancer.modules.pets.rules.UnlockRule;
import de.raindancer.modules.pets.screen.PetCollectionMenu;
import de.raindancer.modules.pets.screen.PetMenu;
import de.raindancer.modules.pets.screen.SpeciesMenu;
import de.raindancer.modules.pets.service.CareService;
import de.raindancer.modules.pets.service.CollectionService;
import de.raindancer.modules.pets.service.ExperienceService;
import de.raindancer.modules.pets.service.PetHooks;
import de.raindancer.modules.pets.service.SummonService;
import de.raindancer.modules.pets.service.UnlockService;
import de.raindancer.modules.pets.store.PetDatabase;
import de.raindancer.modules.pets.store.PetStore;
import de.raindancer.modules.pets.store.SpeciesCatalogue;
import de.raindancer.modules.pets.util.PermissionNodes;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.time.Clock;
import java.util.List;

/**
 * Pets, as a module: species from {@code pets.yml}, each player's collection, levels and personalities, how a species
 * is earned, bought, granted or kept for supporters, and the screens. The pets in the world are Core's companions —
 * made, driven, protected and taken away by Core — so nothing here touches a mob.
 */
public final class PetModule implements FlexModule {

    private static final ModuleInfo INFO = ModuleInfo.of("pets", "Pets", "0.4.0")
            .describedAs("Pets that fight beside you: follow, defend, level up, can be ridden, and come back after "
                    + "a knock-out")
            .by("Raindancer118");

    private LogChannel log;
    private SettingsStore<PetSettings> settings;
    private SummonService summoning;
    private Object activities;

    @Override
    public ModuleInfo info() {
        return INFO;
    }

    @Override
    public void enable(ModuleContext context) {
        log = context.log();
        Server server = context.plugin().getServer();
        settings = context.settings(PetSettings.class, PetSettings.DEFAULTS);
        context.core().messages().defineFrom(PetModule.class.getResourceAsStream("messages.yml"),
                context.chat().brand()::chatPrefix);
        context.core().messages().seriousFrom(PetModule.class.getResourceAsStream("messages-serious.yml"));

        SpeciesCatalogue catalogue = new SpeciesCatalogue(context.dataFolder().resolve("pets.yml"),
                () -> PetModule.class.getResourceAsStream("pets.yml"));
        catalogue.load(problem -> log.warn("pets.yml: {}", problem));
        int registered = PermissionNodes.register(server, catalogue.all());
        if (registered > 0) {
            log.info("{} permission(s) registered.", registered);
        }

        Clock clock = Clock.systemUTC();
        PetSettings now = settings.current();
        PetStore store = new PetStore(context.core().databases().of(PetDatabase.NAME, PetDatabase.SCHEMA));
        CollectionService collection = new CollectionService(store, catalogue, new UnlockRule(), new DiscoveryRule(),
                context.core().supporters(), task -> Scheduling.async(context.plugin(), task), clock, now);
        summoning = new SummonService(context.plugin(), context.core().companions(), collection, clock, now);
        ExperienceService experience = new ExperienceService(collection, summoning, context.core().messages(),
                server::getPlayer, now);
        PetNameRule names = new PetNameRule();
        CareService care = new CareService(collection, summoning, experience, names, context.core().messages(),
                clock, now);
        UnlockService unlocks = new UnlockService(collection, context.core().messages(), now);

        PetServices[] built = new PetServices[1];
        IPetScreensOpener screens = new IPetScreensOpener() {
            @Override
            public void collection(Player viewer) {
                new PetCollectionMenu(built[0], viewer, null).open();
            }

            @Override
            public void species(Player viewer) {
                new SpeciesMenu(built[0], viewer, null).open();
            }

            @Override
            public void pet(Player viewer, Pet pet) {
                new PetMenu(built[0], viewer, null, pet.id()).open();
            }

            @Override
            public void particles(Player viewer, Pet pet) {
                new de.raindancer.modules.pets.screen.PetParticleMenu(built[0], viewer, null, pet.id()).open();
            }
        };
        PetHooks hooks = new PetHooks(collection, experience, context.core().messages(), server::getPlayer,
                screens::collection, clock, now);
        summoning.tellThrough(hooks::of);
        built[0] = new PetServices(context.plugin(), server, context.core(), log, context.core().messages(),
                context.chat().brand(), settings::current, collection, summoning, experience, care, names, screens);

        settings.onChange(fresh -> {
            collection.settings(fresh);
            summoning.settings(fresh);
            experience.settings(fresh);
            care.settings(fresh);
            unlocks.settings(fresh);
            hooks.settings(fresh);
        });

        activities = Activities.listen(context.plugin(), (player, activity) -> {
            unlocks.did(player, activity);
            if (activity.kind() == de.raindancer.core.world.activity.ActivityKind.EXPLORE) {
                experience.explored(summoning.of(player.getUniqueId()));
            }
        });
        context.listener(new PetSessionListener(built[0]));
        for (Player online : server.getOnlinePlayers()) {
            collection.load(online.getUniqueId());
        }
        PetCommands.ready(built[0]);
        log.info("Pets is up: {} species.", catalogue.all().size());
    }

    @Override
    public List<ModuleCommand> commands() {
        return PetCommands.declared();
    }

    @Override
    public void disable() {
        PetCommands.stopped();
        if (activities != null) {
            Activities.stop(activities);
        }
        if (summoning != null) {
            summoning.companions().forgetFrom(summoning.plugin());
        }
    }
}
