package com.h2gas.services;

import com.h2gas.enums.OrderStatusEnum;
import com.h2gas.events.DelivererAssignedEvent;
import com.h2gas.events.OrderPlacedEvent;
import com.h2gas.events.OrderStatusChangedEvent;
import com.h2gas.model.entity.Order;
import com.h2gas.model.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Locale;
import java.util.function.Consumer;

// HU27: push para o cliente (status), para a loja (pedido novo) e para o entregador (atribuição); só depois do commit
@Component
public class PushOrderListener {

    private static final Logger log = LoggerFactory.getLogger(PushOrderListener.class);
    private static final String CUSTOMER_URL = "/meus-pedidos";

    private final PushNotificationService push;
    private final OrderRepository orderRepository;

    public PushOrderListener(PushNotificationService push, OrderRepository orderRepository) {
        this.push = push;
        this.orderRepository = orderRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onStatusChanged(OrderStatusChangedEvent event) {
        withOrder(event.orderId(), order -> {
            String code = code(order);
            String store = order.getSupplier().getName();
            String customer = order.getCustomer().getId();
            String status = event.status();
            if (status.equals(OrderStatusEnum.CONFIRMED.getStatus())) {
                push.notifyUser(customer, "Pedido #" + code + " confirmado", "A " + store + " confirmou o seu pedido.", CUSTOMER_URL);
            } else if (status.equals(OrderStatusEnum.OUT_FOR_DELIVERY.getStatus())) {
                String who = order.getDeliverer() == null ? "O entregador" : firstName(order.getDeliverer().getUser().getName());
                push.notifyUser(customer, "Pedido #" + code + " saiu para entrega",
                        who + " está a caminho com o seu pedido da " + store + ".", CUSTOMER_URL);
            } else if (status.equals(OrderStatusEnum.DELIVERED.getStatus())) {
                push.notifyUser(customer, "Pedido #" + code + " entregue", "Bom proveito! Conte como foi em Meus pedidos.", CUSTOMER_URL);
            } else if (status.equals(OrderStatusEnum.CANCELED.getStatus())) {
                push.notifyUser(customer, "Pedido #" + code + " cancelado",
                        "A " + store + " cancelou o pedido. Veja o motivo em Meus pedidos.", CUSTOMER_URL);
            }
        }, needsOrder(event.status()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOrderPlaced(OrderPlacedEvent event) {
        withOrder(event.orderId(), order -> push.notifyUser(order.getSupplier().getId(), "Novo pedido #" + code(order),
                firstName(order.getCustomer().getName()) + " · " + money(order), "/minha-loja/pedidos"), true);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onDelivererAssigned(DelivererAssignedEvent event) {
        withOrder(event.orderId(), order -> {
            if (order.getDeliverer() == null) return;
            push.notifyUser(order.getDeliverer().getUser().getId(), "Nova entrega #" + code(order),
                    "Da " + order.getSupplier().getName() + ". Toque para ver a rota.", "/entregas/" + order.getId());
        }, true);
    }

    // só busca o pedido quando o status gera aviso
    private static boolean needsOrder(String status) {
        return status.equals(OrderStatusEnum.CONFIRMED.getStatus()) || status.equals(OrderStatusEnum.OUT_FOR_DELIVERY.getStatus())
                || status.equals(OrderStatusEnum.DELIVERED.getStatus()) || status.equals(OrderStatusEnum.CANCELED.getStatus());
    }

    // o pedido já foi gravado: falha aqui fica só no log, de propósito
    private void withOrder(String orderId, Consumer<Order> action, boolean needed) {
        if (!needed) return;
        try {
            orderRepository.findById(orderId).ifPresent(action);
        } catch (Exception e) {
            log.error("Não foi possível enviar o push do pedido {}", orderId, e);
        }
    }

    private static String code(Order order) {
        return order.getId().substring(0, Math.min(8, order.getId().length()));
    }

    private static String firstName(String name) {
        return name == null ? "" : name.trim().split("\\s+")[0];
    }

    private static String money(Order order) {
        return "R$ " + String.format(new Locale("pt", "BR"), "%.2f", order.getTotalPrice());
    }
}
