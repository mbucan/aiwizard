package com.company.aiwizard.view.openaisample;

import com.company.aiwizard.service.OpenAIService;
import com.company.aiwizard.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Sample view demonstrating OpenAI integration in Jmix.
 * Provides a simple interface for entering prompts and displaying AI-generated responses.
 *
 * Accessible at: /openai-sample-view
 * XML descriptor: openai-sample-view.xml
 */
@Route(value = "openai-sample-view", layout = MainView.class)
@ViewController(id = "OpenAISampleView")
@ViewDescriptor(path = "openai-sample-view.xml")
public class OpenAISampleView extends StandardView {

    /** Service for generating AI content via OpenAI API. */
    @Autowired
    private OpenAIService openAIService;

    /** Text area where users enter their prompt. */
    @ViewComponent
    private TextArea promptField;

    /** Text area displaying the AI-generated response. */
    @ViewComponent
    private TextArea responseField;

    /**
     * Handles the Generate button click.
     * Sends the prompt to OpenAI and displays the response.
     *
     * @param event the button click event
     */
    @Subscribe(id = "generateBtn", subject = "clickListener")
    public void onGenerateBtnClick(final ClickEvent<JmixButton> event) {
        String prompt = promptField.getValue();

        // Skip if prompt is empty
        if (prompt == null || prompt.isBlank()) return;

        // Call OpenAI service and display result
        String result = openAIService.generateContent(prompt);
        responseField.setValue(result);
    }

    /**
     * Handles the Clear button click.
     * Clears the prompt field to allow a fresh input.
     *
     * @param event the button click event
     */
    @Subscribe(id = "clearBtn", subject = "clickListener")
    public void onClearBtnClick(final ClickEvent<JmixButton> event) {
        promptField.clear();
    }
}