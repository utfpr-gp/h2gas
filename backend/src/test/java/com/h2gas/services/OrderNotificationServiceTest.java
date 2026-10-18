package com.h2gas.services;

import com.h2gas.model.entity.Order;
import com.h2gas.model.entity.Supplier;
import com.h2gas.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderNotificationServiceTest {

    @Mock
    private MailService mailService;

    @InjectMocks
    private OrderNotificationService orderNotificationService;

    private Order pedidoCancelado;

    @BeforeEach
    void setUp() {
        User cliente = new User();
        cliente.setName("Maria");
        cliente.setEmail("maria@teste.com");
        Supplier fornecedor = new Supplier();
        fornecedor.setName("Gás Centro");
        pedidoCancelado = new Order();
        pedidoCancelado.setId("pedido-123");
        pedidoCancelado.setCustomer(cliente);
        pedidoCancelado.setSupplier(fornecedor);
        pedidoCancelado.setCancellationReason("Sem entregador disponível");
    }

    @Test
    @DisplayName("HU06: avisa o cliente por e-mail com o motivo do cancelamento")
    void deveEnviarEmailAoClienteComMotivo() {
        boolean notificado = orderNotificationService.notifyCancellation(pedidoCancelado);

        assertTrue(notificado);
        ArgumentCaptor<String> corpo = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendMail(eq("maria@teste.com"), eq("Seu pedido foi cancelado"), corpo.capture());
        assertTrue(corpo.getValue().contains("Sem entregador disponível"));
        assertTrue(corpo.getValue().contains("Gás Centro"));
        assertTrue(corpo.getValue().contains("pedido-123"));
    }

    @Test
    @DisplayName("HU06: falha no e-mail não lança, devolve falso")
    void deveRetornarFalsoQuandoEmailFalhar() {
        doThrow(new MailSendException("SMTP fora")).when(mailService).sendMail(anyString(), anyString(), anyString());

        assertFalse(orderNotificationService.notifyCancellation(pedidoCancelado));
    }
}
