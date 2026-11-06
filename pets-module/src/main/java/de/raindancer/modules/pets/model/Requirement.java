package de.raindancer.modules.pets.model;

import de.raindancer.core.world.activity.ActivityKind;

import java.util.Locale;

/**
 * One thing a player has to do often enough to earn a species.
 *
 * @param what only this one thing counts (a mob, a block), upper case; empty for anything of that kind
 */
public record Requirement(ActivityKind activity, String what, long count) {

    public Requirement {
        what = what == null ? "" : what.trim().toUpperCase(Locale.ROOT);
        count = Math.max(1, count);
    }

    public boolean counts(ActivityKind kind, String thing) {
        return activity == kind
                && (what.isEmpty() || (thing != null && what.equals(thing.trim().toUpperCase(Locale.ROOT))));
    }
}
