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
}