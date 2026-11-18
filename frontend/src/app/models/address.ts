import { User } from "./user";

export class Address {
    id?: number;
    streetName?: string;
    number?: string;
    complement?: string;
    neighborhood?: string;
    zipCode?: string;
    cityName?: string;
    cityId?: number;
    user?: User;
    userId?: string;
    addressName?: string;

    constructor(
        id?: number, streetName?: string, numberHouse?: string,
        complement?: string,neighborhood?: string, zipCode?: string, 
        cityName?: string, user?: User, addressName?: string
    ) {
        this.id = id
        this.streetName = streetName
        this.number = numberHouse
        this.complement = complement
        this.neighborhood = neighborhood 
        this.zipCode = zipCode
        this.cityName = cityName
        this.user = user
        this.addressName = addressName
    }
}