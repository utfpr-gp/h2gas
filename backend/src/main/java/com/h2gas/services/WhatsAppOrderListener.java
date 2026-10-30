package com.h2gas.services;

import com.h2gas.enums.AppSettingKey;
import com.h2gas.enums.OrderStatusEnum;
import com.h2gas.events.OrderStatusChangedEvent;
import com.h2gas.model.entity.Order;
import com.h2gas.model.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// HU26: avisa o cliente pelo WhatsApp que o pedido saiu para entrega; só depois do commit do status
@Component
public class WhatsAppOrderListener {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppOrderListener.class);

    private final WhatsAppSender whatsAppSender;
    private final AppSettingService appSettingService;
    private final OrderRepository orderRepository;

    public WhatsAppOrderListener(WhatsAppSender whatsAppSender, AppSettingService appSettingService,
                                 OrderRepository orderRepository) {
        this.whatsAppSender = whatsAppSender;
        this.appSettingService = appSettingService;
        this.orderRepository = orderRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        if (!OrderStatusEnum.OUT_FOR_DELIVERY.getStatus().equals(event.status())
                || !appSettingService.isActive(AppSettingKey.WHATSAPP_ORDER_OUT_FOR_DELIVERY)) {
            return;
        }
        // a rota já começou e foi gravada: falha aqui fica só no log, de propósito
        try {
            orderRepository.findById(event.orderId()).ifPresent(order ->
                    whatsAppSender.send(order.getCustomer().getPhoneNumber(), message(order)));
        } catch (Exception e) {
            log.error("Não foi possível avisar pelo WhatsApp que o pedido {} saiu para entrega", event.orderId(), e);
        }
    }

    private String message(Order order) {
        String code = order.getId().substring(0, Math.min(8, order.getId().length()));
        String deliverer = order.getDeliverer() == null ? "" :
                " com " + order.getDeliverer().getUser().getName().trim().split("\\s+")[0];
        return "Seu pedido #" + code + " da " + order.getSupplier().getName() + " saiu para entrega" + deliverer
                + ". Acompanhe no H2Gás.";
    }
}
