package net.thinkerthonker.alexandriasarchives.service;

import net.thinkerthonker.alexandriasarchives.model.Blueprint;
import java.util.Locale;

// This class resolves known historical landmarks and acts as a deterministic fallback for the competition demo.
public final class LandmarkRegistry {
  private LandmarkRegistry() {
    // Utility Class.
  }

  // Uses natural language-prompt to generate known landmark.
  public static Blueprint generate(String prompt) {
    if(prompt == null) {
      return null;
    }
    String normalized = prompt.toLowerCase(Locale.ROOT).trim();

    // Aliases for Leaning Tower Of Pisa.
    if(normalized.contains("leaning tower") || normalized.contains("tower of pisa") || normalized.contains("pisa tower")) {
      return LeaningTowerGenerator.generate();
    }
    return null;
  }
}
