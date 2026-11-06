package de.raindancer.modules.pets;

import de.raindancer.modules.api.ReuseContract;

import java.nio.file.Path;

/** That pets do not rebuild what Core owns — see {@link ReuseContract}. */
class ReuseTest implements ReuseContract {

    @Override
    public Path moduleSource() {
        return Path.of("src/main/java/de/raindancer/modules/pets");
    }
}
