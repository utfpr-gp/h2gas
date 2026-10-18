// HU07 (Figura 17c): histórico do entregador, entregues e recusadas
export type DeliveryHistoryKind = 'DELIVERED' | 'REFUSED';

export interface DeliveryHistoryItem {
  orderId: string;
  kind: DeliveryHistoryKind;
  occurredAt: string;
  streetName?: string;
  number?: string;
  neighborhood?: string;
  cityName?: string;
  itemsSummary: string;
  paymentMethod?: string;
  totalPrice?: number | null;
}

export interface DeliveryHistory {
  deliveredToday: number;
  items: DeliveryHistoryItem[];
}
