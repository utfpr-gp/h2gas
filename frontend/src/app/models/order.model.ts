import { OrderItemRequest, OrderItemResponse } from "./order-item.model";
import { OrderStatus } from "./order-status.enum";

export interface OrderRequest {
    customerUuid: string;
    supplierRef: string;
    paymentMethod: string;
    note: string;
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