package de.raindancer.modules.pets;

import de.raindancer.core.RainsCore;
import de.raindancer.core.platform.log.LogChannel;
import de.raindancer.core.ui.chat.Brand;
import de.raindancer.core.ui.messages.Messages;
import de.raindancer.modules.pets.rules.PetNameRule;
import de.raindancer.modules.pets.service.CareService;
import de.raindancer.modules.pets.service.CollectionService;
import de.raindancer.modules.pets.service.ExperienceService;
import de.raindancer.modules.pets.service.SummonService;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;

import java.util.function.Supplier;

/** Everything this module built, in one place, handed to listeners, screens and commands. */
public record PetServices(
        Plugin plugin,
        Server server,
        RainsCore core,
        LogChannel log,
        Messages messages,
        Brand brand,
        Supplier<PetSettings> settings,
        CollectionService collection,
        SummonService summoning,
        ExperienceService experience,
        CareService care,
        PetNameRule names,
        IPetScreensOpener screens) {

    public PetSettings config() {
        return settings.get();
    }
}
