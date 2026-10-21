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
}
