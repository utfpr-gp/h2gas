import { Supplier } from "./supplier";

export class ProductItem {
    id?: number;
    brand?: string;
    priceDelivery?: number;
    pricePickup?: number;
    // HU10: computado pela API (preço de entrega == preço de retirada)
    freeDelivery?: boolean;
    category?: string;
    type?: string;
    supplierId: string = '';
    stockQuantity?: number;
    updatedAt?: Date;
    quantitySelect?: number;
    supllierName?: string;
    imageUrl?: string;
    description?: string;
}