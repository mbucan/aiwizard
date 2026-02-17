package com.company.aiwizard.view.unifiedaisample;

import com.company.aiwizard.entity.AIWizardConnection;
import com.company.aiwizard.service.UnifiedAIService;
import com.company.aiwizard.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.Route;
import io.jmix.core.Messages;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Unified AI Sample View demonstrating Spring AI integration in Jmix.
 * Provides a single interface for interacting with multiple AI providers (OpenAI, Gemini).
 *
 * Features:
 * - Provider selection dropdown (only shows configured providers)
 * - Optional system instruction for guiding AI behavior
 * - Prompt input and response display
 */
@Route(value = "unified-ai-sample-view", layout = MainView.class)
@ViewController(id = "UnifiedAISampleView")
@ViewDescriptor(path = "unified-ai-sample-view.xml")
public class UnifiedAISampleView extends StandardView {

    /** Service for generating AI content via multiple providers. */
    @Autowired
    private UnifiedAIService unifiedAiService;

    /** Jmix Messages service for enum localization. */
    @Autowired
    private Messages messages;

    /** Dropdown for selecting the AI provider (OpenAI, Gemini). */
    @ViewComponent
    private ComboBox<AIWizardConnection> providerSelect;

    /** Optional system instruction to guide AI behavior and response style. */
    @ViewComponent
    private TextArea systemInstructionField;

    /** Text area where users enter their prompt. */
    @ViewComponent
    private TextArea promptField;

    /** Text area displaying the AI-generated response. */
    @ViewComponent
    private TextArea responseField;

    /** Button to trigger content generation. */
    @ViewComponent
    private JmixButton generateBtn;

    /**
     * Initializes the view on load.
     * Populates the provider dropdown with only configured (available) providers.
     *
     * @param event the init event
     */
    @Subscribe
    public void onInit(final InitEvent event) {
        // Load only available (configured) providers
        List<AIWizardConnection> availableProviders = unifiedAiService.getAvailableProviders();

        // Show error if no providers are configured
        if (availableProviders.isEmpty()) {
            showError("No AI providers configured! Please set API keys in application.properties");
            generateBtn.setEnabled(false);
            return;
        }

        // Populate dropdown with available providers
        providerSelect.setItems(availableProviders);

        // Use Jmix Messages for localized enum display names
        providerSelect.setItemLabelGenerator(item -> messages.getMessage(item));

        // Set default to first available provider
        unifiedAiService.getDefaultProvider()
                .ifPresent(providerSelect::setValue);
    }

    /**
     * Handles the Generate button click.
     * Validates input, calls the AI service, and displays the response.
     *
     * @param event the button click event
     */
    @Subscribe(id = "generateBtn", subject = "clickListener")
    public void onGenerateBtnClick(final ClickEvent<JmixButton> event) {
        String prompt = promptField.getValue();
        String systemInstruction = systemInstructionField.getValue();
        AIWizardConnection provider = providerSelect.getValue();

        // Validate prompt is not empty
        if (prompt == null || prompt.isBlank()) {
            showWarning("Please enter a prompt");
            return;
        }

        // Validate provider is selected
        if (provider == null) {
            showWarning("Please select an AI provider");
            return;
        }

        // Disable button and show loading state
        generateBtn.setEnabled(false);
        responseField.setValue("Generating response with " + messages.getMessage(provider) + "...");

        try {
            // Call AI service with or without system instruction
            String result;
            if (systemInstruction != null && !systemInstruction.isBlank()) {
                result = unifiedAiService.generateContent(prompt, systemInstruction, provider);
            } else {
                result = unifiedAiService.generateContent(prompt, provider);
            }
            responseField.setValue(result);
        } catch (Exception e) {
            // Display error in response field and show notification
            responseField.setValue("Error: " + e.getMessage());
            showError("Generation failed: " + e.getMessage());
        } finally {
            // Re-enable button regardless of success/failure
            generateBtn.setEnabled(true);
        }
    }

    /**
     * Handles the Clear button click.
     * Clears the prompt and response fields, keeps system instruction.
     *
     * @param event the button click event
     */
    @Subscribe(id = "clearBtn", subject = "clickListener")
    public void onClearBtnClick(final ClickEvent<JmixButton> event) {
        promptField.clear();
        responseField.clear();
    }

    /**
     * Handles the Clear All button click.
     * Clears all input fields including system instruction.
     *
     * @param event the button click event
     */
    @Subscribe(id = "clearAllBtn", subject = "clickListener")
    public void onClearAllBtnClick(final ClickEvent<JmixButton> event) {
        systemInstructionField.clear();
        promptField.clear();
        responseField.clear();
    }

    /**
     * Shows a warning notification.
     *
     * @param message the warning message to display
     */
    private void showWarning(String message) {
        Notification.show(message, 3000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_WARNING);
    }

    /**
     * Shows an error notification.
     *
     * @param message the error message to display
     */
    private void showError(String message) {
        Notification.show(message, 5000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}