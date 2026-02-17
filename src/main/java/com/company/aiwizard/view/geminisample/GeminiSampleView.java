package com.company.aiwizard.view.geminisample;

import com.company.aiwizard.service.GeminiService;
import com.company.aiwizard.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Sample view demonstrating Gemini AI integration in Jmix.
 * Provides a simple interface for entering prompts and displaying AI-generated responses.
 *
 * Accessible at: /gemini-sample-view
 * XML descriptor: gemini-sample-view.xml
 */
@Route(value = "gemini-sample-view", layout = MainView.class)
@ViewController(id = "GeminiSampleView")
@ViewDescriptor(path = "gemini-sample-view.xml")
public class GeminiSampleView extends StandardView {

    /** Service for generating AI content via Google Gemini API. */
    @Autowired
    private GeminiService geminiService;

    /** Text area where users enter their prompt. */
    @ViewComponent
    private TextArea promptField;

    /** Text area displaying the AI-generated response. */
    @ViewComponent
    private TextArea responseField;

    /**
     * Handles the Generate button click.
     * Sends the prompt to Gemini and displays the response.
     *
     * @param event the button click event
     */
    @Subscribe(id = "generateBtn", subject = "clickListener")
    public void onGenerateBtnClick1(final ClickEvent<JmixButton> event) {
        String prompt = promptField.getValue();

        // Skip if prompt is empty
        if (prompt == null || prompt.isBlank()) return;

        // Call Gemini service and display result
        String result = geminiService.generateContent(prompt);
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
