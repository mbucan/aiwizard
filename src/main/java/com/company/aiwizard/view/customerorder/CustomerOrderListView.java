package com.company.aiwizard.view.customerorder;

import com.company.aiwizard.entity.Customer;
import com.company.aiwizard.entity.CustomerOrder;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

/**
 * List view for CustomerOrder entities with a "Populate with data" action
 * that generates 30 sample orders linking existing customers and products
 * (referenced in the README walkthrough).
 */
@Route(value = "customerOrders", layout = MainView.class)
@ViewController(id = "CustomerOrder.list")
@ViewDescriptor(path = "customer-order-list-view.xml")
@LookupComponent("customerOrdersDataGrid")
@DialogMode(width = "64em")
public class CustomerOrderListView extends StandardListView<CustomerOrder> {

    private static final int SAMPLE_ORDER_COUNT = 30;

    @Autowired
    private DataManager dataManager;

    @Autowired
    private Notifications notifications;

    @ViewComponent
    private CollectionLoader<CustomerOrder> customerOrdersDl;

    @Subscribe("populateButton")
    public void onPopulateButtonClick(final ClickEvent<JmixButton> event) {
        long existingCount = dataManager.loadValue(
                        "select count(e) from CustomerOrder e", Long.class)
                .one();

        if (existingCount > 0) {
            notifications.create("Data already exists. Population skipped.")
                    .withType(Notifications.Type.WARNING)
                    .show();
            return;
        }

        List<Customer> customers = dataManager.load(Customer.class).all().list();
        List<Product> products = dataManager.load(Product.class).all().list();

        if (customers.isEmpty() || products.isEmpty()) {
            notifications.create("Populate Customers and Products first.")
                    .withType(Notifications.Type.WARNING)
                    .show();
            return;
        }

        Random random = new Random(42); // deterministic seed for reproducible demos
        LocalDate today = LocalDate.now();

        for (int i = 1; i <= SAMPLE_ORDER_COUNT; i++) {
            Customer customer = customers.get(random.nextInt(customers.size()));
            Product product = products.get(random.nextInt(products.size()));
            int quantity = 1 + random.nextInt(5);
            BigDecimal totalAmount = product.getPrice()
                    .multiply(BigDecimal.valueOf(quantity));

            CustomerOrder order = dataManager.create(CustomerOrder.class);
            order.setOrderNumber(String.format("ORD-%04d", i));
            order.setCustomer(customer);
            order.setProduct(product);
            order.setQuantity(quantity);
            order.setTotalAmount(totalAmount);
            order.setOrderDate(today.minusDays(random.nextInt(90)));
            dataManager.save(order);
        }

        customerOrdersDl.load();

        notifications.create("Successfully populated " + SAMPLE_ORDER_COUNT + " orders.")
                .withType(Notifications.Type.SUCCESS)
                .show();
    }
}
