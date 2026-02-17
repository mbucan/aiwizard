package com.company.aiwizard.service;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * Service for interacting with OpenAI's Chat Completions API.
 *
 * Required configuration:
 * - openai.api-key: Your OpenAI API key
 * - openai.model: (Optional) Model name, defaults to gpt-4o-mini
 */
@Service("aiwizard_OpenAIService")
public class OpenAIService {

    /** API key injected from openai.api-key property. */
    @Value("${openai.api-key}")
    private String apiKey;

    /** Model name, defaults to gpt-4o-mini. */
    @Value("${openai.model:gpt-4o-mini}")
    private String modelName;

    /** OpenAI API client, initialized in init(). */
    private OpenAIClient client;

    /**
     * Initializes the OpenAI client after properties are injected.
     * Client remains null if API key is missing or blank.
     */
    @PostConstruct
    private void init() {
        if (apiKey != null && !apiKey.isBlank()) {
            client = OpenAIOkHttpClient.builder()
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
            throw new IllegalStateException("OpenAI client not initialized - check API key");
        }

        // Build chat completion request with user message only
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .addUserMessage(prompt)
                .model(modelName)
                .build();

        ChatCompletion completion = client.chat().completions().create(params);

        // Extract text from the first choice, or return fallback message
        return completion.choices().stream()
                .findFirst()
                .flatMap(choice -> choice.message().content())
                .orElse("No response generated");
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
            throw new IllegalStateException("OpenAI client not initialized");
        }

        // Build chat completion request with system message first, then user message
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .addSystemMessage(systemInstruction)
                .addUserMessage(prompt)
                .model(modelName)
                .build();

        ChatCompletion completion = client.chat().completions().create(params);

        // Extract text from the first choice, or return fallback message
        return completion.choices().stream()
                .findFirst()
                .flatMap(choice -> choice.message().content())
                .orElse("No response generated");
    }
}