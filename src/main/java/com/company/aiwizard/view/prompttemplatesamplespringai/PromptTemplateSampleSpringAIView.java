package com.company.aiwizard.view.prompttemplatesamplespringai;


import com.company.aiwizard.entity.AIWizardConnection;
import com.company.aiwizard.entity.AIWizardOperation;
import com.company.aiwizard.entity.AIWizardTemplate;
import com.company.aiwizard.service.*;
import com.company.aiwizard.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.action.DialogAction;
import io.jmix.flowui.component.textarea.JmixTextArea;
import io.jmix.flowui.component.twincolumn.TwinColumn;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.model.DataComponents;
import io.jmix.flowui.view.*;
import io.jmix.reports.entity.DataSetType;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Route(value = "prompt-template-sample-spring-ai-view", layout = MainView.class)
@ViewController(id = "PromptTemplateSampleSpringAIView")
@ViewDescriptor(path = "prompt-template-sample-spring-ai-view.xml")
public class PromptTemplateSampleSpringAIView extends StandardView {

    // ==================== Service Injections ====================

    /** Service to retrieve list of available JPA entities */
    @Autowired
    private AIWizardEntityListService aiWizardEntityListService;

    /** Service to retrieve entity metadata/definitions for JPQL context */
    @Autowired
    private AIWizardEntityDefinitionService aiWizardEntityDefinitionService;

    /** Service to retrieve list of database tables */
    @Autowired
    private AIWizardTableListService aiWizardTableListService;

    /** Service to retrieve table DDL definitions for SQL context */
    @Autowired
    private AIWizardTableDDLDefinitionService aiWizardTableDDLDefinitionService;

    /** Jmix dialogs utility for creating option dialogs */
    @Autowired
    private Dialogs dialogs;

    /** Jmix notifications utility for showing user messages */
    @Autowired
    private Notifications notifications;

    /** Factory for creating UI components programmatically */
    @Autowired
    private UiComponents uiComponents;

    /** Factory for creating data containers and loaders programmatically */
    @Autowired
    private DataComponents dataComponents;

    /** Unified AI service supporting multiple providers (Gemini, OpenAI) via Spring AI */
    @Autowired
    private UnifiedAIService unifiedAIService;

    // ==================== View Components ====================

    /** Text area displaying the constructed context (definitions + prompt) */
    @ViewComponent
    private JmixTextArea contextField;

    /** Text area displaying the AI-generated response */
    @ViewComponent
    private JmixTextArea responseField;

    // ==================== Event Handlers ====================

