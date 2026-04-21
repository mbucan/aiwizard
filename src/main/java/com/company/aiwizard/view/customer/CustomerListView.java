package com.company.aiwizard.view.customer;

import com.company.aiwizard.entity.Customer;
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
 * List view for Customer entities with a "Populate with data" action
 * that seeds 10 sample customers (referenced in the README walkthrough).
 */
@Route(value = "customers", layout = MainView.class)
@ViewController(id = "Customer.list")
@ViewDescriptor(path = "customer-list-view.xml")
@LookupComponent("customersDataGrid")
@DialogMode(width = "64em")
public class CustomerListView extends StandardListView<Customer> {

    private static final String[][] SAMPLE_CUSTOMERS = {
            {"Acme Retail", "orders@acme-retail.com", "New York"},
            {"Globex Trading", "ops@globex.com", "Chicago"},
            {"Initech Supplies", "purchasing@initech.com", "Austin"},
            {"Umbrella Goods", "buying@umbrella.com", "Boston"},
            {"Stark Industries", "procurement@stark.com", "Los Angeles"},
            {"Wayne Enterprises", "ap@wayne.com", "Gotham"},
            {"Wonka Distribution", "sweet@wonka.com", "Philadelphia"},
            {"Soylent Foods", "orders@soylent.com", "Seattle"},
            {"Hooli Systems", "purchasing@hooli.com", "San Francisco"},
            {"Pied Piper", "hello@piedpiper.com", "Palo Alto"}
    };

    @Autowired
    private DataManager dataManager;

    @Autowired
    private Notifications notifications;

    @ViewComponent
    private CollectionLoader<Customer> customersDl;

    @Subscribe("populateButton")
    public void onPopulateButtonClick(final ClickEvent<JmixButton> event) {
        long existingCount = dataManager.loadValue(
                        "select count(e) from Customer e", Long.class)
                .one();

        if (existingCount > 0) {
            notifications.create("Data already exists. Population skipped.")
                    .withType(Notifications.Type.WARNING)
                    .show();
            return;
        }

        for (String[] row : SAMPLE_CUSTOMERS) {
            Customer entity = dataManager.create(Customer.class);
            entity.setName(row[0]);
            entity.setEmail(row[1]);
            entity.setCity(row[2]);
            dataManager.save(entity);
        }

        customersDl.load();

        notifications.create("Successfully populated " + SAMPLE_CUSTOMERS.length + " customers.")
                .withType(Notifications.Type.SUCCESS)
                .show();
    }
}
