package com.h2gas.model.mapper;

import com.h2gas.configuration.mapperConfig.MapperConfig;
import com.h2gas.enums.OrderStatusEnum;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.dto.OrderResponseDTO;
import com.h2gas.model.entity.Deliverer;
import com.h2gas.model.entity.Order;
import com.h2gas.model.entity.Supplier;
import com.h2gas.model.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

// HU25: telefones para o atalho de WhatsApp
class OrderMapperTest {

    private OrderMapper novoMapper() {
        OrderMapper mapper = new OrderMapper();
        ReflectionTestUtils.setField(mapper, "modelMapper", new MapperConfig().modelMapper());
        return mapper;
    }

    private Order pedido(OrderStatusEnum status) {
        User cliente = new User("cliente-1", "Maria Souza", "maria@teste.com", "41999990000", "111", "senha", UserTypeEnum.CUSTOMER);
        Supplier loja = new Supplier();
        loja.setId("loja-1");
        loja.setName("Gás Centro");
        loja.setPhoneNumber("4233334444");
        User userEntregador = new User("ent-1", "Carlos Lima", "carlos@teste.com", "42988887777", "222", "senha", UserTypeEnum.DELIVERER);
        Deliverer entregador = new Deliverer();
        entregador.setId(11L);
        entregador.setUser(userEntregador);
        Order o = new Order();
        o.setId("4a1a4ffe-0000");
        o.setCustomer(cliente);
        o.setSupplier(loja);
        o.setDeliverer(entregador);
        o.setStatus(status);
        o.setTotalPrice(new BigDecimal("100.00"));
        return o;
    }

    @Test
    void deveLevarOTelefoneDaLoja() {
        OrderResponseDTO dto = novoMapper().toResponseDto(pedido(OrderStatusEnum.CONFIRMED));

        assertEquals("4233334444", dto.getSupplierPhone());
    }

    @Test
    void deveLevarOTelefoneDoEntregadorSoEmRota() {
        assertEquals("42988887777", novoMapper().toResponseDto(pedido(OrderStatusEnum.OUT_FOR_DELIVERY)).getDelivererPhone());
        for (OrderStatusEnum status : new OrderStatusEnum[]{OrderStatusEnum.IN_PREPARATION, OrderStatusEnum.DELIVERED, OrderStatusEnum.CANCELED}) {
            assertNull(novoMapper().toResponseDto(pedido(status)).getDelivererPhone(), status.name());
        }
    }
}
