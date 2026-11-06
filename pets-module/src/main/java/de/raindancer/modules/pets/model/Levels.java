package de.raindancer.modules.pets.model;

/** How a pet grows: the experience each level needs, and what a level is worth. */
public record Levels(int maxLevel) {

    /** Gained per level on top of level one's health. */
    public static final double HEALTH_PER_LEVEL = 0.08;
    /** Gained per level on top of level one's damage. */
    public static final double DAMAGE_PER_LEVEL = 0.06;

    public record Progress(int level, long xp, int levelsGained) {
    }

    public Levels {
        maxLevel = Math.max(1, maxLevel);
    }

    /** Experience from {@code level} to the next one. */
    public long toNext(int level) {
        return 100L + 40L * (level - 1) + 6L * (long) (level - 1) * (level - 1);
    }

    public Progress gain(int level, long xp, long gained) {
        if (gained <= 0) {
            return new Progress(level, xp, 0);
        }
        int now = Math.min(level, maxLevel);
        if (now >= maxLevel) {
            return new Progress(maxLevel, 0, 0);
        }
        long held = xp + gained;
        while (now < maxLevel && held >= toNext(now)) {
            held -= toNext(now);
            now++;
        }
        return new Progress(now, now >= maxLevel ? 0 : held, now - level);
    }

    public double health(double base, int level) {
        return base * (1 + HEALTH_PER_LEVEL * (clamp(level) - 1));
    }

    public double damage(double base, int level) {
        return base * (1 + DAMAGE_PER_LEVEL * (clamp(level) - 1));
    }

    /** Half hearts a second it mends out of fights: to full in a minute at level one, 35 seconds at the top. */
    public double regenPerSecond(double maxHealth, int level) {
        double grown = maxLevel <= 1 ? 0 : (clamp(level) - 1) / (double) (maxLevel - 1);
        return maxHealth / (60 - 25 * grown);
    }

    private int clamp(int level) {
        return Math.max(1, Math.min(maxLevel, level));
    }
}
