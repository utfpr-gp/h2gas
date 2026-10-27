import { ProductTypeChip } from './product-type';

// HU13: GET /suppliers/{id}/storefront — o que a vitrine pública mostra de uma loja (sem e-mail, CNPJ, telefone e endereço completo)
export interface Storefront {
  id: string;
  name: string;
  description?: string | null;
  logoUrl?: string | null;
  bannerUrl?: string | null;
  openingTime?: string | null;
  closingTime?: string | null;
  averageDeliveryTime?: number | null;
  open: boolean;
  cityId: number;
  cityName: string;
  addressNeighborhood?: string | null;
  averageRating?: number | null;
  reviewCount: number;
  // HU15: tipos que a loja vende, em ordem alfabética; vazia quando não há categorias
  productTypes?: ProductTypeChip[];
}
