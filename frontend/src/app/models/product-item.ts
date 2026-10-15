import { Supplier } from "./supplier";

export class ProductItem {
    id?: number;
    brand?: string;
    price?: number;
    priceDelivery?: number;
    pricePickup?: number;
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