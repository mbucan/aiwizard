package com.company.aiwizard.view.aiwizardhistory;

import com.company.aiwizard.entity.AIWizardHistory;
import com.company.aiwizard.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.*;


@Route(value = "ai-wizard-histories", layout = MainView.class)
@ViewController(id = "AIWizardHistory.list")
@ViewDescriptor(path = "ai-wizard-history-list-view.xml")
@LookupComponent("aIWizardHistoriesDataGrid")
@DialogMode(width = "64em")
public class AIWizardHistoryListView extends StandardListView<AIWizardHistory> {
}