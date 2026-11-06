package de.raindancer.modules.pets.model;

import java.util.Locale;
import java.util.Optional;

/** How rare a species is — the pages of the collection, in order, and the colour its name is shown in. */
public enum Rarity {

    COMMON("Common", "#b0b0b0"),
    UNCOMMON("Uncommon", "#55d46a"),
    RARE("Rare", "#4f9dff"),
    EPIC("Epic", "#b45cff"),
    LEGENDARY("Legendary", "#ffaa2b"),
    MYTHIC("Mythic", "#ff4f7a");

    private final String title;
    private final String colour;

    Rarity(String title, String colour) {
        this.title = title;
        this.colour = colour;
    }

    public String title() {
        return title;
    }

    /** A hex colour, for MiniMessage's {@code <color:…>}. */
    public String colour() {
        return colour;
    }

    public static Optional<Rarity> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(raw.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException unknown) {
            return Optional.empty();
        }
    }
}
