package com.vyra.be_virtual_your_assets.service.ai;

import java.util.Optional;

/** Writes a reflection text from anonymised weekly aggregates. */
public interface AiReflectionClient {

    /** false when no model is configured (no API key): the feature is then hidden. */
    boolean isAvailable();

    /** The text, or empty when the model declined or failed. */
    Optional<Result> generate(String system, String prompt);

    record Result(String text, String model) {
    }
}
