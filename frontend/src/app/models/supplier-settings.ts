// F7 fatia 6: configurações da loja do fornecedor logado (GET/PUT /suppliers/mine/settings)
export interface SupplierSettings {
  openingTime: string;
  closingTime: string;
  averageDeliveryTime: number;
  lowStockThreshold: number;
}
