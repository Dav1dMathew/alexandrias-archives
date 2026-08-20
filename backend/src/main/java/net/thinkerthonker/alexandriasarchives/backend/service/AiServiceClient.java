package net.thinkerthonker.alexandriasarchives.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import net.thinkerthonker.alexandriasarchives.backend.api.GenerateRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
public class AiServiceClient {
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
            if (blueprint == null || !blueprint.isObject() || !blueprint.hasNonNull("blocks")) {
                throw new InvalidAiResponseException("AI service returned an invalid blueprint");
            }
            return blueprint;
        } catch (RestClientResponseException exception) {
            throw new AiServiceUnavailableException(
                    "AI service returned HTTP " + exception.getStatusCode().value(), exception);
        } catch (RestClientException exception) {
            throw new AiServiceUnavailableException("AI service is unavailable", exception);
        }
    }
}
