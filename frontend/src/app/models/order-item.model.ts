export interface OrderItemRequest {
    productItemRef: number;
    quantity: number;
}

export interface OrderItemResponse {
    id: number;
    productId: number;
    productBrand: string;
    productNameCategory: string;
    quantity: number;
    unitPrice: number;
    totalPrice: number;
}