package com.company.aiwizard.view.override.reportdetail;

import com.company.aiwizard.entity.AIWizardConnection;
import com.company.aiwizard.entity.AIWizardHistory;
import com.company.aiwizard.entity.AIWizardOperation;
import com.company.aiwizard.entity.AIWizardTemplate;
import com.company.aiwizard.service.*;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.action.DialogAction;
import io.jmix.flowui.component.codeeditor.CodeEditor;
import io.jmix.flowui.component.select.JmixSelect;
import io.jmix.flowui.component.textarea.JmixTextArea;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.component.twincolumn.TwinColumn;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.model.DataComponents;
import io.jmix.flowui.view.*;
import io.jmix.reports.entity.DataSetType;
import io.jmix.reportsflowui.view.report.ReportDetailView;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Extended Report Detail View with AI Wizard functionality.
 * Overrides the standard Jmix Reports add-on ReportDetailView to add
 * AI-assisted generation and modification of report data band queries.
 *
 * Features:
 * - AI-powered SQL/JPQL query generation using multiple LLM providers (Gemini, OpenAI)
 * - Template-based prompt configuration with customizable system instructions
 * - Automatic inclusion of table DDL or entity definitions in context
 * - Conversation history support for iterative query refinement
 * - Audit trail of all AI interactions via AIWizardHistory entity
 *
 * Usage:
 * 1. Open a report in edit mode
 * 2. Select or create a data band with SQL or JPQL dataset type
 * 3. Click the "AI Wizard" button
 * 4. Select a prompt template, tables/entities, and enter your prompt
 * 5. The AI will generate or modify the query based on your input
 */
@Route(value = "reports/:id", layout = DefaultMainViewParent.class)
@ViewController(id = "report_Report.detail")
@ViewDescriptor(path = "ext-report-detail-view.xml")
public class ExtReportDetailView extends ReportDetailView {

    // ==================== Service Injections ====================

    /** Service to retrieve list of available JPA entities for JPQL queries */
    @Autowired
    private AIWizardEntityListService aiWizardEntityListService;

    /** Service to retrieve entity metadata/definitions for JPQL context */
    @Autowired
    private AIWizardEntityDefinitionService aiWizardEntityDefinitionService;

    /** Service to retrieve list of database tables for SQL queries */
    @Autowired
    private AIWizardTableListService aiWizardTableListService;

    /** Service to retrieve table DDL definitions for SQL context */
    @Autowired
    private AIWizardTableDDLDefinitionService aiWizardTableDDLDefinitionService;

    /** Unified AI service supporting multiple providers (Gemini, OpenAI) via Spring AI */
    @Autowired
    private UnifiedAIService unifiedAIService;

    /** Factory for creating UI components programmatically */
    @Autowired
    private UiComponents uiComponents;

    /** Factory for creating data containers and loaders programmatically */
    @Autowired
    private DataComponents dataComponents;

    /** Jmix DataManager for CRUD operations on AIWizardHistory */
    @Autowired
    private DataManager dataManager;

    // ==================== View Components ====================

    /** Text field displaying the current data band code/name */
    @ViewComponent
    private TypedTextField<String> codeField;

    /** Dropdown for selecting dataset type (SQL, JPQL, Groovy, etc.) */
    @ViewComponent
    private JmixSelect<DataSetType> singleDataSetTypeField;

    /** Code editor containing the data band query script */
    @ViewComponent
    private CodeEditor dataSetScriptCodeEditor;

    // ==================== Event Handlers ====================

