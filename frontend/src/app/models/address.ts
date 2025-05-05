import { City } from "./city";
import { User } from "./user";

export class Address {
    id?: number;
    streetName?: string;
    numberHouse?: string;
    complement?: string;
    neighborhood?: string;
    zipCode?: string;
    city?: City;
    user?: User;

    constructor(
        id?: number, streetName?: string, numberHouse?: string,
        complement?: string,neighborhood?: string, zipCode?: string, city?: City, user?: User
    ) {
        this.id = id
        this.streetName = streetName
        this.numberHouse = numberHouse
        this.complement = complement
        this.neighborhood = neighborhood 
        this.zipCode = zipCode
        this.city = city
        this.user = user
    }
}