package net.thinkerthonker.alexandriasarchives.backend.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GenerateRequest(
        @NotBlank(message = "prompt must not be blank")
        @Size(max = 1_000, message = "prompt must be at most 1000 characters")
        String prompt
) {}
