// F7 fatia 6: configurações da loja do fornecedor logado (GET/PUT /suppliers/mine/settings)
export interface SupplierSettings {
  openingTime: string;
  closingTime: string;
  averageDeliveryTime: number;
  lowStockThreshold: number;
  // HU17: como a loja entrega; raio nulo = sem limite; frete = taxa fixa + valor por km × distância
  offersDelivery: boolean;
  offersPickup: boolean;
  deliveryRadiusKm: number | null;
  deliveryBaseFee: number;
  deliveryFeePerKm: number;
}
