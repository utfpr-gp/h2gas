// F7 fatia 6: GET /product-items/mine/summary (contado no backend com o estoque mínimo da loja)
export interface ProductStockSummary {
  total: number;
  available: number;
  lowStock: number;
  outOfStock: number;
  lowStockThreshold: number;
}

export type StockFilter = 'all' | 'available' | 'low' | 'out';
