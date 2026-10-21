export type UserType = 'ADMIN' | 'CUSTOMER' | 'SUPPLIER' | 'DELIVERER';

export interface AdminUser {
  id: string;
  name: string;
  email: string;
  phoneNumber?: string;
  userType: UserType;
  enabled: boolean;
}

export interface AdminUserCreate {
  name: string;
  email: string;
  password: string;
  phoneNumber: string;
  userType: UserType;
  cpf?: string;
  cnpj?: string;
  cityId?: number;
  supplierId?: string;
}

export interface AdminUserFilter {
  type?: UserType;
  q?: string;
  page?: number;
  size?: number;
}

export interface AdminDashboard {
  usersByType: Partial<Record<UserType, number>>;
  activeSuppliers: number;
  pendingSuppliers: number;
  cities: number;
  ordersToday: number;
  ordersThisMonth: number;
  ordersByStatus: Partial<Record<string, number>>;
  revenueThisMonth: number;
}
