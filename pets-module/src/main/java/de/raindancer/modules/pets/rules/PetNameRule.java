package de.raindancer.modules.pets.rules;

import java.util.Optional;
import java.util.regex.Pattern;

/** What a pet may be called: letters, digits, spaces and a little punctuation — never markup. */
public final class PetNameRule implements IPetRule {

    public static final int LONGEST = 20;
    private static final Pattern ALLOWED = Pattern.compile("[\\p{L}\\p{N} .'\\-_!?]+");

    public Optional<String> clean(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String name = raw.strip().replaceAll(" {2,}", " ");
        if (name.isEmpty() || name.length() > LONGEST || !ALLOWED.matcher(name).matches()) {
            return Optional.empty();
        }
        return Optional.of(name);
    }

    @Override
    public String describe() {
        return "what a pet may be called";
    }
}
