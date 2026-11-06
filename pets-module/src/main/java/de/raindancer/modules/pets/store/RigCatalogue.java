package de.raindancer.modules.pets.store;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.raindancer.core.world.companion.Rig;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/** The rigs shipped with the plugin ({@code rigs/<name>.json}, written by {@code art/<name>/rig.py}), read once each. */
public final class RigCatalogue {

    private final Function<String, InputStream> open;
    private final Map<String, Optional<Rig>> read = new ConcurrentHashMap<>();

    public RigCatalogue(Function<String, InputStream> open) {
        this.open = open;
    }

    public Optional<Rig> find(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return read.computeIfAbsent(name.toLowerCase(Locale.ROOT), this::load);
    }

    private Optional<Rig> load(String name) {
        try (InputStream in = open.apply("rigs/" + name + ".json")) {
            if (in == null) {
                return Optional.empty();
            }
            return Optional.of(parse(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .getAsJsonObject()));
        } catch (IOException unreadable) {
            throw new UncheckedIOException(unreadable);
        }
    }

    static Rig parse(JsonObject json) {
        List<Rig.Part> parts = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray("parts")) {
            JsonObject part = element.getAsJsonObject();
            Rig.Motion motion = null;
            if (part.has("motion") && part.get("motion").isJsonObject()) {
                JsonObject moving = part.getAsJsonObject("motion");
                motion = new Rig.Motion(Rig.Motion.Kind.valueOf(moving.get("kind").getAsString().toUpperCase(Locale.ROOT)),
                        moving.has("side") ? moving.get("side").getAsInt() : 1,
                        moving.has("index") ? moving.get("index").getAsInt() : 0);
            }
            parts.add(new Rig.Part(part.get("name").getAsString(),
                    part.has("model") && !part.get("model").isJsonNull() ? part.get("model").getAsString() : null,
                    part.has("parent") && !part.get("parent").isJsonNull() ? part.get("parent").getAsString() : null,
                    floats(part.getAsJsonArray("pivot")), floats(part.getAsJsonArray("rotation")),
                    part.get("modelScale").getAsFloat(), motion));
        }
        return new Rig(json.get("height").getAsFloat(), parts,
                json.has("offset") ? floats(json.getAsJsonArray("offset")) : new float[3]);
    }

    private static float[] floats(JsonArray array) {
        float[] out = new float[3];
        for (int i = 0; i < 3 && i < array.size(); i++) {
            out[i] = array.get(i).getAsFloat();
        }
        return out;
    }
}
