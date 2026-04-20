package com.company.aiwizard.view.product;

import com.company.aiwizard.entity.Product;
import com.company.aiwizard.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

/**
 * List view for Product entities with a "Populate with data" action
 * that seeds 15 sample products (referenced in the README walkthrough).
 */
@Route(value = "products", layout = MainView.class)
@ViewController(id = "Product.list")
@ViewDescriptor(path = "product-list-view.xml")
@LookupComponent("productsDataGrid")
@DialogMode(width = "64em")
public class ProductListView extends StandardListView<Product> {

    private static final Object[][] SAMPLE_PRODUCTS = {
            {"Wireless Mouse", "19.99", "Electronics"},
            {"Mechanical Keyboard", "89.50", "Electronics"},
            {"USB-C Hub", "34.00", "Electronics"},
            {"Noise-Cancelling Headphones", "249.99", "Electronics"},
            {"Office Chair", "199.00", "Furniture"},
            {"Standing Desk", "429.00", "Furniture"},
            {"LED Desk Lamp", "45.50", "Furniture"},
            {"Coffee Beans 1kg", "24.90", "Groceries"},
            {"Green Tea Pack", "12.50", "Groceries"},
            {"Sparkling Water 12-pack", "9.99", "Groceries"},
            {"Cotton T-Shirt", "18.00", "Apparel"},
            {"Running Shoes", "79.99", "Apparel"},
            {"Hardcover Notebook", "14.25", "Stationery"},
            {"Gel Pen Set", "8.75", "Stationery"},
            {"Backpack", "59.00", "Accessories"}
    };

    @Autowired
    private DataManager dataManager;

    @Autowired
    private Notifications notifications;

    @ViewComponent
    private CollectionLoader<Product> productsDl;

    @Subscribe("populateButton")
    public void onPopulateButtonClick(final ClickEvent<JmixButton> event) {
        long existingCount = dataManager.loadValue(
                        "select count(e) from Product e", Long.class)
                .one();

        if (existingCount > 0) {
            notifications.create("Data already exists. Population skipped.")
                    .withType(Notifications.Type.WARNING)
                    .show();
            return;
        }

        for (Object[] row : SAMPLE_PRODUCTS) {
            Product entity = dataManager.create(Product.class);
            entity.setName((String) row[0]);
            entity.setPrice(new BigDecimal((String) row[1]));
            entity.setCategory((String) row[2]);
            dataManager.save(entity);
        }

        productsDl.load();

        notifications.create("Successfully populated " + SAMPLE_PRODUCTS.length + " products.")
                .withType(Notifications.Type.SUCCESS)
                .show();
    }
}
