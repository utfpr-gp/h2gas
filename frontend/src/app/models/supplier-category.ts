// F7 fatia 6: GET /category-products/mine (categoria da loja com o tipo e a contagem de produtos)
export interface SupplierCategory {
  id: number;
  name: string;
  type: string;
  productTypeId: number;
  typeIcon?: string;
  productCount: number;
}

export interface CategoryRequest {
  name: string;
  productTypeId: number;
}
