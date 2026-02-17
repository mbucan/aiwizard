package com.company.aiwizard.view.prompttemplatesample;


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

/**
 * View for testing AI Wizard prompt templates.
 * Allows users to select a prompt template, choose database tables or entities,
 * enter a prompt, and generate SQL/JPQL queries using AI services (Gemini or OpenAI).
 */
@Route(value = "prompt-template-sample-view", layout = MainView.class)
@ViewController(id = "PromptTemplateSampleView")
@ViewDescriptor(path = "prompt-template-sample-view.xml")
public class PromptTemplateSampleView extends StandardView {

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

    /** Service for calling Google Gemini LLM API */
    @Autowired
    private GeminiService geminiService;

    /** Service for calling OpenAI LLM API */
    @Autowired
    private OpenAIService openAIService;

    // ==================== View Components ====================

    /** Text area displaying the constructed context (prefix + definitions + prompt) */
    @ViewComponent
    private JmixTextArea contextField;

    /** Text area displaying the AI-generated response */
    @ViewComponent
    private JmixTextArea responseField;

    // ==================== Event Handlers ====================

    /**
     * Handles click on the AI Wizard button.
     * Opens a dialog allowing user to:
     * 1. Select a prompt template (defines LLM, operation type, dataset type, context prefix)
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

                            // -------- Build Context --------
                            // Context = contextPrefix + table/entity definitions + user prompt

                            StringBuilder context = new StringBuilder();

                            // 1. Add context prefix from template (e.g., "Jmix 2.7.4, Reports add-on, Use SQL...")
                            String contextPrefix = selectedTemplate.getContextPrefix();
                            if (contextPrefix != null && !contextPrefix.isEmpty()) {
                                context.append(contextPrefix).append("\n\n");
                            }

                            // 2. Add table DDL or entity definitions based on dataset type
                            if (selectedItems != null && !selectedItems.isEmpty()) {
                                if (dataSetType == DataSetType.SQL) {
                                    // Fetch and append DDL for each selected table
                                    for (String tableName : selectedItems) {
                                        String ddl = aiWizardTableDDLDefinitionService.getTableDDLAsString(tableName);
                                        context.append("=== ").append(tableName).append(" ===\n");
                                        context.append(ddl).append("\n\n");
                                    }
                                } else if (dataSetType == DataSetType.JPQL) {
                                    // Fetch and append metadata for each selected entity
                                    for (String entityName : selectedItems) {
                                        String definition = aiWizardEntityDefinitionService.getEntityDefinitionAsString(entityName);
                                        context.append("=== ").append(entityName).append(" ===\n");
                                        context.append(definition).append("\n\n");
                                    }
                                }
                            }

                            // 3. Add the user's prompt at the end
                            if (prompt != null && !prompt.isEmpty()) {
                                context.append(prompt);
                            }

                            // Display the constructed context for debugging/transparency
                            contextField.setValue(context.toString());

                            // -------- Call AI Service --------
                            // Route to appropriate LLM based on template's connection setting

                            AIWizardConnection connection = selectedTemplate.getConnection();
                            String result = null;

                            if (connection == AIWizardConnection.GEMINI) {
                                // Call Google Gemini API
                                result = geminiService.generateContent(contextField.getValue());
                            } else if (connection == AIWizardConnection.OPENAI) {
                                // Call OpenAI API
                                result = openAIService.generateContent(contextField.getValue());
                            } else {
                                notifications.create("Unsupported AI connection type: " + connection)
                                        .withType(Notifications.Type.ERROR)
                                        .show();
                                return;
                            }

                            // -------- Process and Display Result --------

                            if (result != null) {
                                // Strip markdown code block formatting (```sql, ```, etc.)
                                result = stripCodeBlockFormatting(result);
                                responseField.setValue(result);
                            }

                            notifications.create("Context built successfully")
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