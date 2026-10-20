package com.h2gas.services;

import com.h2gas.enums.OrderStatusEnum;
import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.model.dto.PaymentShareDTO;
import com.h2gas.model.dto.ProductSalesDTO;
import com.h2gas.model.dto.RevenuePointDTO;
import com.h2gas.model.dto.SupplierDashboardDTO;
import com.h2gas.model.entity.Order;
import com.h2gas.model.entity.OrderItem;
import com.h2gas.model.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// HU09: indicadores do mês do fornecedor. Agrega em Java (mês de um fornecedor é pequeno) para não
// depender de funções de data específicas do MySQL e poder testar a regra sem banco real.
@Service
public class SupplierDashboardService {
    private static final int TOP_PRODUCTS = 5;
    private static final String NO_PAYMENT = "NAO_INFORMADO";

    private final OrderRepository orderRepository;

    public SupplierDashboardService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public SupplierDashboardDTO buildDashboard(String supplierId, YearMonth month) {
        if (month.isAfter(YearMonth.now())) {
            throw new BusinessRuleException("Mês futuro não tem indicadores.");
        }
        List<Order> orders = countedOrders(supplierId, month);
        List<Order> previousOrders = countedOrders(supplierId, month.minusMonths(1));

        BigDecimal revenue = sumTotal(orders);
        BigDecimal previousRevenue = sumTotal(previousOrders);

        SupplierDashboardDTO dashboard = new SupplierDashboardDTO();
        dashboard.setMonth(month.toString());
        dashboard.setTotalRevenue(revenue);
        dashboard.setRevenueChangePercent(changePercent(revenue, previousRevenue));
        dashboard.setTotalOrders(orders.size());
        dashboard.setOrdersChangePercent(changePercent(BigDecimal.valueOf(orders.size()), BigDecimal.valueOf(previousOrders.size())));
        dashboard.setAverageTicket(orders.isEmpty() ? null : revenue.divide(BigDecimal.valueOf(orders.size()), 2, RoundingMode.HALF_UP));
        dashboard.setReceivable(sumTotal(orders.stream().filter(o -> o.getStatus() != OrderStatusEnum.DELIVERED).toList()));
        dashboard.setDeliveredOrders(orders.stream().filter(o -> o.getStatus() == OrderStatusEnum.DELIVERED).count());
        dashboard.setPendingOrders(orderRepository.countBySupplierIdAndStatus(supplierId, OrderStatusEnum.PENDING));
        dashboard.setOutForDeliveryOrders(orderRepository.countBySupplierIdAndStatus(supplierId, OrderStatusEnum.OUT_FOR_DELIVERY));
        dashboard.setAverageDeliveryMinutes(averageDeliveryMinutes(orders));
        dashboard.setRevenueByDay(revenueByDay(orders, month));
        dashboard.setSalesByProduct(salesByProduct(orders));
        dashboard.setPaymentBreakdown(paymentBreakdown(orders));
        return dashboard;
    }

    // pedido contado = não cancelado e criado no mês
    private List<Order> countedOrders(String supplierId, YearMonth month) {
        return orderRepository.findForDashboard(supplierId, month.atDay(1).atStartOfDay(), month.plusMonths(1).atDay(1).atStartOfDay())
                .stream()
                .filter(o -> o.getStatus() != OrderStatusEnum.CANCELED)
                .toList();
    }

    private BigDecimal sumTotal(List<Order> orders) {
        return orders.stream()
                .map(o -> o.getTotalPrice() == null ? BigDecimal.ZERO : o.getTotalPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Double changePercent(BigDecimal current, BigDecimal previous) {
        if (previous.signum() == 0) {
            return null;
        }
        return current.subtract(previous).multiply(BigDecimal.valueOf(100))
                .divide(previous, 1, RoundingMode.HALF_UP).doubleValue();
    }

    private Double averageDeliveryMinutes(List<Order> orders) {
        List<Order> measured = orders.stream()
                .filter(o -> o.getStatus() == OrderStatusEnum.DELIVERED && o.getDispatchedAt() != null && o.getDeliveredAt() != null)
                .toList();
        if (measured.isEmpty()) {
            return null;
        }
        double minutes = measured.stream()
                .mapToLong(o -> Duration.between(o.getDispatchedAt(), o.getDeliveredAt()).getSeconds())
                .average().orElse(0) / 60.0;
        return BigDecimal.valueOf(minutes).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private List<RevenuePointDTO> revenueByDay(List<Order> orders, YearMonth month) {
        Map<LocalDate, BigDecimal> byDay = orders.stream().collect(Collectors.groupingBy(
                o -> o.getCreatedAt().toLocalDate(),
                Collectors.reducing(BigDecimal.ZERO, o -> o.getTotalPrice() == null ? BigDecimal.ZERO : o.getTotalPrice(), BigDecimal::add)));
        return IntStream.rangeClosed(1, month.lengthOfMonth())
                .mapToObj(month::atDay)
                .map(day -> new RevenuePointDTO(day, byDay.getOrDefault(day, BigDecimal.ZERO)))
                .toList();
    }

    private List<ProductSalesDTO> salesByProduct(List<Order> orders) {
        Map<String, List<OrderItem>> byProduct = orders.stream()
                .filter(o -> o.getItems() != null)
                .flatMap(o -> o.getItems().stream())
                .collect(Collectors.groupingBy(item -> item.getProduct().getBrand() + " " + item.getProduct().getCategoryProduct().getName()));
        return byProduct.entrySet().stream()
                .map(entry -> new ProductSalesDTO(
                        entry.getKey(),
                        entry.getValue().stream().mapToLong(OrderItem::getQuantity).sum(),
                        entry.getValue().stream().map(i -> i.getTotalPrice() == null ? BigDecimal.ZERO : i.getTotalPrice()).reduce(BigDecimal.ZERO, BigDecimal::add)))
                .sorted(Comparator.comparingLong(ProductSalesDTO::getQuantity).reversed().thenComparing(ProductSalesDTO::getProductName))
                .limit(TOP_PRODUCTS)
                .toList();
    }

    private List<PaymentShareDTO> paymentBreakdown(List<Order> orders) {
        Map<String, List<Order>> byMethod = orders.stream()
                .collect(Collectors.groupingBy(o -> o.getPaymentMethod() == null ? NO_PAYMENT : o.getPaymentMethod()));
        return byMethod.entrySet().stream()
                .map(entry -> new PaymentShareDTO(
                        entry.getKey(),
                        entry.getValue().size(),
                        sumTotal(entry.getValue()),
                        BigDecimal.valueOf(entry.getValue().size() * 100.0 / orders.size()).setScale(1, RoundingMode.HALF_UP).doubleValue()))
                .sorted(Comparator.comparingLong(PaymentShareDTO::getOrders).reversed().thenComparing(PaymentShareDTO::getMethod))
                .toList();
    }
}
