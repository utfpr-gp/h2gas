import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { LocationContextService } from '../../../services/location-context.service';
import { AddressFormComponent } from '../../address-form/address-form.component';
import { Address } from '../../../models/address';

@Component({
  selector: 'app-first-access-address',
  imports: [AddressFormComponent],
  templateUrl: './first-access-address.component.html',
  styleUrl: './first-access-address.component.css',
})
export class FirstAccessAddressComponent {
  constructor(
    private locationContext: LocationContextService,
    private router: Router
  ) {}

  onSaved(address: Address) {
    this.locationContext.selectAddress(address);
    this.router.navigate(['/inicio']);
  }
}
