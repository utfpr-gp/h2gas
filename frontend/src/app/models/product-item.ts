import { Supplier } from "./supplier";

export class ProductItem {
    id?: number;
    brand?: string;
    price?: number;
    category?: string;
    type?: string;
    distributorId?: number;
    stockQuantity?: number;
    updatedAt?: Date;
    quantitySelect?: number;
    constructor(
        id?: number,
        brand?: string,
        price?: number,
        category?: string,
        type?: string,
        distributorId?: number,
        stockQuantity?: number,
        updatedAt?: Date
    ) {
        this.id = id;
        this.brand = brand;
        this.price = price;
        this.category = category;
        this.type = type;
        this.distributorId = distributorId;
        this.stockQuantity = stockQuantity;
        this.updatedAt = updatedAt
    }
}