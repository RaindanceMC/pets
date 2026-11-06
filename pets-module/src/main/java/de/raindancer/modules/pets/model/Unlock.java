package de.raindancer.modules.pets.model;

import de.raindancer.core.world.activity.ActivityKind;

import java.util.Locale;

/**
 * How a species is had.
 *
 * @param activity for {@link Way#EARN}: what counts
 * @param what     for {@link Way#EARN}: only this one thing (a mob, a block) counts, upper case; empty for anything
 * @param count    for {@link Way#EARN}: how many times
 * @param price    for {@link Way#BUY}: money as written, like {@code 2.5k}
 */
public record Unlock(Way way, ActivityKind activity, String what, long count, String price) {

    public enum Way {
        /** Anybody may take it. */
        FREE,
        /** Taken once a player did something often enough. */
        EARN,
        /** Bought for money. */
        BUY,
        /** Only given by a rank: {@code pets.species.<id>}. */
        PERMISSION,
        /** Kept for the server's supporters. */
        SUPPORTER
    }

    public Unlock {
        what = what == null ? "" : what.trim().toUpperCase(Locale.ROOT);
        price = price == null ? "" : price.trim();
        count = Math.max(0, count);
    }

    public static Unlock free() {
        return new Unlock(Way.FREE, null, "", 0, "");
    }

    public static Unlock earn(ActivityKind activity, long count) {
        return new Unlock(Way.EARN, activity, "", count, "");
    }

    public static Unlock earn(ActivityKind activity, String what, long count) {
        return new Unlock(Way.EARN, activity, what, count, "");
    }

    public static Unlock buy(String price) {
        return new Unlock(Way.BUY, null, "", 0, price);
    }

    public static Unlock permission() {
        return new Unlock(Way.PERMISSION, null, "", 0, "");
    }

    public static Unlock supporter() {
        return new Unlock(Way.SUPPORTER, null, "", 0, "");
    }

    /** Whether doing this brings a player closer to earning it. */
    public boolean counts(ActivityKind kind, String thing) {
        if (way != Way.EARN || activity != kind) {
            return false;
        }
        return what.isEmpty() || (thing != null && what.equals(thing.trim().toUpperCase(Locale.ROOT)));
    }
}
