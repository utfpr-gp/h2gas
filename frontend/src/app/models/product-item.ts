import { Supplier } from "./supplier";

export class ProductItem {
    id?: number;
    brand?: string;
    price?: number;
    category?: string;
    type?: string;
    supplier?: Supplier;

    constructor(
        id?: number,
        brand?: string,
        price?: number,
        category?: string,
        type?: string,
        supplier?: Supplier
    ) {
        this.id = id
        this.brand = brand
        this.price = price
        this.category = category
        this.type = type
        this.supplier = supplier
    }
}