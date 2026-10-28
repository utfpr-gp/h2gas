import { Supplier } from "./supplier";

export class ProductItem {
    id?: number;
    brand?: string;
    // HU21: preço único do produto; o frete vem da distância (HU17)
    price?: number;
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