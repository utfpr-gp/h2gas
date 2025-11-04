

export class CategoryProduct {
    id?: number;
    name?: string;
    type?: string;
    productTypeId?: number;
    supplierId?: string;

    constructor(
        id?: number,
        name?: string,
        type?: string,
        productTypeId?: number,
        supplierId?: string
    ) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.productTypeId = productTypeId;
        this.supplierId = supplierId;
    }
}