    /**
     * Handles click on the AI Wizard button.
     * Opens a dialog allowing user to:
     * 1. Select a prompt template (defines LLM, operation type, dataset type, system instruction)
     * 2. Select tables (for SQL) or entities (for JPQL) to include in context
     * 3. Enter/modify the prompt text
     * 4. Generate AI response and display in responseField
     */
    @Subscribe(id = "aiWizardBtn", subject = "clickListener")
    public void onAiWizardBtnClick(final ClickEvent<JmixButton> event) {

        // -------------------- Load Templates --------------------
        // Create a data container and loader to fetch all AIWizardTemplate entities
        CollectionContainer<AIWizardTemplate> templatesDc = dataComponents.createCollectionContainer(AIWizardTemplate.class);
        CollectionLoader<AIWizardTemplate> templatesDl = dataComponents.createCollectionLoader();
        templatesDl.setContainer(templatesDc);
        templatesDl.setQuery("select e from AIWizardTemplate e order by e.name");
        templatesDl.load();

        List<AIWizardTemplate> templates = templatesDc.getItems();

        // -------------------- Create Dialog Components --------------------

        // ComboBox for selecting the prompt template
        ComboBox<AIWizardTemplate> templateComboBox = new ComboBox<>("Prompt Template");
        templateComboBox.setItems(templates);
        templateComboBox.setItemLabelGenerator(t -> t.getName() != null ? t.getName() : "");
        templateComboBox.setWidth("100%");

        // TwinColumn for selecting tables (SQL) or entities (JPQL)
        // Left column shows available items, right column shows selected items
        TwinColumn<String> twinColumn = uiComponents.create(TwinColumn.class);
        twinColumn.setItemLabelGenerator(item -> item);
        twinColumn.setWidth("100%");
        twinColumn.setHeight("300px");
        twinColumn.setSelectAllButtonsVisible(true);
        twinColumn.setLabel("Select Items");
        twinColumn.setItems(Collections.emptyList()); // Initially empty until template is selected

        // Text area for user to enter or modify their prompt
        JmixTextArea promptField = uiComponents.create(JmixTextArea.class);
        promptField.setLabel("Prompt");
        promptField.setWidth("100%");
        promptField.setHeight("150px");

        // -------------------- Template Selection Handler --------------------
        // When user selects a template, populate the twin column with appropriate items
        // and set default prompt text based on operation type
        templateComboBox.addValueChangeListener(e -> {
            AIWizardTemplate selectedTemplate = e.getValue();

            // Handle null selection - clear all dependent fields
            if (selectedTemplate == null) {
                twinColumn.setItems(Collections.emptyList());
                twinColumn.setLabel("Select Items");
                twinColumn.clear();
                promptField.clear();
                return;
            }

            twinColumn.clear();

            // Populate twin column based on dataset type from selected template
            DataSetType dataSetType = selectedTemplate.getDatasetType();
            if (dataSetType == DataSetType.SQL) {
                // For SQL templates, show database table names
                List<String> tableNames = aiWizardTableListService.getAllTableNames();
                twinColumn.setItems(tableNames);
                twinColumn.setLabel("Select Tables");
            } else if (dataSetType == DataSetType.JPQL) {
                // For JPQL templates, show JPA entity names
                List<String> entityNames = aiWizardEntityListService.getAllEntityNames();
                twinColumn.setItems(entityNames);
                twinColumn.setLabel("Select Entities");
            } else {
                // Unsupported dataset types (e.g., Groovy, Entity, JSON)
                twinColumn.setItems(Collections.emptyList());
                twinColumn.setLabel("Select Items");
                notifications.create("Unsupported dataset type: " + dataSetType)
                        .withType(Notifications.Type.WARNING)
                        .show();
            }

            // Set default prompt text based on operation type (CREATE or MODIFY)
            AIWizardOperation operation = selectedTemplate.getOperation();
            if (operation == AIWizardOperation.CREATE) {
                promptField.setValue("Create new report data band query based on this definitions.");
            } else if (operation == AIWizardOperation.MODIFY) {
                promptField.setValue("Modify report data band query based on this definitions.");
            } else {
                promptField.clear();
            }
        });

        // -------------------- Build and Show Dialog --------------------

        // Layout container for dialog content
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);
        content.setWidth("600px");
        content.add(templateComboBox, twinColumn, promptField);

