import { Address } from "./address";
import { City } from "./city";
import { User } from "./user";

export class Supplier {
    id?: number;
    name?: string;
    phone?: string;
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

    constructor(
        id?: number,
        name?: string,
        phone?: string,
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
        addressNumber?: string
    ) {
        this.id = id;
        this.name = name;
        this.phone = phone;
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
    }
}