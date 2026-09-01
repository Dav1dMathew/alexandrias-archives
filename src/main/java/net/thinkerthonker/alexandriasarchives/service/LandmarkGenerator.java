package net.thinkerthonker.alexandriasarchives.service;

import net.thinkerthonker.alexandriasarchives.model.Blueprint;

// This class acts as the common interface for Alexandria's Archives procedural landmark generators.
public interface LandmarkGenerator {
  // Generates Blueprint.
  Blueprint generate();
}
