export interface Deliverer {
  id: number;
  name: string;
  email: string;
  phoneNumber: string;
  vehicleInfo?: string;
  active: boolean;
  busy: boolean;
}

export interface DelivererCreate {
  name: string;
  email: string;
  phoneNumber: string;
  password: string;
  vehicleInfo?: string;
}
