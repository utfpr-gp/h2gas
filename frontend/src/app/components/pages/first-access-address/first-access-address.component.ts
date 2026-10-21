import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { LocationContextService } from '../../../services/location-context.service';
import { AddressFormComponent } from '../../address-form/address-form.component';
import { Address } from '../../../models/address';
import { safeReturnUrl } from '../../../utils/return-url';

@Component({
  selector: 'app-first-access-address',
  imports: [AddressFormComponent],
  templateUrl: './first-access-address.component.html',
  styleUrl: './first-access-address.component.css',
})
export class FirstAccessAddressComponent {
  constructor(
    private locationContext: LocationContextService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  onSaved(address: Address) {
    this.locationContext.selectAddress(address);
    // HU13: veio do checkout → volta para ele
    const returnUrl = safeReturnUrl(this.route.snapshot.queryParamMap.get('returnUrl'));
    if (returnUrl) {
      this.router.navigateByUrl(returnUrl);
    } else {
      this.router.navigate(['/inicio']);
    }
  }
}
