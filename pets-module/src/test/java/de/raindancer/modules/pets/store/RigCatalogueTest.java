package de.raindancer.modules.pets.store;

import de.raindancer.core.world.companion.Rig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("the rigs shipped with the plugin")
class RigCatalogueTest {

    private final RigCatalogue rigs = new RigCatalogue(path ->
            RigCatalogue.class.getResourceAsStream("/de/raindancer/modules/pets/" + path));

    @Test
    @DisplayName("the mini ender dragon reads whole: body, head, a jaw that bites, wings that flap, every model shipped")
    void dragon() {
        Rig dragon = rigs.find("ender_dragon").orElseThrow();
        assertThat(dragon.parts()).extracting(Rig.Part::name)
                .contains("root", "body", "neck_0", "neck_4", "head", "jaw", "left_wing", "left_wing_tip", "right_wing",
                        "tail_0", "tail_11", "left_front_foot", "right_hind_leg_tip");
        assertThat(dragon.parts().get(dragon.indexOf("root")).shown()).isFalse();
        assertThat(dragon.parts().get(dragon.indexOf("tail_11")).motion().index()).isEqualTo(11);
        assertThat(dragon.offset()[2]).isNotZero();
        assertThat(dragon.parts().get(dragon.indexOf("jaw")).motion().kind()).isEqualTo(Rig.Motion.Kind.JAW);
        assertThat(dragon.parts().get(dragon.indexOf("right_wing")).motion().side()).isEqualTo(-1);
        for (Rig.Part part : dragon.parts()) {
            if (!part.shown()) {
                continue;
            }
            String key = part.model().substring(part.model().indexOf(':') + 1);
            assertThat(RigCatalogue.class.getResource("/de/raindancer/modules/pets/pack/assets/rainspets/items/"
                    + key + ".json")).as(part.name()).isNotNull();
            if (part.parent() != null) {
                assertThat(dragon.indexOf(part.parent())).as(part.name()).isBetween(0, dragon.indexOf(part.name()) - 1);
            }
        }
        assertThat(rigs.find("no_such_rig")).isEmpty();
    }
}
