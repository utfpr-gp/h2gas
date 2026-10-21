import { DeliveryMode } from './order.model';

// F7 fatia 6: filtros e resumo do histórico de pedidos finalizados do fornecedor
export type HistoryStatus = 'delivered' | 'canceled';
export type PaymentMethod = 'PIX' | 'DINHEIRO' | 'CREDIT_CARD';

export interface OrderHistoryFilter {
  status?: HistoryStatus;
  from?: string;   // AAAA-MM-DD
  to?: string;     // AAAA-MM-DD, inclusivo
  payment?: PaymentMethod;
  mode?: DeliveryMode;
  q?: string;
}

export interface OrderHistorySummary {
  delivered: number;
  canceled: number;
  revenue: number;
  averageTicket: number | null;
}
