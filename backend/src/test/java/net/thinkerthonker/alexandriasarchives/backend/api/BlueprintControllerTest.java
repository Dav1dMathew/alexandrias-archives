package net.thinkerthonker.alexandriasarchives.backend.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.thinkerthonker.alexandriasarchives.backend.service.AiServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BlueprintController.class)
class BlueprintControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean AiServiceClient aiServiceClient;

    @Test void reportsHealth() throws Exception {
        mockMvc.perform(get("/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test void rejectsBlankPrompt() throws Exception {
        mockMvc.perform(post("/generate").contentType("application/json")
                        .content("{\"prompt\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("prompt must not be blank"));
    }

    @Test void returnsBlueprintFromAiService() throws Exception {
        when(aiServiceClient.generate(any())).thenReturn(objectMapper.readTree("""
                {"schemaVersion":1,"id":"tower","name":"Tower","width":1,"height":1,"depth":1,
                 "blocks":[{"x":0,"y":0,"z":0,"blockId":"minecraft:stone"}],
                 "reasoning":"documented"}
                """));
        mockMvc.perform(post("/generate").contentType("application/json")
                        .content("{\"prompt\":\"a stone tower\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value("tower"))
                .andExpect(jsonPath("$.reasoning").value("documented"));
    }
}
