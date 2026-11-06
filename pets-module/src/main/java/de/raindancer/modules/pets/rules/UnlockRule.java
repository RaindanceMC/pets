package de.raindancer.modules.pets.rules;

import de.raindancer.core.social.economy.Fees;
import de.raindancer.core.social.economy.Money;
import de.raindancer.modules.pets.model.Species;

import java.util.ArrayList;
import java.util.List;

/** Whether a player has, may take, may buy, or may not have a species — and why not. */
public final class UnlockRule implements IPetRule {

    public enum State {
        OWNED, CLAIMABLE, BUYABLE, LOCKED, SUPPORTERS_ONLY, RANK_ONLY
    }

    /**
     * @param granted  holds {@code pets.species.<id>}
     * @param progress done so far towards each part of earning it
     */
    public record Facts(boolean owned, boolean supporter, boolean granted, List<Long> progress) {

        public Facts {
            progress = progress == null ? List.of() : List.copyOf(progress);
        }

        public Facts(boolean owned, boolean supporter, boolean granted, long progress) {
            this(owned, supporter, granted, List.of(progress));
        }
    }

    /** How far one part of earning it is. */
    public record Part(de.raindancer.modules.pets.model.Requirement requirement, long done) {

        public boolean finished() {
            return done >= requirement.count();
        }
    }

    public record Verdict(State state, Money price, List<Part> parts) {

        static Verdict of(State state) {
            return new Verdict(state, Money.ZERO, List.of());
        }

        /** The first part's — for an unlock of one part, the whole of it. */
        public long progress() {
            return parts.isEmpty() ? 0 : parts.getFirst().done();
        }

        public long needed() {
            return parts.isEmpty() ? 0 : parts.getFirst().requirement().count();
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
            case EARN, DROP -> {
                List<Part> parts = new ArrayList<>();
                List<de.raindancer.modules.pets.model.Requirement> needs = species.unlock().needs();
                for (int i = 0; i < needs.size(); i++) {
                    parts.add(new Part(needs.get(i), i < facts.progress().size() ? facts.progress().get(i) : 0));
                }
                yield parts.stream().allMatch(Part::finished) ? Verdict.of(State.CLAIMABLE)
                        : new Verdict(State.LOCKED, Money.ZERO, parts);
            }
            case BUY -> {
                Money price = Fees.amount(species.unlock().price());
                yield price.isPositive() ? new Verdict(State.BUYABLE, price, List.of()) : Verdict.of(State.CLAIMABLE);
            }
        };
    }

    @Override
    public String describe() {
        return "whether a player may have a species";
    }
}
