

export class CategoryProduct {
    id?: number;
    name?: string;
    type?: string;
    productTypeId?: number;

    constructor(
        id?: number,
        name?: string,
        type?: string,
        productTypeId?: number
    ) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.productTypeId = productTypeId;
    }
}