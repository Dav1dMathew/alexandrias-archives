package net.thinkerthonker.alexandriasarchives.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import net.thinkerthonker.alexandriasarchives.backend.api.GenerateRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class AiServiceClient {
    private static final int SUPPORTED_SCHEMA_VERSION = 1;
    private static final int MAX_DIMENSION = 64;
    private static final int MAX_BLOCKS = 50_000;
    private static final Pattern BLOCK_ID_PATTERN = Pattern.compile(
            "^[a-z0-9_.-]+:[a-z0-9_./-]+$"
    );

    private final RestClient restClient;

    public AiServiceClient(RestClient.Builder builder,
                           @Value("${ai-service.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public JsonNode generate(GenerateRequest request) {
        try {
            JsonNode blueprint = restClient.post()
                    .uri("/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);
            validateBlueprint(blueprint);
            return blueprint;
        } catch (RestClientResponseException exception) {
            throw new AiServiceUnavailableException(
                    "AI service returned HTTP " + exception.getStatusCode().value(), exception);
        } catch (RestClientException exception) {
            throw new AiServiceUnavailableException("AI service is unavailable", exception);
        }
    }

    private static void validateBlueprint(JsonNode blueprint) {
        if (blueprint == null || !blueprint.isObject()) {
            invalidBlueprint();
        }
        if (!blueprint.path("schemaVersion").isIntegralNumber()
                || blueprint.path("schemaVersion").intValue() != SUPPORTED_SCHEMA_VERSION) {
            throw new InvalidAiResponseException("AI service returned an unsupported Blueprint schema");
        }
        requireText(blueprint, "id", 128);
        requireText(blueprint, "name", 256);

        int width = requireDimension(blueprint, "width");
        int height = requireDimension(blueprint, "height");
        int depth = requireDimension(blueprint, "depth");
        JsonNode blocks = blueprint.path("blocks");
        if (!blocks.isArray() || blocks.size() > MAX_BLOCKS) {
            invalidBlueprint();
        }

        Set<String> occupied = new HashSet<>();
        for (JsonNode block : blocks) {
            if (!block.isObject()
                    || !block.path("x").isIntegralNumber()
                    || !block.path("y").isIntegralNumber()
                    || !block.path("z").isIntegralNumber()
                    || !block.path("blockId").isTextual()) {
                invalidBlueprint();
            }
            int x = block.path("x").intValue();
            int y = block.path("y").intValue();
            int z = block.path("z").intValue();
            if (x < 0 || x >= width || y < 0 || y >= height || z < 0 || z >= depth
                    || !BLOCK_ID_PATTERN.matcher(block.path("blockId").textValue()).matches()
                    || !occupied.add(x + ":" + y + ":" + z)) {
                invalidBlueprint();
            }
        }
    }

    private static int requireDimension(JsonNode blueprint, String field) {
        JsonNode value = blueprint.path(field);
        if (!value.isIntegralNumber() || value.intValue() < 1 || value.intValue() > MAX_DIMENSION) {
            invalidBlueprint();
        }
        return value.intValue();
    }

    private static void requireText(JsonNode blueprint, String field, int maximumLength) {
        JsonNode value = blueprint.path(field);
        if (!value.isTextual() || value.textValue().isBlank()
                || value.textValue().length() > maximumLength) {
            invalidBlueprint();
        }
    }

    private static void invalidBlueprint() {
        throw new InvalidAiResponseException("AI service returned an invalid Blueprint");
    }
}
