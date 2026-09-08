package net.thinkerthonker.alexandriasarchives.service;

import net.thinkerthonker.alexandriasarchives.model.Blueprint;

import java.util.List;
import java.util.Locale;

// Resolves a free-text prompt to one of the curated landmark models.
//
// The match is deliberately loose: the prompt does not have to be a command,
// it only has to mention the building. "build me the eiffel tower at night",
// "eiffel", and "/aa build La Tour Eiffel" all resolve to the same model.
// Anything that matches nothing falls through to the AI service.
public final class LandmarkRegistry {

    // Longer, more specific aliases are listed first so that a prompt naming
    // two things ("a tower like the leaning tower of pisa") locks onto the
    // most specific match rather than the first loose keyword.
    private record Landmark(String id, String display, List<String> aliases) {
    }

    private static final List<Landmark> LANDMARKS = List.of(
            new Landmark("leaning_tower_of_pisa", "Leaning Tower of Pisa",
                    List.of("leaning tower of pisa", "leaning tower", "tower of pisa",
                            "pisa tower", "torre di pisa", "pisa")),
            new Landmark("burj_khalifa", "Burj Khalifa",
                    List.of("burj khalifa", "burj kalifa", "burj al khalifa",
                            "khalifa tower", "burj", "khalifa")),
            new Landmark("eiffel_tower", "Eiffel Tower",
                    List.of("eiffel tower", "la tour eiffel", "tour eiffel", "eiffel")),
            new Landmark("concorde", "Concorde",
                    List.of("concorde", "concord"))
    );

    private LandmarkRegistry() {
        // Utility class.
    }

    /** The landmark id a prompt names, or null if it names none of them. */
    public static String match(String prompt) {
        if (prompt == null) {
            return null;
        }
        String normalized = normalize(prompt);
        if (normalized.isEmpty()) {
            return null;
        }

        String bestId = null;
        int bestLength = 0;
        for (Landmark landmark : LANDMARKS) {
            for (String alias : landmark.aliases()) {
                if (alias.length() > bestLength && normalized.contains(alias)) {
                    bestId = landmark.id();
                    bestLength = alias.length();
                }
            }
        }
        return bestId;
    }

    /** Display name for a landmark id, or the id itself if unknown. */
    public static String displayName(String landmarkId) {
        for (Landmark landmark : LANDMARKS) {
            if (landmark.id().equals(landmarkId)) {
                return landmark.display();
            }
        }
        return landmarkId;
    }

    /** Resolves and loads in one step; null when the prompt names no landmark. */
    public static Blueprint generate(String prompt) {
        String landmarkId = match(prompt);
        return landmarkId == null ? null : LandmarkLibrary.load(landmarkId);
    }

    // Lower-cases and reduces every run of non-letters to a single space, so
    // punctuation and spacing in the prompt cannot break a match.
    private static String normalize(String prompt) {
        String lower = prompt.toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(lower.length());
        boolean lastWasSpace = true;
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (c >= 'a' && c <= 'z') {
                out.append(c);
                lastWasSpace = false;
            } else if (!lastWasSpace) {
                out.append(' ');
                lastWasSpace = true;
            }
        }
        return out.toString().trim();
    }
}
