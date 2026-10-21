// F7 fatia 6: GET/PUT /suppliers/mine/profile (dados da loja do fornecedor logado)
export interface SupplierProfile {
  name: string;
  email?: string;
  phone: string;
  description?: string;
  cnpj?: string;
  logoUrl?: string | null;
  addressId?: number;
  addressStreetName?: string;
  addressNumber?: string;
  addressNeighborhood?: string;
  cityName?: string;
  cityState?: string;
}

export interface SupplierProfileRequest {
  name: string;
  phone: string;
  description?: string;
}

// mesmo limite do backend (multipart de 1 MB)
export const LOGO_MAX_BYTES = 1_048_576;
export const LOGO_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
