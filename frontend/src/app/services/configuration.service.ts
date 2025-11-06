import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class ConfigurationService {

  constructor() { }

  private STORAGE_KEY = 'app_settings';

  saveSettings(settings: any) {
    localStorage.setItem(this.STORAGE_KEY, JSON.stringify(settings));
  }

  getSettings(): any {
    const data = localStorage.getItem(this.STORAGE_KEY);
    return data ? JSON.parse(data) : null;
  }

  clearSettings() {
    localStorage.removeItem(this.STORAGE_KEY);
  }
}
