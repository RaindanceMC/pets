package de.raindancer.modules.pets.rules;

import de.raindancer.core.social.economy.Fees;
import de.raindancer.core.social.economy.Money;
import de.raindancer.modules.pets.model.Species;

/** Whether a player has, may take, may buy, or may not have a species — and why not. */
public final class UnlockRule implements IPetRule {

    public enum State {
        OWNED, CLAIMABLE, BUYABLE, LOCKED, SUPPORTERS_ONLY, RANK_ONLY
    }

    /**
     * @param granted  holds {@code pets.species.<id>}
     * @param progress done so far towards earning it
     */
    public record Facts(boolean owned, boolean supporter, boolean granted, long progress) {
    }

    public record Verdict(State state, Money price, long progress, long needed) {

        static Verdict of(State state) {
            return new Verdict(state, Money.ZERO, 0, 0);
        }
    }

    public Verdict judge(Species species, Facts facts) {
        if (facts.owned()) {
            return Verdict.of(State.OWNED);
        }
        if (facts.granted()) {
            return Verdict.of(State.CLAIMABLE);
        }
        return switch (species.unlock().way()) {
            case FREE -> Verdict.of(State.CLAIMABLE);
            case SUPPORTER -> Verdict.of(facts.supporter() ? State.CLAIMABLE : State.SUPPORTERS_ONLY);
            case PERMISSION -> Verdict.of(State.RANK_ONLY);
            case EARN -> {
                long needed = species.unlock().count();
                yield facts.progress() >= needed ? Verdict.of(State.CLAIMABLE)
                        : new Verdict(State.LOCKED, Money.ZERO, facts.progress(), needed);
            }
            case BUY -> {
                Money price = Fees.amount(species.unlock().price());
                yield price.isPositive() ? new Verdict(State.BUYABLE, price, 0, 0) : Verdict.of(State.CLAIMABLE);
            }
        };
    }

    @Override
    public String describe() {
        return "whether a player may have a species";
    }
}
