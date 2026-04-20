package com.company.aiwizard.view.customerorder;

import com.company.aiwizard.entity.CustomerOrder;
import com.company.aiwizard.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;

@Route(value = "customerOrders/:id", layout = MainView.class)
@ViewController(id = "CustomerOrder.detail")
@ViewDescriptor(path = "customer-order-detail-view.xml")
@EditedEntityContainer("customerOrderDc")
public class CustomerOrderDetailView extends StandardDetailView<CustomerOrder> {
}
