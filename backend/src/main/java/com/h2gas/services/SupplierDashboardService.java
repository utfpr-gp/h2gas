package com.h2gas.services;

import com.h2gas.enums.DeliveryModeEnum;
import com.h2gas.enums.OrderStatusEnum;
import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.model.dto.CategorySalesDTO;
import com.h2gas.model.dto.DashboardFilter;
import com.h2gas.model.dto.DeliveryModeShareDTO;
import com.h2gas.model.dto.PaymentShareDTO;
import com.h2gas.model.dto.ProductSalesDTO;
import com.h2gas.model.dto.RevenuePointDTO;
import com.h2gas.model.dto.StoreCategorySalesDTO;
import com.h2gas.model.dto.SupplierDashboardDTO;
import com.h2gas.model.entity.CategoryProduct;
import com.h2gas.model.entity.Order;
import com.h2gas.model.entity.OrderItem;
import com.h2gas.model.entity.ProductItem;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// HU09: indicadores do mês do fornecedor. Agrega em Java (mês de um fornecedor é pequeno) para não
// depender de funções de data específicas do MySQL e poder testar a regra sem banco real.
// F7 fatia 6: filtros de pagamento, modo, tipo, categoria e produto (ver DashboardFilter).
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
        return buildDashboard(supplierId, month, DashboardFilter.NONE);
    }

    @Transactional(readOnly = true)
    public SupplierDashboardDTO buildDashboard(String supplierId, YearMonth month, DashboardFilter filter) {
        if (month.isAfter(YearMonth.now())) {
            throw new BusinessRuleException("Mês futuro não tem indicadores.");
        }
        Counted current = countedOrders(supplierId, month, filter);
        Counted previous = countedOrders(supplierId, month.minusMonths(1), filter);

        BigDecimal revenue = current.sum(current.orders);
        BigDecimal previousRevenue = previous.sum(previous.orders);
        int total = current.orders.size();

        SupplierDashboardDTO dashboard = new SupplierDashboardDTO();
        dashboard.setMonth(month.toString());
        dashboard.setTotalRevenue(revenue);
        dashboard.setRevenueChangePercent(changePercent(revenue, previousRevenue));
        dashboard.setTotalOrders(total);
        dashboard.setOrdersChangePercent(changePercent(BigDecimal.valueOf(total), BigDecimal.valueOf(previous.orders.size())));
        dashboard.setAverageTicket(total == 0 ? null : revenue.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP));
        dashboard.setReceivable(current.sum(current.orders.stream().filter(o -> o.getStatus() != OrderStatusEnum.DELIVERED).toList()));
        dashboard.setDeliveredOrders(current.orders.stream().filter(o -> o.getStatus() == OrderStatusEnum.DELIVERED).count());
        dashboard.setPendingOrders(orderRepository.countBySupplierIdAndStatus(supplierId, OrderStatusEnum.PENDING));
        dashboard.setOutForDeliveryOrders(orderRepository.countBySupplierIdAndStatus(supplierId, OrderStatusEnum.OUT_FOR_DELIVERY));
        dashboard.setAverageDeliveryMinutes(averageDeliveryMinutes(current.orders));
        dashboard.setRevenueByDay(revenueByDay(current, month));
        dashboard.setSalesByProduct(salesByProduct(current));
        dashboard.setPaymentBreakdown(paymentBreakdown(current));
        dashboard.setSalesByCategory(salesByCategory(current));
        dashboard.setSalesByStoreCategory(salesByStoreCategory(current));
        dashboard.setDeliveryModeBreakdown(deliveryModeBreakdown(current));
        return dashboard;
    }

    // pedidos contados do mês e, com filtro de item, só os itens que batem
    private record Counted(List<Order> orders, DashboardFilter filter) {
        List<OrderItem> items(Order o) {
            if (o.getItems() == null) return List.of();
            return filter.hasItemFilter() ? o.getItems().stream().filter(filter::matchesItem).toList() : o.getItems();
        }

        List<OrderItem> allItems() {
            return orders.stream().flatMap(o -> items(o).stream()).toList();
        }

        // sem filtro de item vale o total do pedido (frete incluso); com filtro, a soma dos itens que batem
        BigDecimal value(Order o) {
            if (!filter.hasItemFilter()) return o.getTotalPrice() == null ? BigDecimal.ZERO : o.getTotalPrice();
            return sumItems(items(o));
        }

        BigDecimal sum(List<Order> list) {
            return list.stream().map(this::value).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    // pedido contado = não cancelado, criado no mês e que passa no filtro
    private Counted countedOrders(String supplierId, YearMonth month, DashboardFilter filter) {
        List<Order> orders = orderRepository.findForDashboard(supplierId, month.atDay(1).atStartOfDay(), month.plusMonths(1).atDay(1).atStartOfDay())
                .stream()
                .filter(o -> o.getStatus() != OrderStatusEnum.CANCELED)
                .filter(filter::matchesOrder)
                .filter(o -> !filter.hasItemFilter() || (o.getItems() != null && o.getItems().stream().anyMatch(filter::matchesItem)))
                .toList();
        return new Counted(orders, filter);
    }

    private static BigDecimal sumItems(List<OrderItem> items) {
        return items.stream().map(i -> i.getTotalPrice() == null ? BigDecimal.ZERO : i.getTotalPrice()).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static double percentOf(BigDecimal part, BigDecimal total) {
        return total.signum() == 0 ? 0 : part.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP).doubleValue();
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

    private List<RevenuePointDTO> revenueByDay(Counted counted, YearMonth month) {
        Map<LocalDate, BigDecimal> byDay = counted.orders.stream().collect(Collectors.groupingBy(
                o -> o.getCreatedAt().toLocalDate(),
                Collectors.reducing(BigDecimal.ZERO, counted::value, BigDecimal::add)));
        return IntStream.rangeClosed(1, month.lengthOfMonth())
                .mapToObj(month::atDay)
                .map(day -> new RevenuePointDTO(day, byDay.getOrDefault(day, BigDecimal.ZERO)))
                .toList();
    }

    private List<ProductSalesDTO> salesByProduct(Counted counted) {
        Map<ProductItem, List<OrderItem>> byProduct = counted.allItems().stream()
                .collect(Collectors.groupingBy(OrderItem::getProduct));
        return byProduct.entrySet().stream()
                .map(entry -> new ProductSalesDTO(
                        entry.getKey().getId(),
                        entry.getKey().getBrand() + " " + entry.getKey().getCategoryProduct().getName(),
                        entry.getValue().stream().mapToLong(OrderItem::getQuantity).sum(),
                        sumItems(entry.getValue())))
                .sorted(Comparator.comparingLong(ProductSalesDTO::getQuantity).reversed().thenComparing(ProductSalesDTO::getProductName))
                .limit(TOP_PRODUCTS)
                .toList();
    }

    // categoria = tipo de produto (Gás, Água…); percentual sobre a receita dos itens contados
    private List<CategorySalesDTO> salesByCategory(Counted counted) {
        List<OrderItem> items = counted.allItems();
        BigDecimal total = sumItems(items);
        Function<OrderItem, Long> typeId = i -> i.getProduct().getCategoryProduct().getProductType().getId();
        return items.stream().collect(Collectors.groupingBy(typeId)).values().stream()
                .map(group -> {
                    BigDecimal revenue = sumItems(group);
                    var type = group.get(0).getProduct().getCategoryProduct().getProductType();
                    return new CategorySalesDTO(type.getId(), type.getName(), group.stream().mapToLong(OrderItem::getQuantity).sum(),
                            revenue, percentOf(revenue, total));
                })
                .sorted(Comparator.comparing(CategorySalesDTO::getRevenue).reversed().thenComparing(CategorySalesDTO::getCategoryName))
                .toList();
    }

    // categorias da loja (P13, 20L…), com o tipo de cada uma
    private List<StoreCategorySalesDTO> salesByStoreCategory(Counted counted) {
        List<OrderItem> items = counted.allItems();
        BigDecimal total = sumItems(items);
        return items.stream().collect(Collectors.groupingBy(i -> i.getProduct().getCategoryProduct().getId())).values().stream()
                .map(group -> {
                    CategoryProduct category = group.get(0).getProduct().getCategoryProduct();
                    BigDecimal revenue = sumItems(group);
                    return new StoreCategorySalesDTO(category.getId(), category.getName(),
                            category.getProductType() == null ? null : category.getProductType().getName(),
                            group.stream().mapToLong(OrderItem::getQuantity).sum(), revenue, percentOf(revenue, total));
                })
                .sorted(Comparator.comparing(StoreCategorySalesDTO::getRevenue).reversed().thenComparing(StoreCategorySalesDTO::getCategoryName))
                .toList();
    }

    private List<PaymentShareDTO> paymentBreakdown(Counted counted) {
        Map<String, List<Order>> byMethod = counted.orders.stream()
                .collect(Collectors.groupingBy(o -> o.getPaymentMethod() == null ? NO_PAYMENT : o.getPaymentMethod()));
        return byMethod.entrySet().stream()
                .map(entry -> new PaymentShareDTO(
                        entry.getKey(),
                        entry.getValue().size(),
                        counted.sum(entry.getValue()),
                        BigDecimal.valueOf(entry.getValue().size() * 100.0 / counted.orders.size()).setScale(1, RoundingMode.HALF_UP).doubleValue()))
                .sorted(Comparator.comparingLong(PaymentShareDTO::getOrders).reversed().thenComparing(PaymentShareDTO::getMethod))
                .toList();
    }

    private List<DeliveryModeShareDTO> deliveryModeBreakdown(Counted counted) {
        Map<DeliveryModeEnum, List<Order>> byMode = counted.orders.stream()
                .collect(Collectors.groupingBy(o -> o.getDeliveryMode() == null ? DeliveryModeEnum.DELIVERY : o.getDeliveryMode()));
        return byMode.entrySet().stream()
                .map(entry -> new DeliveryModeShareDTO(
                        entry.getKey().name(),
                        entry.getValue().size(),
                        counted.sum(entry.getValue()),
                        BigDecimal.valueOf(entry.getValue().size() * 100.0 / counted.orders.size()).setScale(1, RoundingMode.HALF_UP).doubleValue()))
                .sorted(Comparator.comparingLong(DeliveryModeShareDTO::getOrders).reversed().thenComparing(DeliveryModeShareDTO::getMode))
                .toList();
    }
}
