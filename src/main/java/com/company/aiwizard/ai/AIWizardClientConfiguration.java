package com.company.aiwizard.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for AI provider ChatClient beans.
 * Each provider is conditionally created only if its API key is configured.
 *
 * Required configuration in application.properties:
 * - spring.ai.openai.api-key: OpenAI API key (for OpenAI provider)
 * - spring.ai.gemini.api-key: Google AI Studio API key (for Gemini provider)
 * - spring.ai.gemini.model: (Optional) Gemini model name, defaults to gemini-2.0-flash
 */
@Configuration
public class AIWizardClientConfiguration {

    /**
     * OpenAI ChatClient - created if spring.ai.openai.api-key is set.
     * Uses auto-configured OpenAiChatModel from Spring AI starter.
     *
     * @param openAiChatModel auto-configured model from spring-ai-starter-model-openai
     * @return ChatClient configured for OpenAI
     */
    @Bean("openAiChatClient")
    @ConditionalOnProperty(name = "spring.ai.openai.api-key")
    public ChatClient openAiChatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel).build();
    }

    /**
     * Google Gemini ChatClient via OpenAI-compatible API.
     * Uses Google AI Studio API key (not Vertex AI).
     * Get your key at: https://aistudio.google.com/apikey
     *
     * This works because Google provides an OpenAI-compatible endpoint,
     * allowing us to reuse the OpenAI client infrastructure.
     *
     * @param apiKey Gemini API key from spring.ai.gemini.api-key
     * @param model  Gemini model name, defaults to gemini-2.0-flash
     * @return ChatClient configured for Google Gemini
     */
    @Bean("geminiChatClient")
    @ConditionalOnProperty(name = "spring.ai.gemini.api-key")
    public ChatClient geminiChatClient(
            @Value("${spring.ai.gemini.api-key}") String apiKey,
            @Value("${spring.ai.gemini.model:gemini-2.0-flash}") String model) {

        // Configure OpenAI API to point to Gemini's OpenAI-compatible endpoint
        OpenAiApi geminiApi = OpenAiApi.builder()
                .apiKey(apiKey)
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/openai")
                .build();

        // Build chat model with Gemini-specific options
        OpenAiChatModel geminiModel = OpenAiChatModel.builder()
                .openAiApi(geminiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(0.7)
                        .build())
                .build();

        return ChatClient.builder(geminiModel).build();
    }
}