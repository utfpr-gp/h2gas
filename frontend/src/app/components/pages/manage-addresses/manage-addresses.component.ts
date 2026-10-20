import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastrService } from 'ngx-toastr';
import { AddressService } from '../../../services/address.service';
import { UserService } from '../../../services/user.service';
import { LocationContextService } from '../../../services/location-context.service';
import { AddressFormComponent } from '../../address-form/address-form.component';
import { Address } from '../../../models/address';
import { backendMessage } from '../../../utils/backend-message';

@Component({
  selector: 'app-manage-addresses',
  standalone: true,
  imports: [CommonModule, AddressFormComponent],
  templateUrl: './manage-addresses.component.html'
})
export class ManageAddressesComponent implements OnInit {
  addresses: Address[] = [];
  showForm = false;
  private userId?: string;

  constructor(
    private addressService: AddressService,
    private userService: UserService,
    private locationContext: LocationContextService,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    this.userId = this.userService.getUserLocalStorage()?.id;
    this.load();
  }

  load() {
    this.addressService.findAllByUser(this.userId!).subscribe({
      next: (response) => (this.addresses = response),
      error: (err) => this.toastr.error(`Erro ao buscar endereços: ${backendMessage(err)}`),
    });
  }

  get activeAddress(): Address | undefined {
    return this.addresses.find((a) => this.isActive(a));
  }

  get otherAddresses(): Address[] {
    return this.addresses.filter((a) => !this.isActive(a));
  }

  // endereço ativo salvo antes da HU04 não tem addressId: vale o mais recente, como no guard da HU01
  isActive(address: Address): boolean {
    const activeId = this.locationContext.current()?.addressId;
    return activeId != null ? address.id === activeId : address.id === this.addresses[0]?.id;
  }

  select(address: Address) {
    this.locationContext.selectAddress(address);
    this.toastr.success(`Entregando em: ${address.addressName || address.streetName}`);
  }

  onAddressSaved(_address: Address) {
    this.showForm = false;
    this.load();
  }

  iconFor(address: Address): string {
    const rotulo = (address.addressName ?? '').toLowerCase();
    if (rotulo.includes('casa')) return 'home';
    if (rotulo.includes('trabalho')) return 'work';
    return 'place';
  }
}
