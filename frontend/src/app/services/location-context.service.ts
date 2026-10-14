import { Injectable, signal } from '@angular/core';
import { Address } from '../models/address';

export interface ActiveAddress {
  addressId?: number;
  cityId: number;
  cityName: string;
  label: string;
}

@Injectable({ providedIn: 'root' })
export class LocationContextService {
  private readonly STORAGE_KEY = 'h2gas.activeAddress';
  private addressSignal = signal<ActiveAddress | null>(this.readFromStorage());
  readonly current = this.addressSignal.asReadonly();

  setAddress(address: ActiveAddress) {
    this.addressSignal.set(address);
    localStorage.setItem(this.STORAGE_KEY, JSON.stringify(address));
  }

  selectAddress(address: Address) {
    this.setAddress({
      addressId: address.id,
      cityId: address.cityId!,
      cityName: address.cityName!,
      label: `${address.streetName}, ${address.number} · ${address.cityName}`,
    });
  }

  clear() {
    this.addressSignal.set(null);
    localStorage.removeItem(this.STORAGE_KEY);
  }

  private readFromStorage(): ActiveAddress | null {
    const raw = localStorage.getItem(this.STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  }
}
