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
 * Each provider is built manually and conditionally created only if its API key is configured.
 * Spring AI auto-configuration is excluded via application.properties so a missing
 * API key does not break application startup.
 * Required configuration:
 * - spring.ai.openai.api-key: OpenAI API key (for OpenAI provider)
 * - spring.ai.gemini.api-key: Google AI Studio API key (for Gemini provider)
 */
@Configuration
public class AIWizardClientConfiguration {

    @Bean("openAiChatClient")
    @ConditionalOnProperty(name = "spring.ai.openai.api-key")
    public ChatClient openAiChatClient(
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}") String model,
            @Value("${spring.ai.openai.chat.options.temperature:0.1}") Double temperature) {

        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(apiKey)
                .build();

        OpenAiChatModel openAiModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(temperature)
                        .build())
                .build();

        return ChatClient.builder(openAiModel).build();
    }

    /**
     * Google Gemini ChatClient via OpenAI-compatible API.
     * Google AI Studio exposes an OpenAI-compatible endpoint, so the same
     * OpenAI client libraries work by swapping the base URL.
     * Get your key at: https://aistudio.google.com/apikey
     */
    @Bean("geminiChatClient")
    @ConditionalOnProperty(name = "spring.ai.gemini.api-key")
    public ChatClient geminiChatClient(
            @Value("${spring.ai.gemini.api-key}") String apiKey,
            @Value("${spring.ai.gemini.model:gemini-2.0-flash}") String model) {

        OpenAiApi geminiApi = OpenAiApi.builder()
                .apiKey(apiKey)
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/openai")
                .build();

        OpenAiChatModel geminiModel = OpenAiChatModel.builder()
                .openAiApi(geminiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(0.1)
                        .build())
                .build();

        return ChatClient.builder(geminiModel).build();
    }
}
