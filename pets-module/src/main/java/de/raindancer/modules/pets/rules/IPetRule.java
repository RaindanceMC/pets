package de.raindancer.modules.pets.rules;

/**
 * Decides, and does nothing else: nothing saved, sent or scheduled, safe from any thread — a screen asks a rule for
 * every button it draws.
 */
public interface IPetRule {

    /** What it decides, for a diagnostic. */
    String describe();
}
