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