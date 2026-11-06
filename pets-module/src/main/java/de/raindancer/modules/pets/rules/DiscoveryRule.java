package de.raindancer.modules.pets.rules;

import de.raindancer.modules.pets.model.Rarity;

/** Whether a player knows a species exists: common ones always, rarer ones once seen out with somebody, owned or open to them. */
public final class DiscoveryRule implements IPetRule {

    /** @param open they could take it now (earned, granted, a supporters' one for a supporter) */
    public record Facts(boolean seen, boolean owned, boolean open) {
    }

    public boolean known(Rarity rarity, Facts facts) {
        return rarity == Rarity.COMMON || facts.seen() || facts.owned() || facts.open();
    }

    @Override
    public String describe() {
        return "which species a player knows of";
    }
}
