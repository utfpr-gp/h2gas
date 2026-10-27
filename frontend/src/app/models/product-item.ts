import { Supplier } from "./supplier";

export class ProductItem {
    id?: number;
    brand?: string;
    // HU10: contrato do fornecedor (price = valor de retirada; shippingFee = acréscimo da entrega)
    price?: number;
    shippingFee?: number;
    // leitura (cards, carrinho e checkout): derivados dos dois campos acima
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
    // HU15 §5.5: logo da loja que vende, para o avatar redondo do card
    supplierLogoUrl?: string | null;
    imageUrl?: string;
    description?: string;
}