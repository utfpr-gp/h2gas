import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { NgxMaskDirective } from 'ngx-mask';
import { AddressService } from '../../services/address.service';
import { CityService } from '../../services/city.service';
import { LocationContextService } from '../../services/location-context.service';
import { ViaCepCity, matchCityFromCep } from '../../utils/city-match';
import { safeReturnUrl } from '../../utils/return-url';

interface ViaCepResponse extends ViaCepCity {
  bairro?: string;
  erro?: boolean | string;
}

// HU13: painel reutilizável — visitante informa o CEP; a cidade atendida filtra o catálogo (RN05)
@Component({
  selector: 'app-visitor-cep-panel',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NgxMaskDirective],
  templateUrl: './visitor-cep-panel.component.html',
})
export class VisitorCepPanelComponent {
  form = new FormGroup({ cep: new FormControl('', { nonNullable: true }) });
  loading = false;
  message = '';

  constructor(
    private addressService: AddressService,
    private cityService: CityService,
    private locationContext: LocationContextService,
    private router: Router,
    private route: ActivatedRoute,
  ) {}

  get returnUrl(): string | null {
    return this.route.snapshot.queryParamMap.get('returnUrl');
  }

  submit() {
    const cep = this.form.controls.cep.value.replace(/\D/g, '');
    if (cep.length !== 8) {
      this.message = 'CEP inválido. Digite os 8 números do CEP.';
      return;
    }
    this.message = '';
    this.loading = true;
    this.addressService.buscaViaCep(cep).subscribe({
      next: (response) => {
        const viaCep = response as ViaCepResponse;
        if (!viaCep || viaCep.erro || !viaCep.localidade) {
          this.loading = false;
          this.message = 'CEP não encontrado. Confira os números e tente de novo.';
          return;
        }
        this.matchServedCity(viaCep);
      },
      error: () => {
        this.loading = false;
        this.message = 'Não foi possível consultar o CEP agora. Tente de novo em instantes.';
      },
    });
  }

  private matchServedCity(viaCep: ViaCepResponse) {
    this.cityService.findAll().subscribe({
      next: (cities) => {
        this.loading = false;
        const city = matchCityFromCep(cities, viaCep);
        if (!city) {
          this.message = `Ainda não atendemos ${viaCep.localidade}/${viaCep.uf}. Tente outro CEP.`;
          return;
        }
        this.locationContext.setAddress({
          cityId: city.id!,
          cityName: city.name!,
          label: [viaCep.bairro, city.name].filter(Boolean).join(' · '),
        });
        this.router.navigateByUrl(safeReturnUrl(this.returnUrl) ?? '/inicio');
      },
      error: () => {
        this.loading = false;
        this.message = 'Não foi possível carregar as cidades atendidas. Tente de novo em instantes.';
      },
    });
  }
}
