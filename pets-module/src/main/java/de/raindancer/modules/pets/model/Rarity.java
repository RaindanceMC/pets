package de.raindancer.modules.pets.model;

import java.util.Locale;
import java.util.Optional;

/** How rare a species is — the pages of the collection, in order, and the colour its name is shown in. */
public enum Rarity {

    // Default name gradients are presets of RainsCosmetics: Mint, Ocean, Berry, Gold, Royal.
    COMMON("Common", "#b0b0b0", ""),
    UNCOMMON("Uncommon", "#55d46a", "#00b09b,#96c93d"),
    RARE("Rare", "#4f9dff", "#2193b0,#6dd5ed"),
    EPIC("Epic", "#e0379e", "#8e2de2,#ff0080"),
    LEGENDARY("Legendary", "#fbb40f", "#f7971e,#ffd200|bold"),
    MYTHIC("Mythic", "#7a1fe6", "#8e2de2,#4a00e0|bold");

    private final String title;
    private final String colour;
    private final de.raindancer.core.ui.text.NameStyle nameStyle;

    Rarity(String title, String colour, String nameStyle) {
        this.title = title;
        this.colour = colour;
        this.nameStyle = de.raindancer.core.ui.text.NameStyle.parse(nameStyle);
    }

    /** What a pet of this rarity's name is painted with until its owner paints it themselves. */
    public de.raindancer.core.ui.text.NameStyle nameStyle() {
        return nameStyle;
    }

    /** The owner's paint ({@code NameStyle.encode()}), or this rarity's when they chose none. */
    public de.raindancer.core.ui.text.NameStyle styleFor(String ownPaint) {
        return ownPaint == null || ownPaint.isBlank() ? nameStyle
                : de.raindancer.core.ui.text.NameStyle.parse(ownPaint);
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
