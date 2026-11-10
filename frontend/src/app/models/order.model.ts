import { OrderItemRequest, OrderItemResponse } from "./order-item.model";
import { OrderStatus } from "./order-status.enum";

export type DeliveryMode = 'DELIVERY' | 'PICKUP';

export interface OrderPreviewRequest {
    items: OrderItemRequest[];
    deliveryMode: DeliveryMode;
}

export interface OrderPreviewItem {
    productItemRef: number;
    quantity: number;
    unitPrice: number;
    totalPrice: number;
}

export interface OrderPreview {
    items: OrderPreviewItem[];
    subtotal: number;
}

export interface ReorderItem {
    productId?: number;
    productBrand?: string;
    category?: string;
    type?: string;
    imageUrl?: string;
    supplierId?: string;
    quantity: number;
    price?: number;
    unavailableReason?: 'PRODUTO_REMOVIDO' | 'SEM_ESTOQUE';
}

export interface ReorderPreview {
    availableItems: ReorderItem[];
    unavailableItems: ReorderItem[];
}

export interface OrderRequest {
    customerUuid: string;
    supplierRef: string;
    paymentMethod: string;
    note: string;
    addressId?: number;
    deliveryMode?: DeliveryMode;
    items: OrderItemRequest[]
}

export interface OrderResponse {
    id: string;
    orderDate: Date;
    totalPrice: number;
    paymentMethod: string;
    note: string;
    deliveryDate: Date;
    createdAt: Date;
    updatedAt: Date;
    customerName: string;
    supplierName: string;
    customerId: string;
    supplierId: number;
    orderItems: OrderItemResponse[];
    status: OrderStatus;
    streetName:string;
    number:string;
    complement:string;
    neighborhood:string;
    deliveryMode?: DeliveryMode;
    customerPhone?: string;
    // HU25: atalho de WhatsApp; o do entregador só vem com o pedido em rota
    supplierPhone?: string | null;
    delivererPhone?: string | null;
    delivererName?: string;
    cancellationReason?: string;
    cityName?: string;
    deliveredAt?: string;
    lastRefusalReason?: string;
    lastRefusalBy?: string;
    // HU24: avaliação feita pelo cliente; nula enquanto não avaliou
    review?: OrderReview | null;
    // HU28: o backend já calcula (RN01); total = subtotal + frete
    subtotal?: number;
    deliveryFee?: number;
    deliveryDistanceKm?: number | null;
}

// HU24
export interface OrderReview {
    storeRating: number;
    delivererRating?: number | null;
    comment?: string | null;
}

export interface CancelOrderResponse {
    status: string;
    customerNotified: boolean;
}