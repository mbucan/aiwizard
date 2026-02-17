package com.company.aiwizard.view.sampledata1;

import com.company.aiwizard.entity.SampleData1;
import com.company.aiwizard.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;

@Route(value = "sample-data1s/:id", layout = MainView.class)
@ViewController(id = "SampleData1.detail")
@ViewDescriptor(path = "sample-data1-detail-view.xml")
@EditedEntityContainer("sampleData1Dc")
public class SampleData1DetailView extends StandardDetailView<SampleData1> {
}