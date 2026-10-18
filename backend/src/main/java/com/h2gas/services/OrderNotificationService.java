package com.h2gas.services;

import com.h2gas.model.entity.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;

@Service
public class OrderNotificationService {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationService.class);

    private final MailService mailService;

    public OrderNotificationService(MailService mailService) {
        this.mailService = mailService;
    }

    /**
     * HU06: avisa o cliente do cancelamento. Roda fora da transação do cancelamento:
     * falha de e-mail não desfaz o cancelamento, é devolvida como false para a tela avisar o fornecedor.
     */
    public boolean notifyCancellation(Order order) {
        String supplierName = order.getSupplier() != null ? order.getSupplier().getName() : "o fornecedor";
        String body = "Olá " + order.getCustomer().getName() + ",\n\n"
                + "Seu pedido " + order.getId() + " foi cancelado por " + supplierName + ".\n"
                + "Motivo: " + order.getCancellationReason() + "\n\n"
                + "O pagamento seria feito na entrega, então nada foi cobrado.\n"
                + "Equipe H2Gás";
        try {
            mailService.sendMail(order.getCustomer().getEmail(), "Seu pedido foi cancelado", body);
            return true;
        } catch (MailException e) {
            log.warn("Pedido {} cancelado, mas o e-mail ao cliente falhou: {}", order.getId(), e.getMessage());
            return false;
        }
    }
}
