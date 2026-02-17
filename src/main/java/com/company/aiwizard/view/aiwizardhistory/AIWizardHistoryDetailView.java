package com.company.aiwizard.view.aiwizardhistory;

import com.company.aiwizard.entity.AIWizardHistory;
import com.company.aiwizard.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;

@Route(value = "ai-wizard-history/:id", layout = MainView.class)
@ViewController(id = "AIWizardHistory.detail")
@ViewDescriptor(path = "ai-wizard-history-detail-view.xml")
@EditedEntityContainer("aIWizardHistoryDc")
public class AIWizardHistoryDetailView extends StandardDetailView<AIWizardHistory> {
}