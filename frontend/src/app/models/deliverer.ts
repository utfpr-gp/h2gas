export interface Deliverer {
  id: number;
  name: string;
  email: string;
  phoneNumber: string;
  vehicleInfo?: string;
  active: boolean;
  busy: boolean;
  // HU24: média das notas dadas ao entregador
  averageRating?: number | null;
  ratingCount?: number;
}

export interface DelivererCreate {
  name: string;
  email: string;
  phoneNumber: string;
  password: string;
  vehicleInfo?: string;
}
