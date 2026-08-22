package net.thinkerthonker.alexandriasarchives.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import net.thinkerthonker.alexandriasarchives.backend.api.GenerateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiServiceClientTest {
    private MockRestServiceServer server;
    private AiServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AiServiceClient(builder, "http://ai.test");
    }

    @Test
    void acceptsBlueprintV1() {
        server.expect(requestTo("http://ai.test/generate"))
                .andExpect(method(POST))
                .andRespond(withSuccess(validBlueprint(), MediaType.APPLICATION_JSON));

        JsonNode response = client.generate(new GenerateRequest("a stone tower"));

        assertEquals(1, response.path("schemaVersion").intValue());
        assertEquals("tower", response.path("id").textValue());
        server.verify();
    }

    @Test
    void rejectsBlueprintWithoutSchemaVersion() {
        server.expect(requestTo("http://ai.test/generate"))
                .andRespond(withSuccess(validBlueprint().replace("\"schemaVersion\":1,", ""),
                        MediaType.APPLICATION_JSON));

        assertThrows(
                InvalidAiResponseException.class,
                () -> client.generate(new GenerateRequest("a stone tower"))
        );
        server.verify();
    }

    @Test
    void rejectsDuplicateBlockCoordinates() {
        String duplicate = validBlueprint().replace(
                "]}",
                ",{\"x\":0,\"y\":0,\"z\":0,\"blockId\":\"minecraft:glass\"}]}"
        );
        server.expect(requestTo("http://ai.test/generate"))
                .andRespond(withSuccess(duplicate, MediaType.APPLICATION_JSON));

        assertThrows(
                InvalidAiResponseException.class,
                () -> client.generate(new GenerateRequest("a stone tower"))
        );
        server.verify();
    }

    private static String validBlueprint() {
        return """
                {"schemaVersion":1,"id":"tower","name":"Tower","width":1,"height":1,"depth":1,
                 "blocks":[{"x":0,"y":0,"z":0,"blockId":"minecraft:stone"}]}
                """;
    }
}