    /**
     * Handles click on the AI Wizard button.
     * Opens a dialog allowing users to:
     * 1. Select a prompt template (defines LLM provider, operation type, system instruction)
     * 2. Select tables (for SQL) or entities (for JPQL) to include in context
     * 3. Enter/modify the prompt text
     * 4. Optionally include previous interactions from history
     * 5. Generate or modify the data band query using AI
     *
     * The generated query is written directly to the dataSetScriptCodeEditor
     * and the interaction is saved to AIWizardHistory for audit and future reference.
     *
     * @param event the button click event
     */
    @Subscribe("aiWizardBtn")
    public void onAiWizardBtnClick(final ClickEvent<JmixButton> event) {

        // -------------------- Pre-validation --------------------
        // Ensure the current dataset type is supported (SQL or JPQL only)

        DataSetType currentDataSetType = singleDataSetTypeField.getValue();
        if (currentDataSetType == null ||
                (currentDataSetType != DataSetType.SQL && currentDataSetType != DataSetType.JPQL)) {
            notifications.create("Unsupported dataset type. Only SQL and JPQL are supported.")
                    .withType(Notifications.Type.WARNING)
                    .show();
            return;
        }

        // -------------------- Load Templates --------------------
        // Fetch only templates matching the current dataset type

        CollectionContainer<AIWizardTemplate> templatesDc =
                dataComponents.createCollectionContainer(AIWizardTemplate.class);
        CollectionLoader<AIWizardTemplate> templatesDl = dataComponents.createCollectionLoader();
        templatesDl.setContainer(templatesDc);
        templatesDl.setQuery("select e from AIWizardTemplate e where e.datasetType = :datasetType order by e.name");
        templatesDl.setParameter("datasetType", currentDataSetType);
        templatesDl.load();

        List<AIWizardTemplate> templates = templatesDc.getItems();

        // Show warning if no templates configured for this dataset type
        if (templates.isEmpty()) {
            notifications.create("No prompt templates found for " + currentDataSetType + " dataset type")
                    .withType(Notifications.Type.WARNING)
                    .show();
            return;
        }

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
        twinColumn.setItems(Collections.emptyList());

        // Text area for user to enter or modify their prompt
        JmixTextArea promptField = uiComponents.create(JmixTextArea.class);
        promptField.setLabel("Prompt");
        promptField.setWidth("100%");
        promptField.setHeight("150px");

        // ComboBox for selecting how many previous interactions to include in context
        // Useful for iterative refinement but be careful of token limits
        ComboBox<Integer> historyDepthComboBox = new ComboBox<>("History Depth");
        historyDepthComboBox.setItems(IntStream.rangeClosed(0, 10).boxed().collect(Collectors.toList()));
        historyDepthComboBox.setValue(0); // Default: no history
        historyDepthComboBox.setWidth("100%");

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

            // Clear previous selection to avoid mixing tables and entities
            twinColumn.clear();

            // Populate twin column based on dataset type
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

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);
        content.setWidth("600px");
        content.add(templateComboBox, twinColumn, promptField, historyDepthComboBox);

