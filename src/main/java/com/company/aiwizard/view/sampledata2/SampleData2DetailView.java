package com.company.aiwizard.view.sampledata2;

import com.company.aiwizard.entity.SampleData2;
import com.company.aiwizard.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;

@Route(value = "sample-data2s/:id", layout = MainView.class)
@ViewController(id = "SampleData2.detail")
@ViewDescriptor(path = "sample-data2-detail-view.xml")
@EditedEntityContainer("sampleData2Dc")
public class SampleData2DetailView extends StandardDetailView<SampleData2> {
}