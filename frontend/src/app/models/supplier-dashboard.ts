// HU09 (Figura 13): indicadores do mês do fornecedor logado
export interface RevenuePoint {
  day: string;
  revenue: number;
}

export interface ProductSales {
  productId?: number;
  productName: string;
  quantity: number;
  revenue: number;
}

export interface PaymentShare {
  method: string;
  orders: number;
  revenue: number;
  percent: number;
}

export interface CategorySales {
  productTypeId?: number;
  categoryName: string;
  quantity: number;
  revenue: number;
  percent: number;
}

// F7 fatia 6: categorias da loja (P13, 20L) e entrega × retirada
export interface StoreCategorySales {
  categoryId: number;
  categoryName: string;
  typeName?: string;
  quantity: number;
  revenue: number;
  percent: number;
}

export interface DeliveryModeShare {
  mode: 'DELIVERY' | 'PICKUP';
  orders: number;
  revenue: number;
  percent: number;
}

export interface DashboardFilter {
  payment?: 'PIX' | 'DINHEIRO' | 'CREDIT_CARD';
  mode?: 'DELIVERY' | 'PICKUP';
  typeId?: number;
  categoryId?: number;
  productId?: number;
}

export interface SupplierDashboard {
  month: string;
  totalRevenue: number;
  revenueChangePercent: number | null;
  totalOrders: number;
  ordersChangePercent: number | null;
  averageTicket: number | null;
  receivable: number;
  deliveredOrders: number;
  pendingOrders: number;
  outForDeliveryOrders: number;
  averageDeliveryMinutes: number | null;
  revenueByDay: RevenuePoint[];
  salesByProduct: ProductSales[];
  paymentBreakdown: PaymentShare[];
  salesByCategory: CategorySales[];
  salesByStoreCategory?: StoreCategorySales[];
  deliveryModeBreakdown?: DeliveryModeShare[];
}
