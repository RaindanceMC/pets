package de.raindancer.modules.pets.screen;

import de.raindancer.core.ui.menu.ConfirmMenu;
import de.raindancer.core.ui.menu.Menu;
import de.raindancer.modules.pets.PetServices;
import org.bukkit.entity.Player;

import java.util.List;

/** "Are you sure?" — Core's dialog, under this module's name. */
public final class ConfirmScreen extends ConfirmMenu implements IPetScreen {

    public ConfirmScreen(PetServices services, Player viewer, Menu parent, String question,
                         List<String> consequences, Runnable onYes) {
        super(viewer, services.brand(), parent, question, consequences, onYes);
    }
}
