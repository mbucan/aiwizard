package com.company.aiwizard.service;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * Service for interacting with Google's Gemini AI API.
 *
 * Required configuration:
 * - gemini.api-key: Your Google Gemini API key
 * - gemini.model: (Optional) Model name, defaults to gemini-2.5-flash-lite
 */
@Service("aiwizard_GeminiService")
public class GeminiService {

    /** API key injected from gemini.api-key property. */
    @Value("${gemini.api-key}")
    private String apiKey;

    /** Model name, defaults to gemini-2.5-flash-lite. */
    @Value("${gemini.model:gemini-2.5-flash-lite}")
    private String modelName;

    /** Gemini API client, initialized in init(). */
    private Client client;

    /**
     * Initializes the Gemini client after properties are injected.
     * Client remains null if API key is missing or blank.
     */
    @PostConstruct
    private void init() {
        if (apiKey != null && !apiKey.isBlank()) {
            client = Client.builder()
                    .apiKey(apiKey)
                    .build();
        }
    }

    /**
     * Generates content using a simple prompt.
     *
     * @param prompt the user prompt to send to the model
     * @return the generated text response
     * @throws IllegalStateException if client not initialized
     */
    public String generateContent(String prompt) {
        if (client == null) {
            throw new IllegalStateException("Gemini client not initialized - check API key");
        }

        // Pass null for config since no additional options are needed
        GenerateContentResponse response = client.models.generateContent(modelName, prompt, null);
        return response.text();
    }

    /**
     * Generates content with a prompt and system instruction.
     * System instructions guide the model's behavior and response style.
     *
     * @param prompt            the user prompt to send to the model
     * @param systemInstruction behavioral guidelines for the model
     * @return the generated text response
     * @throws IllegalStateException if client not initialized
     */
    public String generateContent(String prompt, String systemInstruction) {
        if (client == null) {
            throw new IllegalStateException("Gemini client not initialized");
        }

        // Wrap the system instruction text in the Content structure required by the API
        Content sysContent = Content.fromParts(Part.fromText(systemInstruction));

        // Build configuration with the system instruction
        GenerateContentConfig config = GenerateContentConfig.builder()
                .systemInstruction(sysContent)
                .build();

        GenerateContentResponse response = client.models.generateContent(modelName, prompt, config);
        return response.text();
    }
}