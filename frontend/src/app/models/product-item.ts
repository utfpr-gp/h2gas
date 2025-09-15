import { Supplier } from "./supplier";

export class ProductItem {
    id?: number;
    brand?: string;
    price?: number;
    category?: string;
    type?: string;
    supplierId: number = 0;
    stockQuantity?: number;
    updatedAt?: Date;
    quantitySelect?: number;
}