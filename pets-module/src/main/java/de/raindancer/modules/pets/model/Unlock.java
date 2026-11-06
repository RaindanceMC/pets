package de.raindancer.modules.pets.model;

import de.raindancer.core.world.activity.ActivityKind;

import java.util.ArrayList;
import java.util.List;

/**
 * How a species is had.
 *
 * @param needs for {@link Way#EARN}: every one of these, done often enough
 * @param price  for {@link Way#BUY}: money as written, like {@code 2.5k}
 * @param chance for {@link Way#DROP}: how likely one kill lets it fall, above 0 and at most 1
 */
public record Unlock(Way way, List<Requirement> needs, String price, double chance) {

    public enum Way {
        /** Anybody may take it. */
        FREE,
        /** Taken once a player did everything it asks often enough. */
        EARN,
        /** Bought for money. */
        BUY,
        /** Only given by a rank: {@code pets.species.<id>}. */
        PERMISSION,
        /** Kept for the server's supporters. */
        SUPPORTER,
        /** Now and then let fall by a mob a player slays: its one need is that mob, its chance how rarely. */
        DROP
    }

    public Unlock {
        needs = needs == null ? List.of() : List.copyOf(needs);
        price = price == null ? "" : price.trim();
    }

    public Unlock(Way way, List<Requirement> needs, String price) {
        this(way, needs, price, 0);
    }

    public static Unlock free() {
        return new Unlock(Way.FREE, List.of(), "");
    }

    public static Unlock earn(ActivityKind activity, long count) {
        return earnAll(List.of(new Requirement(activity, "", count)));
    }

    public static Unlock earn(ActivityKind activity, String what, long count) {
        return earnAll(List.of(new Requirement(activity, what, count)));
    }

    public static Unlock earnAll(List<Requirement> needs) {
        return new Unlock(Way.EARN, needs, "");
    }

    public static Unlock drop(ActivityKind activity, String what, double chance) {
        return new Unlock(Way.DROP, List.of(new Requirement(activity, what, 1)), "", chance);
    }

    /** Whether this roll (0 inclusive to 1 exclusive) of one kill lets it fall. */
    public boolean drops(double roll) {
        return way == Way.DROP && roll < chance;
    }

    public static Unlock buy(String price) {
        return new Unlock(Way.BUY, List.of(), price);
    }

    public static Unlock permission() {
        return new Unlock(Way.PERMISSION, List.of(), "");
    }

    public static Unlock supporter() {
        return new Unlock(Way.SUPPORTER, List.of(), "");
    }

    /** The first part's — for an unlock of one part, the whole of it. */
    public ActivityKind activity() {
        return needs.isEmpty() ? null : needs.getFirst().activity();
    }

    public String what() {
        return needs.isEmpty() ? "" : needs.getFirst().what();
    }

    public long count() {
        return needs.isEmpty() ? 0 : needs.getFirst().count();
    }

    /** Whether doing this brings a player closer to earning it. */
    public boolean counts(ActivityKind kind, String thing) {
        return !matching(kind, thing).isEmpty();
    }

    /** The parts doing this counts towards. */
    public List<Integer> matching(ActivityKind kind, String thing) {
        List<Integer> parts = new ArrayList<>();
        if (way != Way.EARN && way != Way.DROP) {
            return parts;
        }
        for (int i = 0; i < needs.size(); i++) {
            if (needs.get(i).counts(kind, thing)) {
                parts.add(i);
            }
        }
        return parts;
    }

    /** Where this unlock keeps a part's progress — a drop apart, so kills once counted towards earning open nothing. */
    public String keyFor(String species, int part) {
        return way == Way.DROP ? species + "#dropped" : progressKey(species, part);
    }

    /** Where a part's progress is kept: the species itself for the first (as before parts existed). */
    public static String progressKey(String species, int part) {
        return part == 0 ? species : species + "#" + part;
    }
}
