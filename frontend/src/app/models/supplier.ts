import { Address } from "./address";
import { City } from "./city";
import { User } from "./user";

export class supplier {
    id?: number;
    name?: string;
    phone?: string;
    workingHours?: string;
    averageDeliveryTime?: string;
    active?: boolean;
    address?: Address;
    city?: City;
    user?: User;

    constructor(
        id?: number,name?: string,
        phone?: string, workingHours?: string, averageDeliveryTime?: string,
        active?: boolean, address?: Address, city?: City, user?: User
    ) {
        this.id = id
        this.name = name
        this.phone = phone
        this.workingHours = workingHours
        this.averageDeliveryTime = averageDeliveryTime
        this.active = active
        this.address = address
        this.city = city
        this.user = user
    }
}