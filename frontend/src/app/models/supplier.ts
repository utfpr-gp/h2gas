import { ProductTypeChip } from "./product-type";
import { User } from "./user";

export class Supplier {
    id?: string;
    name?: string;
    phone?: string;
    description?: string;
    openingTime?: string;
    closingTime?: string;
    averageDeliveryTime?: string;
    active?: boolean;
    addressId?: number;
    cityId?: number;
    cityName?: string;
    user?: User;
    userId?: string;
    addressStreetName?: string;
    addressNumber?: string;
    addressNeighborhood?: string;
    open?: boolean;
    email?: string;
    cnpj?: string;
    logoUrl?: string | null;
    bannerUrl?: string | null;
    // F7 §5.25: preenchidos só no catálogo do cliente
    averageRating?: number | null;
    reviewCount?: number;
    // HU15: tipos que a loja vende (selos do card); só o catálogo do cliente preenche
    productTypes?: ProductTypeChip[];
    // HU17: entrega e frete por km, calculados no backend até o CEP do endereço ativo
    offersDelivery?: boolean;
    offersPickup?: boolean;
    distanceKm?: number | null;
    deliveryFee?: number | null;
    deliversHere?: boolean | null;
    freeDelivery?: boolean;

    constructor(
        id?: string,
        name?: string,
        phone?: string,
        description?: string,
        openingTime?: string,
        closingTime?: string,
        averageDeliveryTime?: string,
        active?: boolean,
        addressId?: number,
        cityName?: string,
        cityId?: number,
        user?: User,
        userId?: string,
        addressStreetName?: string,
        addressNumber?: string,
        open?: boolean
    ) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.description = description;
        this.openingTime = openingTime;
        this.closingTime = closingTime;
        this.averageDeliveryTime = averageDeliveryTime;
        this.active = active;
        this.addressId = addressId;
        this.cityName = cityName;
        this.user = user;
        this.cityId = cityId;
        this.userId = userId;
        this.addressStreetName = addressStreetName;
        this.addressNumber = addressNumber;
        this.open = open
    }
}