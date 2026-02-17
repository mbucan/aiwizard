package com.company.aiwizard.view.supportingservicessample;


import com.company.aiwizard.service.AIWizardEntityDefinitionService;
import com.company.aiwizard.service.AIWizardEntityListService;
import com.company.aiwizard.service.AIWizardTableDDLDefinitionService;
import com.company.aiwizard.service.AIWizardTableListService;
import com.company.aiwizard.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.action.DialogAction;
import io.jmix.flowui.component.textarea.JmixTextArea;
import io.jmix.flowui.component.twincolumn.TwinColumn;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.List;

@Route(value = "supporting-services-sample-view", layout = MainView.class)
@ViewController(id = "SupportingServicesSampleView")
@ViewDescriptor(path = "supporting-services-sample-view.xml")
public class SupportingServicesSampleView extends StandardView {
    @Autowired
    private AIWizardEntityListService aIWizardEntityListService;
    @Autowired
    private AIWizardEntityDefinitionService aIWizardEntityDefinitionService;
    @Autowired
    private AIWizardTableListService aIWizardTableListService;
    @Autowired
    private AIWizardTableDDLDefinitionService aIWizardTableDDLDefinitionService;
    @Autowired
    private Dialogs dialogs;
    @Autowired
    private UiComponents uiComponents;
    @ViewComponent
    private JmixTextArea responseField;

    @Subscribe(id = "entityListBtn", subject = "clickListener")
    public void onEntityListBtnClick(final ClickEvent<JmixButton> event) {
        responseField.clear();
        List<String> entityNames = aIWizardEntityListService.getAllEntityNames();
        responseField.setValue(String.join("\n", entityNames));
    }

    @Subscribe(id = "entityDefinitionsBtn", subject = "clickListener")
    public void onEntityDefinitionsBtnClick(final ClickEvent<JmixButton> event) {
        List<String> entityNames = aIWizardEntityListService.getAllEntityNames();

        TwinColumn<String> twinColumn = uiComponents.create(TwinColumn.class);
        twinColumn.setItems(entityNames);
        twinColumn.setItemLabelGenerator(item -> item);
        twinColumn.setWidth("600px");
        twinColumn.setHeight("400px");
        twinColumn.setSelectAllButtonsVisible(true);

        dialogs.createOptionDialog()
                .withHeader("Select Entities")
                .withContent(twinColumn)
                .withActions(
                        new DialogAction(DialogAction.Type.OK).withHandler(e -> {
                            Collection<String> selectedEntities = twinColumn.getValue();
                            if (selectedEntities != null && !selectedEntities.isEmpty()) {
                                StringBuilder sb = new StringBuilder();
                                for (String entityName : selectedEntities) {
                                    String definition = aIWizardEntityDefinitionService.getEntityDefinitionAsString(entityName);
                                    sb.append("=== ").append(entityName).append(" ===\n");
                                    sb.append(definition).append("\n\n");
                                }
                                responseField.setValue(sb.toString());
                            }
                        }),
                        new DialogAction(DialogAction.Type.CANCEL)
                )
                .open();
    }

    @Subscribe(id = "tableListBtn", subject = "clickListener")
    public void onTableListBtnClick(final ClickEvent<JmixButton> event) {
        responseField.clear();
        List<String> tableNames = aIWizardTableListService.getAllTableNames();
        responseField.setValue(String.join("\n", tableNames));
    }

    @Subscribe(id = "tableDefinitionsDDLBtn", subject = "clickListener")
    public void onTableDefinitionsDDLBtnClick(final ClickEvent<JmixButton> event) {
        List<String> tableNames = aIWizardTableListService.getAllTableNames();

        TwinColumn<String> twinColumn = uiComponents.create(TwinColumn.class);
        twinColumn.setItems(tableNames);
        twinColumn.setItemLabelGenerator(item -> item);
        twinColumn.setWidth("600px");
        twinColumn.setHeight("400px");
        twinColumn.setSelectAllButtonsVisible(true);

        dialogs.createOptionDialog()
                .withHeader("Select Tables")
                .withContent(twinColumn)
                .withActions(
                        new DialogAction(DialogAction.Type.OK).withHandler(e -> {
                            Collection<String> selectedTables = twinColumn.getValue();
                            if (selectedTables != null && !selectedTables.isEmpty()) {
                                StringBuilder sb = new StringBuilder();
                                for (String tableName : selectedTables) {
                                    String ddl = aIWizardTableDDLDefinitionService.getTableDDLAsString(tableName);
                                    sb.append("=== ").append(tableName).append(" ===\n");
                                    sb.append(ddl).append("\n\n");
                                }
                                responseField.setValue(sb.toString());
                            }
                        }),
                        new DialogAction(DialogAction.Type.CANCEL)
                )
                .open();
    }

    @Subscribe(id = "tableDefinitionsMetadataBtn", subject = "clickListener")
    public void onTableDefinitionsMetadataBtnClick(final ClickEvent<JmixButton> event) {
        List<String> tableNames = aIWizardTableListService.getAllTableNames();

        TwinColumn<String> twinColumn = uiComponents.create(TwinColumn.class);
        twinColumn.setItems(tableNames);
        twinColumn.setItemLabelGenerator(item -> item);
        twinColumn.setWidth("600px");
        twinColumn.setHeight("400px");
        twinColumn.setSelectAllButtonsVisible(true);

        dialogs.createOptionDialog()
                .withHeader("Select Tables")
                .withContent(twinColumn)
                .withActions(
                        new DialogAction(DialogAction.Type.OK).withHandler(e -> {
                            Collection<String> selectedTables = twinColumn.getValue();
                            if (selectedTables != null && !selectedTables.isEmpty()) {
                                StringBuilder sb = new StringBuilder();
                                for (String tableName : selectedTables) {
                                    String metadata = aIWizardTableDDLDefinitionService.getTableMetadataAsString(tableName);
                                    sb.append("=== ").append(tableName).append(" ===\n");
                                    sb.append(metadata).append("\n\n");
                                }
                                responseField.setValue(sb.toString());
                            }
                        }),
                        new DialogAction(DialogAction.Type.CANCEL)
                )
                .open();
    }

}