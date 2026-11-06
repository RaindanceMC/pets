package de.raindancer.modules.pets.model;

import org.bukkit.Material;

/**
 * One kind of pet a server offers, as written in {@code pets.yml}.
 *
 * @param entityType the mob, as Bukkit's {@code EntityType} name — kept a string so the model needs no server
 * @param health     at level one, in half hearts
 * @param damage     a hit at level one, in half hearts; a healer's mend
 * @param speed      blocks per tick when following or ridden
 * @param size       how tall it stands, in player heights ({@link #SMALLEST} to {@link #LARGEST}) — a parrot
 *                   is tiny, a dragon the biggest, and every pet smaller than its wild self
 * @param flies      whether it flies when following and when ridden
 * @param baby       whether it is the young version, where the mob has one
 * @param missile    what a ranged pet throws, as Bukkit's {@code EntityType} name
 */
public record Species(String id, String title, String entityType, Role role, Rarity rarity, Material icon,
                      double health, double damage, double speed, double size, boolean flies, boolean baby,
                      Unlock unlock, String blurb, String missile) {

    public static final double SMALLEST = 0.15;
    public static final double LARGEST = 0.6;

    public Species {
        size = Math.max(SMALLEST, Math.min(LARGEST, size));
    }

    public boolean supportersOnly() {
        return unlock.way() == Unlock.Way.SUPPORTER;
    }

    /** The permission that hands this species to a rank, whatever its way. */
    public String permission() {
        return "pets.species." + id;
    }
}
