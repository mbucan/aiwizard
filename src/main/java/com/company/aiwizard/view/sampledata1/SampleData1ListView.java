package com.company.aiwizard.view.sampledata1;

import com.company.aiwizard.entity.SampleData1;
import com.company.aiwizard.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * List view for SampleData1 entities.
 * Displays a data grid and provides functionality to populate sample data.
 */
@Route(value = "sample-data1s", layout = MainView.class)  // URL path for this view
@ViewController(id = "SampleData1.list")                   // Unique view identifier
@ViewDescriptor(path = "sample-data1-list-view.xml")       // Associated XML layout
@LookupComponent("sampleData1sDataGrid")                   // Component used for entity lookup/selection
@DialogMode(width = "64em")                                // Dialog width when opened as modal
public class SampleData1ListView extends StandardListView<SampleData1> {

    @Autowired
    private DataManager dataManager;  // Jmix data access API for CRUD operations

    @Autowired
    private Notifications notifications;  // UI notification service

    @ViewComponent
    private CollectionLoader<SampleData1> sampleData1sDl;  // Data loader bound to the data grid

    /**
     * Handles click on the "Populate" button.
     * Creates 20 sample records if no data exists yet.
     */
    @Subscribe("populateButton")
    public void onPopulateButtonClick(final ClickEvent<JmixButton> event) {
        // Check if data already exists to prevent duplicates
        long existingCount = dataManager.loadValue(
                        "select count(e) from SampleData1 e", Long.class)
                .one();

        if (existingCount > 0) {
            notifications.create("Data already exists. Population skipped.")
                    .withType(Notifications.Type.WARNING)
                    .show();
            return;
        }

        // Create 20 sample records with sequential values
        for (int i = 1; i <= 20; i++) {
            SampleData1 entity = dataManager.create(SampleData1.class);
            entity.setName("Measurement" + i);
            entity.setValue1(i);                    // Integer value: 1, 2, 3...
            entity.setValue2((double) i / 100);     // Decimal value: 0.01, 0.02, 0.03...
            dataManager.save(entity);
        }

        // Refresh the data grid to show new records
        sampleData1sDl.load();

        notifications.create("Successfully populated 20 records.")
                .withType(Notifications.Type.SUCCESS)
                .show();
    }
}