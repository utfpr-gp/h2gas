// HU09 (Figura 13): indicadores do mês do fornecedor logado
export interface RevenuePoint {
  day: string;
  revenue: number;
}

export interface ProductSales {
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
}
