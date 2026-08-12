package net.thinkerthonker.alexandriasarchives.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.thinkerthonker.alexandriasarchives.model.Blueprint;
import net.thinkerthonker.alexandriasarchives.model.BlueprintBlock;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

// Talks to the Python AI service's POST /generate endpoint over HTTP.
// Requests run off the server thread; callers must hop back onto the
// server thread (via MinecraftServer#execute) before touching world state
// with the result.
public final class AiServiceClient {

    private static final String DEFAULT_BASE_URL = "http://localhost:8000";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            // Force HTTP/1.1: the default HTTP/2 preference makes the client send
            // an "Upgrade: h2c" cleartext-upgrade header on every request, which
            // uvicorn doesn't support and responds to by dropping the request body
            // (FastAPI then sees a 422 "body: Field required" even though a body
            // was sent).
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(CONNECT_TIMEOUT)
            .build();

    private final String baseUrl;

    public AiServiceClient() {
        this(DEFAULT_BASE_URL);
    }

    public AiServiceClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    // Requests a Blueprint generated from a free-text prompt.
    // @param prompt player-supplied description of the structure to build
    public CompletableFuture<Blueprint> generateBlueprint(String prompt) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("prompt", prompt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/generate"))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(requestBody)))
                .build();

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(AiServiceClient::parseResponse);
    }

    private static Blueprint parseResponse(HttpResponse<String> response) {
        if (response.statusCode() != 200) {
            throw new AiServiceException(
                    "AI service returned HTTP " + response.statusCode() + ": " + response.body());
        }

        BlueprintDto dto;
        try {
            dto = GSON.fromJson(response.body(), BlueprintDto.class);
        } catch (Exception exception) {
            throw new AiServiceException("AI service returned malformed JSON: " + exception.getMessage());
        }

        if (dto == null || dto.blocks == null) {
            throw new AiServiceException("AI service response was missing required fields.");
        }

        List<BlueprintBlock> blocks = new ArrayList<>(dto.blocks.size());
        for (BlueprintBlockDto blockDto : dto.blocks) {
            blocks.add(new BlueprintBlock(
                    new BlockPos(blockDto.x, blockDto.y, blockDto.z),
                    blockDto.blockId
            ));
        }

        return new Blueprint(dto.id, dto.name, dto.width, dto.height, dto.depth, blocks);
    }

    // Mirrors the AI service's JSON Blueprint contract for Gson deserialization.
    private static final class BlueprintDto {
        String id;
        String name;
        int width;
        int height;
        int depth;
        List<BlueprintBlockDto> blocks;
    }

    private static final class BlueprintBlockDto {
        int x;
        int y;
        int z;
        String blockId;
    }

    public static final class AiServiceException extends RuntimeException {
        public AiServiceException(String message) {
            super(message);
        }
    }
}
