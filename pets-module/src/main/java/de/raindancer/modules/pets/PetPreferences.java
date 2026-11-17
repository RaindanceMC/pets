package de.raindancer.modules.pets;

import de.raindancer.core.ui.preference.Preference;
import de.raindancer.modules.pets.service.CareService;

import java.util.List;
import java.util.function.Supplier;

/** Pets' personal switches for Core's {@code /preferences}; the commands {@code /pets hideothers} and {@code squabbles} stay. */
final class PetPreferences {

    private static final String GROUP = "Pets";

    private PetPreferences() {
    }

    static List<Preference> build(CareService care, Supplier<PetSettings> settings) {
        Preference others = Preference.toggle("pets:see-others", GROUP, "Other people's pets",
                "Shows the pets that belong to other players. Off, you see only your own.",
                CareService.SEE_OTHERS::isOn, care::setOthers);
        Preference squabbles = Preference.toggle("pets:squabbles", GROUP, "Pet squabbles",
                "Lets your fierce pets square up to other players' pets, just for show.",
                CareService.SQUABBLES::isOn, care::setSquabbles)
                .shownTo(player -> settings.get().squabbles());
        return List.of(others, squabbles);
    }
}