        dialogs.createOptionDialog()
                .withHeader("AI Wizard")
                .withContent(content)
                .withActions(
                        // OK button handler - builds context and calls AI service
                        new DialogAction(DialogAction.Type.OK).withHandler(e -> {
                            processAIWizardRequest(
                                    templateComboBox.getValue(),
                                    twinColumn.getValue(),
                                    promptField.getValue(),
                                    historyDepthComboBox.getValue()
                            );
                        }),
                        // Cancel button - closes dialog without action
                        new DialogAction(DialogAction.Type.CANCEL)
                )
                .open();
    }

    // ==================== AI Processing Methods ====================

    /**
     * Processes the AI Wizard request by building context, calling the AI service,
     * and saving the interaction to history.
     *
     * @param selectedTemplate the selected prompt template
     * @param selectedItems    the selected tables (SQL) or entities (JPQL)
     * @param prompt           the user's prompt text
     * @param historyDepth     number of previous interactions to include in context
     */
    private void processAIWizardRequest(AIWizardTemplate selectedTemplate,
                                        Collection<String> selectedItems,
                                        String prompt,
                                        Integer historyDepth) {

        // -------- Validation --------

        if (selectedTemplate == null) {
            notifications.create("Please select a prompt template")
                    .withType(Notifications.Type.WARNING)
                    .show();
            return;
        }

        DataSetType dataSetType = selectedTemplate.getDatasetType();
        if (dataSetType != DataSetType.SQL && dataSetType != DataSetType.JPQL) {
            notifications.create("Unsupported dataset type: " + dataSetType)
                    .withType(Notifications.Type.ERROR)
                    .show();
            return;
        }

        // Verify the AI provider is available/configured
        AIWizardConnection connection = selectedTemplate.getConnection();
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
        // User prompt = definitions + history + current script (if modify) + user input

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

        // 2. Add previous interactions from history if requested
        // Provides conversation continuity for iterative refinement
        if (historyDepth != null && historyDepth > 0) {
            List<AIWizardHistory> historyList = dataManager.load(AIWizardHistory.class)
                    .query("select e from AIWizardHistory e " +
                            "where e.aiWizardPromptTemplate = :template " +
                            "order by e.createdDate desc")
                    .parameter("template", selectedTemplate)
                    .maxResults(historyDepth)
                    .list();

            if (!historyList.isEmpty()) {
                userPrompt.append("=== Previous Interactions ===\n");
                // Reverse to show oldest first (chronological order)
                for (int i = historyList.size() - 1; i >= 0; i--) {
                    AIWizardHistory history = historyList.get(i);
                    userPrompt.append("--- Interaction ").append(historyList.size() - i).append(" ---\n");
                    if (history.getPrompt() != null) {
                        userPrompt.append("Prompt: ").append(history.getPrompt()).append("\n");
                    }
                    if (history.getResponse() != null) {
                        userPrompt.append("Response: ").append(history.getResponse()).append("\n");
                    }
                    userPrompt.append("\n");
                }
                userPrompt.append("\n");
            }
        }

        // 3. For MODIFY operation, include the current script in context
        // Store original value for history audit trail
        String originalValue = null;
        AIWizardOperation operation = selectedTemplate.getOperation();
        if (operation == AIWizardOperation.MODIFY) {
            String currentScript = dataSetScriptCodeEditor.getValue();
            if (currentScript != null && !currentScript.isEmpty()) {
                originalValue = currentScript;
                userPrompt.append("=== Current Script ===\n");
                userPrompt.append(currentScript).append("\n\n");
            }
        }

        // 4. Add the user's prompt at the end
        if (prompt != null && !prompt.isEmpty()) {
            userPrompt.append(prompt);
        }

        // Store for history - the full user prompt without system instruction
        final String fullUserPrompt = userPrompt.toString();
        final String finalOriginalValue = originalValue;

        // -------- Call Unified AI Service --------

        String result;
        try {
            if (systemInstruction != null && !systemInstruction.isBlank()) {
                // Call with system instruction (contextPrefix)
                result = unifiedAIService.generateContent(
                        fullUserPrompt,
                        systemInstruction,
                        connection
                );
            } else {
                // Call without system instruction
                result = unifiedAIService.generateContent(
                        fullUserPrompt,
                        connection
                );
            }
        } catch (Exception ex) {
            notifications.create("AI generation failed: " + ex.getMessage())
                    .withType(Notifications.Type.ERROR)
                    .show();
            return;
        }

        // -------- Process and Apply Result --------

        if (result != null) {
            // Strip markdown code block formatting (```sql, ```, etc.)
            result = stripCodeBlockFormatting(result);

            // Apply the generated/modified query to the code editor
            dataSetScriptCodeEditor.setValue(result);

            // Save interaction to AIWizardHistory for audit and future reference
            saveToHistory(selectedTemplate, finalOriginalValue, fullUserPrompt, result);

            notifications.create("AI assistance completed successfully")
                    .withType(Notifications.Type.SUCCESS)
                    .show();
        }
    }

    /**
     * Saves the AI interaction to AIWizardHistory for audit purposes.
     * Records the template used, original value (for modifications),
     * the full prompt sent, and the AI response received.
     *
     * @param template      the prompt template used
     * @param originalValue the original script value (for MODIFY operations)
     * @param prompt        the full prompt sent to the AI
     * @param response      the AI-generated response
     */
    private void saveToHistory(AIWizardTemplate template,
                               String originalValue,
                               String prompt,
                               String response) {
        AIWizardHistory history = dataManager.create(AIWizardHistory.class);
        history.setAiWizardPromptTemplate(template);
        history.setOriginalValue(originalValue);
        history.setPrompt(prompt);
        history.setResponse(response);
        dataManager.save(history);
    }

    // ==================== Utility Methods ====================

    /**
     * Strips markdown code block formatting from LLM responses.
     * LLMs often wrap code in markdown fences like:
     *
     * ```sql
     * SELECT * FROM table
     * ```
     *
     * This method removes the opening fence (```sql, ```jpql, ```groovy, etc.)
     * and the closing fence (```) to extract clean code suitable for
     * direct insertion into the code editor.
     *
     * @param text The raw LLM response potentially containing markdown formatting
     * @return Clean code without markdown code block wrappers,
     *         or original text if no formatting found
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