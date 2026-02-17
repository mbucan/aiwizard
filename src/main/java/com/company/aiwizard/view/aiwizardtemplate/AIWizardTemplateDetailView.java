package com.company.aiwizard.view.aiwizardtemplate;

import com.company.aiwizard.entity.AIWizardTemplate;
import com.company.aiwizard.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;

@Route(value = "ai-wizard-templates/:id", layout = MainView.class)
@ViewController(id = "AIWizardTemplate.detail")
@ViewDescriptor(path = "ai-wizard-template-detail-view.xml")
@EditedEntityContainer("aIWizardTemplateDc")
public class AIWizardTemplateDetailView extends StandardDetailView<AIWizardTemplate> {
}