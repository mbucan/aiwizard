package com.company.aiwizard.view.aiwizardtemplate;

import com.company.aiwizard.entity.AIWizardTemplate;
import com.company.aiwizard.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.*;


@Route(value = "ai-wizard-templates", layout = MainView.class)
@ViewController(id = "AIWizardTemplate.list")
@ViewDescriptor(path = "ai-wizard-template-list-view.xml")
@LookupComponent("aIWizardTemplatesDataGrid")
@DialogMode(width = "64em")
public class AIWizardTemplateListView extends StandardListView<AIWizardTemplate> {
}