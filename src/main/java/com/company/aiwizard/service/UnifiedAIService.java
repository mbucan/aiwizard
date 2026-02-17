package com.company.aiwizard.service;

import com.company.aiwizard.entity.AIWizardConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * Unified AI service that supports multiple providers via Spring AI.
 * Required configuration (set Gemini at least):
 * - spring.ai.gemini.api-key: Google Gemini API key
 * - spring.ai.openai.api-key: OpenAI API key
 */
@Service("aiwizard_UnifiedAiService")
public class UnifiedAIService {

    private static final Logger log = LoggerFactory.getLogger(UnifiedAIService.class);

    private final Map<AIWizardConnection, ChatClient> chatClients = new EnumMap<>(AIWizardConnection.class);

    @Autowired(required = false)
    @Qualifier("openAiChatClient")
    private ChatClient openAiClient;

    @Autowired(required = false)
    @Qualifier("geminiChatClient")
    private ChatClient geminiClient;

    @PostConstruct
    private void init() {
        if (openAiClient != null) {
            chatClients.put(AIWizardConnection.OPENAI, openAiClient);
            log.info("OpenAI provider registered");
        }
        if (geminiClient != null) {
            chatClients.put(AIWizardConnection.GEMINI, geminiClient);
            log.info("Gemini provider registered");
        }

        if (chatClients.isEmpty()) {
            log.warn("No AI providers configured! Set at least one API key.");
        } else {
            log.info("Available AI providers: {}", chatClients.keySet());
        }
    }

    /**
     * Get list of available (configured) providers.
     */
    public List<AIWizardConnection> getAvailableProviders() {
        return new ArrayList<>(chatClients.keySet());
    }

    /**
     * Check if a specific provider is available.
     */
    public boolean isProviderAvailable(AIWizardConnection provider) {
        return chatClients.containsKey(provider);
    }

    /**
     * Get the default (first available) provider.
     */
    public Optional<AIWizardConnection> getDefaultProvider() {
        return chatClients.keySet().stream().findFirst();
    }

    /**
     * Generates content using a simple prompt.
     *
     * @param prompt   the user prompt to send to the model
     * @param provider the AI provider to use
     * @return the generated text response
     * @throws IllegalStateException if provider not configured
     */
    public String generateContent(String prompt, AIWizardConnection provider) {
        ChatClient client = getClientOrThrow(provider);

        log.debug("Generating content with {}", provider);

        try {
            return client.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("Error generating content with {}: {}", provider, e.getMessage());
            throw new RuntimeException("Failed to generate content: " + e.getMessage(), e);
        }
    }

    /**
     * Generates content with a prompt and system instruction.
     * System instructions guide the model's behavior and response style.
     *
     * @param prompt            the user prompt to send to the model
     * @param systemInstruction behavioral guidelines for the model
     * @param provider          the AI provider to use
     * @return the generated text response
     * @throws IllegalStateException if provider not configured
     */
    public String generateContent(String prompt, String systemInstruction, AIWizardConnection provider) {
        ChatClient client = getClientOrThrow(provider);

        log.debug("Generating content with {} using system instruction", provider);

        try {
            return client.prompt()
                    .system(systemInstruction)
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("Error generating content with {}: {}", provider, e.getMessage());
            throw new RuntimeException("Failed to generate content: " + e.getMessage(), e);
        }
    }

    /**
     * Generates content using the default provider.
     * Convenience method when provider selection is not needed.
     *
     * @param prompt the user prompt
     * @return the generated text response
     */
    public String generateContent(String prompt) {
        AIWizardConnection provider = getDefaultProvider()
                .orElseThrow(() -> new IllegalStateException("No AI providers configured"));
        return generateContent(prompt, provider);
    }

    /**
     * Generates content with system instruction using the default provider.
     *
     * @param prompt            the user prompt
     * @param systemInstruction behavioral guidelines for the model
     * @return the generated text response
     */
    public String generateContent(String prompt, String systemInstruction) {
        AIWizardConnection provider = getDefaultProvider()
                .orElseThrow(() -> new IllegalStateException("No AI providers configured"));
        return generateContent(prompt, systemInstruction, provider);
    }

    private ChatClient getClientOrThrow(AIWizardConnection provider) {
        ChatClient client = chatClients.get(provider);
        if (client == null) {
            throw new IllegalStateException(
                    "Provider " + provider + " is not configured. " +
                            "Available providers: " + getAvailableProviders());
        }
        return client;
    }
}