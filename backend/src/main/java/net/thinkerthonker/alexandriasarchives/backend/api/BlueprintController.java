package net.thinkerthonker.alexandriasarchives.backend.api;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import net.thinkerthonker.alexandriasarchives.backend.service.AiServiceClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class BlueprintController {
    private final AiServiceClient aiServiceClient;

    public BlueprintController(AiServiceClient aiServiceClient) {
        this.aiServiceClient = aiServiceClient;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @PostMapping(path = "/generate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public JsonNode generate(@Valid @RequestBody GenerateRequest request) {
        return aiServiceClient.generate(request);
    }
}
