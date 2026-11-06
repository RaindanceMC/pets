package de.raindancer.modules.pets;

import de.raindancer.core.ui.help.HelpLint;
import de.raindancer.modules.api.ModuleCommand;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class GuideTest {

    @Test
    void theGuideIsComplete() {
        assertThat(HelpLint.of(Path.of("src/main/resources/rains-help"))
                .module("pets")
                .settings(PetSettings.class, PetSettings.DEFAULTS)
                .commands(new PetModule().commands().stream().map(ModuleCommand::name).toList())
                .problems()).isEmpty();
    }

    @Test
    @org.junit.jupiter.api.DisplayName("no text a player reads names a pet above common: they are secret until seen")
    void noRarePetNamed() throws java.io.IOException {
        java.util.List<de.raindancer.modules.pets.model.Species> all = de.raindancer.modules.pets.store.SpeciesCatalogue
                .read(de.raindancer.modules.pets.store.SpeciesCatalogue.class
                        .getResourceAsStream("/de/raindancer/modules/pets/pets.yml"), problem -> { });
        StringBuilder read = new StringBuilder();
        try (var files = java.nio.file.Files.walk(Path.of("src/main/resources"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".yml")
                    && !p.getFileName().toString().equals("pets.yml")
                    && !p.getFileName().toString().equals("tree.yml")).toList()) {
                read.append(java.nio.file.Files.readString(file)).append('\n');
            }
        }
        // the guide too, which shares the name pets.yml with the catalogue
        read.append(java.nio.file.Files.readString(Path.of("src/main/resources/rains-help/pets.yml")));
        String text = read.toString().toLowerCase(java.util.Locale.ROOT);
        for (de.raindancer.modules.pets.model.Species species : all) {
            if (species.rarity() == de.raindancer.modules.pets.model.Rarity.COMMON) {
                continue;
            }
            String name = species.title().toLowerCase(java.util.Locale.ROOT);
            assertThat(java.util.regex.Pattern.compile("\\b" + java.util.regex.Pattern.quote(name) + "s?\\b")
                    .matcher(text).find()).as(species.title() + " is named where players read").isFalse();
        }
    }
}
