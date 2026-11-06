package de.raindancer.modules.pets.screen;

import de.raindancer.modules.pets.model.Role;

import java.util.Locale;

/** A skill tree effect's worth, said for a player. */
final class PetSkills {

    private PetSkills() {
    }

    static String describe(String effect, double value) {
        String n = number(value);
        if (effect.startsWith("calling_")) {
            return Role.parse(effect.substring(8)).map(role -> "Makes it a " + role.title() + ": " + role.blurb()
                    .toLowerCase(Locale.ROOT) + ".").orElse(null);
        }
        if (effect.startsWith("aura_")) {
            String what = effect.substring(5).replace('_', ' ');
            return "Keeps " + Character.toUpperCase(what.charAt(0)) + what.substring(1) + " on you, +" + n
                    + " level" + (value == 1 ? "" : "s");
        }
        return switch (effect) {
            case "health" -> "+" + n + "% health";
            case "damage" -> "+" + n + "% damage";
            case "speed" -> "+" + n + "% speed";
            case "attack_speed" -> "+" + n + "% attack speed";
            case "regen" -> "+" + n + "% mending out of fights";
            case "heal" -> "+" + n + "% healing";
            case "xp" -> "+" + n + "% experience";
            case "ability_power" -> "+" + n + "% power of its ability";
            case "ability_haste" -> "Its ability ready " + n + "% sooner";
            case "lifesteal" -> n + "% of its damage mends it";
            case "toughness" -> "Shrugs off " + n + "% of every blow";
            case "thorns" -> n + "% of every blow goes back";
            case "collect" -> "Fetches drops " + n + " blocks further";
            case "spot" -> "Marks monsters " + n + " blocks further";
            default -> null;
        };
    }

    private static String number(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(Math.round(value * 10) / 10.0);
    }
}
