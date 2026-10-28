// HU20: o navegador guarda só isto; marca, preço e disponibilidade vêm de POST /api/cart/preview
export interface CartLine {
  productId: number;
  quantity: number;
}

export interface BuyNowLine extends CartLine {
  supplierId: string | null;
}

export type CartUnavailableReason = 'NOT_FOUND' | 'STORE_INACTIVE' | 'STORE_CLOSED' | 'OUT_OF_STOCK';

export interface CartPreviewItem {
  productId: number;
  brand?: string | null;
  category?: string | null;
  type?: string | null;
  imageUrl?: string | null;
  quantity: number;
  stockQuantity?: number | null;
  unitPrice?: number | null;
  totalPrice?: number | null;
  available: boolean;
  unavailableReason?: CartUnavailableReason | null;
}

export interface CartPreview {
  supplierId: string | null;
  supplierName: string | null;
  items: CartPreviewItem[];
  subtotal: number;
}

export const UNAVAILABLE_REASON_LABELS: Record<CartUnavailableReason, string> = {
  NOT_FOUND: 'Produto saiu do catálogo',
  STORE_INACTIVE: 'Loja indisponível',
  STORE_CLOSED: 'Loja fechada agora',
  OUT_OF_STOCK: 'Sem estoque suficiente',
};
