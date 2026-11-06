package de.raindancer.modules.pets.screen;

import de.raindancer.core.social.economy.Fees;
import de.raindancer.core.ui.menu.Icons;
import de.raindancer.core.ui.menu.Menu;
import de.raindancer.core.ui.menu.PaginatedMenu;
import de.raindancer.modules.pets.PetServices;
import de.raindancer.modules.pets.model.Species;
import de.raindancer.modules.pets.rules.UnlockRule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Every species, rarest last: what each is and how to get it. Anything above common that a player has never seen out
 * with somebody stays a question mark — its place shown, its secret kept.
 */
public final class SpeciesMenu extends PaginatedMenu<Species> implements IPetScreen {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final PetServices services;

    public SpeciesMenu(PetServices services, Player viewer, Menu parent) {
        super(viewer, services.brand(), parent);
        this.services = services;
    }

    @Override
    protected Component title() {
        return MINI.deserialize("<dark_gray>Every species");
    }

    @Override
    public String breadcrumb() {
        return "Every species";
    }

    @Override
    protected List<Species> entries() {
        List<Species> all = new ArrayList<>(services.collection().catalogue().all());
        all.sort(Comparator.comparing(Species::rarity));
        return all;
    }

    @Override
    protected ItemStack emptyIcon() {
        return Icons.of(Material.BARRIER, "<gray>No species", "<gray>This server offers no pets right now.");
    }

    @Override
    protected ItemStack icon(Species species) {
        if (!services.collection().knows(viewer, species)) {
            return Icons.of(Material.GRAY_DYE, PetLook.rarity(species.rarity()) + " <gray>???",
                    "<gray>A " + species.rarity().title().toLowerCase(Locale.ROOT) + " pet nobody has shown you yet.",
                    "<dark_gray>See one out with somebody to learn what it is.");
        }
        UnlockRule.Verdict verdict = services.collection().verdict(viewer, species);
        List<String> lore = new ArrayList<>();
        lore.add(PetLook.rarity(species.rarity()) + " <dark_gray>· <gray>" + species.role().title()
                + (species.flies() ? " <dark_gray>· <gray>flies" : ""));
        if (!species.blurb().isBlank()) {
            lore.add("<gray>" + species.blurb());
        }
        lore.add("");
        switch (verdict.state()) {
            case OWNED -> lore.add("<green>Yours already.");
            case CLAIMABLE -> lore.add("<yellow>Click<gray> to take it.");
            case BUYABLE -> lore.add("<yellow>Click<gray> to buy it for <white>" + Fees.format(verdict.price()) + "<gray>.");
            case LOCKED -> {
                lore.add("<gray>Earned by " + (verdict.parts().size() > 1 ? "all of these" : "") + ":");
                for (UnlockRule.Part part : verdict.parts()) {
                    long shown = Math.min(part.done(), part.requirement().count());
                    lore.add((part.finished() ? "<green>✔ " : "<gray>• ") + earning(part.requirement()) + " "
                            + PetLook.bar(shown, part.requirement().count()) + " <gray>" + shown + "/"
                            + part.requirement().count());
                }
            }
            case SUPPORTERS_ONLY -> lore.add("<light_purple>A thank-you for the server's supporters — /support");
            case RANK_ONLY -> lore.add("<gray>Given by a rank.");
        }
        return Icons.of(species.icon(), "<color:" + species.rarity().colour() + ">" + species.title(), lore);
    }

    private static String earning(de.raindancer.modules.pets.model.Requirement unlock) {
        String what = unlock.what().isEmpty() ? "" : " " + unlock.what().toLowerCase(Locale.ROOT).replace('_', ' ');
        return switch (unlock.activity()) {
            case MONSTER -> "slaying" + (what.isEmpty() ? " monsters" : what + "s");
            case BOSS -> "slaying" + (what.isEmpty() ? " bosses" : " the" + what);
            case FISH -> "fishing";
            case BREED -> "breeding animals";
            case HARVEST -> "harvesting crops";
            case MINE -> "mining";
            case DIG -> "digging";
            case EXCAVATE -> "excavating";
            case CHOP -> "chopping trees";
            case FORAGE -> "foraging";
            case TRADE -> "trading with villagers";
            case TRAVEL -> "travelling (blocks)";
            case EXPLORE -> "exploring new places";
            case BUILD -> "building";
            case RAID -> "winning raids";
            default -> unlock.activity().name().toLowerCase(Locale.ROOT);
        };
    }

    @Override
    protected void onClick(Species species, InventoryClickEvent event) {
        if (!services.collection().knows(viewer, species)) {
            services.messages().send(viewer, "pets.unknown-yet");
            return;
        }
        UnlockRule.Verdict verdict = services.collection().verdict(viewer, species);
        if (verdict.state() == UnlockRule.State.BUYABLE) {
            new ConfirmScreen(services, viewer, this, "Buy the " + species.title() + "?",
                    List.of("<gray>Costs <white>" + Fees.format(verdict.price()) + "<gray>.",
                            "<gray>It is yours for good."),
                    () -> {
                        services.care().adopted(viewer, services.collection().buy(viewer, species), species);
                        open();
                    }).open();
            return;
        }
        services.care().adopted(viewer, services.collection().claim(viewer, species), species);
        refresh();
    }
}
