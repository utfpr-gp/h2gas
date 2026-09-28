import { Injectable, signal } from '@angular/core';

export interface ActiveAddress {
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

  clear() {
    this.addressSignal.set(null);
    localStorage.removeItem(this.STORAGE_KEY);
  }

  private readFromStorage(): ActiveAddress | null {
    const raw = localStorage.getItem(this.STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  }
}