        // Create and open the option dialog with OK and Cancel buttons
        dialogs.createOptionDialog()
                .withHeader("AI Assistance")
                .withContent(content)
                .withActions(
                        // OK button handler - builds context and calls AI service
                        new DialogAction(DialogAction.Type.OK).withHandler(e -> {
                            AIWizardTemplate selectedTemplate = templateComboBox.getValue();
                            Collection<String> selectedItems = twinColumn.getValue();
                            String prompt = promptField.getValue();

                            // -------- Validation --------

                            // Ensure a template is selected
                            if (selectedTemplate == null) {
                                notifications.create("Please select a prompt template")
                                        .withType(Notifications.Type.WARNING)
                                        .show();
                                return;
                            }

                            // Ensure dataset type is supported (SQL or JPQL only)
                            DataSetType dataSetType = selectedTemplate.getDatasetType();
                            if (dataSetType != DataSetType.SQL && dataSetType != DataSetType.JPQL) {
                                notifications.create("Unsupported dataset type: " + dataSetType)
                                        .withType(Notifications.Type.ERROR)
                                        .show();
                                return;
                            }

                            // Get AI provider from template
                            AIWizardConnection connection = selectedTemplate.getConnection();

                            // Verify the provider is available
                            if (!unifiedAIService.isProviderAvailable(connection)) {
                                notifications.create("AI provider " + connection + " is not configured. " +
                                                "Available providers: " + unifiedAIService.getAvailableProviders())
                                        .withType(Notifications.Type.ERROR)
                                        .show();
                                return;
                            }

                            // -------- Build System Instruction --------
                            // contextPrefix from template serves as the system instruction
                            // guiding the AI's behavior and response style

                            String systemInstruction = selectedTemplate.getContextPrefix();

                            // -------- Build User Prompt --------
                            // User prompt = table/entity definitions + user's input

                            StringBuilder userPrompt = new StringBuilder();

                            // 1. Add table DDL or entity definitions based on dataset type
                            if (selectedItems != null && !selectedItems.isEmpty()) {
                                if (dataSetType == DataSetType.SQL) {
                                    // Fetch and append DDL for each selected table
                                    for (String tableName : selectedItems) {
                                        String ddl = aiWizardTableDDLDefinitionService.getTableDDLAsString(tableName);
                                        userPrompt.append("=== ").append(tableName).append(" ===\n");
                                        userPrompt.append(ddl).append("\n\n");
                                    }
                                } else if (dataSetType == DataSetType.JPQL) {
                                    // Fetch and append metadata for each selected entity
                                    for (String entityName : selectedItems) {
                                        String definition = aiWizardEntityDefinitionService.getEntityDefinitionAsString(entityName);
                                        userPrompt.append("=== ").append(entityName).append(" ===\n");
                                        userPrompt.append(definition).append("\n\n");
                                    }
                                }
                            }

                            // 2. Add the user's prompt at the end
                            if (prompt != null && !prompt.isEmpty()) {
                                userPrompt.append(prompt);
                            }

                            // Display the constructed context for debugging/transparency
                            // Shows both system instruction and user prompt
                            StringBuilder displayContext = new StringBuilder();
                            if (systemInstruction != null && !systemInstruction.isEmpty()) {
                                displayContext.append("=== SYSTEM INSTRUCTION ===\n");
                                displayContext.append(systemInstruction).append("\n\n");
                            }
                            displayContext.append("=== USER PROMPT ===\n");
                            displayContext.append(userPrompt);
                            contextField.setValue(displayContext.toString());

                            // -------- Call Unified AI Service --------

                            String result = null;

                            try {
                                if (systemInstruction != null && !systemInstruction.isBlank()) {
                                    // Call with system instruction (contextPrefix)
                                    result = unifiedAIService.generateContent(
                                            userPrompt.toString(),
                                            systemInstruction,
                                            connection
                                    );
                                } else {
                                    // Call without system instruction
                                    result = unifiedAIService.generateContent(
                                            userPrompt.toString(),
                                            connection
                                    );
                                }
                            } catch (Exception ex) {
                                notifications.create("AI generation failed: " + ex.getMessage())
                                        .withType(Notifications.Type.ERROR)
                                        .show();
                                responseField.setValue("Error: " + ex.getMessage());
                                return;
                            }

                            // -------- Process and Display Result --------

                            if (result != null) {
                                // Strip markdown code block formatting (```sql, ```, etc.)
                                result = stripCodeBlockFormatting(result);
                                responseField.setValue(result);
                            }

                            notifications.create("AI response generated successfully")
                                    .withType(Notifications.Type.SUCCESS)
                                    .show();
                        }),
                        // Cancel button - closes dialog without action
                        new DialogAction(DialogAction.Type.CANCEL)
                )
                .open();
    }

    // ==================== Utility Methods ====================

    /**
     * Strips markdown code block formatting from LLM responses.
     * LLMs often wrap code in markdown fences like:
     * ```sql
     * SELECT * FROM table
     * ```
     *
     * This method removes the opening fence (```sql, ```jpql, ```groovy, etc.)
     * and the closing fence (```) to extract clean code.
     *
     * @param text The raw LLM response potentially containing markdown formatting
     * @return Clean code without markdown code block wrappers, or original text if no formatting found
     */
    private String stripCodeBlockFormatting(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        String result = text.trim();

        // Remove opening code fence with optional language identifier
        // Matches patterns like: ```sql, ```jpql, ```groovy, ```java, ``` (plain)
        if (result.startsWith("```")) {
            int firstNewline = result.indexOf('\n');
            if (firstNewline != -1) {
                // Remove everything from start to first newline (inclusive)
                result = result.substring(firstNewline + 1);
            } else {
                // No newline found - edge case, just remove the opening fence using regex
                result = result.replaceFirst("^```\\w*\\s*", "");
            }
        }

        // Remove closing code fence if present at the end
        if (result.endsWith("```")) {
            result = result.substring(0, result.length() - 3);
        }

        return result.trim();
    }
}