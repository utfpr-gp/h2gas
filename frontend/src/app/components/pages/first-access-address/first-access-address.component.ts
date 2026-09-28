import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { NgxMaskDirective } from 'ngx-mask';
import { AddressService } from '../../../services/address.service';
import { CityService } from '../../../services/city.service';
import { UserService } from '../../../services/user.service';
import { LocationContextService } from '../../../services/location-context.service';
import { City } from '../../../models/city';

@Component({
  selector: 'app-first-access-address',
  imports: [ReactiveFormsModule, CommonModule, FormsModule, NgxMaskDirective],
  templateUrl: './first-access-address.component.html',
  styleUrl: './first-access-address.component.css',
})
export class FirstAccessAddressComponent implements OnInit {
  form!: FormGroup;
  cities?: City[];
  cidadeEncontrada = false;
  isLoadingCep = false;
  isSaving = false;

  constructor(
    private fb: FormBuilder,
    private addressService: AddressService,
    private cityService: CityService,
    private userService: UserService,
    private locationContext: LocationContextService,
    private router: Router,
    private toastr: ToastrService
  ) {
    this.form = this.fb.group({
      zipCode: ['', [Validators.required]],
      streetName: ['', [Validators.required]],
      neighborhood: ['', [Validators.required]],
      number: ['', [Validators.required]],
      complement: [''],
      cityId: [null, [Validators.required]],
    });
  }

  ngOnInit(): void {
    this.getCities();
  }

  getCities() {
    this.cityService.findAll().subscribe({
      next: (response) => (this.cities = response),
      error: (err) => this.toastr.error('Erro ao buscar cidades'),
    });
  }

  buscarCep() {
    const cep = this.form.get('zipCode')?.value;
    if (!cep) {
      return;
    }

    this.isLoadingCep = true;
    this.addressService.buscaViaCep(cep).subscribe({
      next: (response: any) => {
        const cidade = this.cities?.find(
          (c) => c.name!.toLowerCase() === response.localidade.toLowerCase()
        );
        this.cidadeEncontrada = !!cidade;

        if (!cidade) {
          this.toastr.warning(`No momento ainda não atendemos a cidade de ${response.localidade}.`);
          this.isLoadingCep = false;
          return;
        }

        this.form.get('streetName')?.setValue(response.logradouro);
        this.form.get('neighborhood')?.setValue(response.bairro);
        this.form.get('cityId')?.setValue(cidade.id);
        this.toastr.success('Cep buscado com sucesso');
        this.isLoadingCep = false;
      },
      error: (err) => {
        this.isLoadingCep = false;
        this.toastr.error(`Erro ao buscar cep: ${err}`);
      },
    });
  }

  salvar() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.toastr.warning('Confira as informações do endereço!');
      return;
    }

    const user = this.userService.getUserLocalStorage();
    const cidade = this.cities?.find((c) => c.id === this.form.get('cityId')?.value);

    const payload = {
      ...this.form.value,
      userId: user?.id,
    };

    this.isSaving = true;
    this.addressService.create(payload).subscribe({
      next: () => {
        this.isSaving = false;
        this.locationContext.setAddress({
          cityId: cidade!.id!,
          cityName: cidade!.name!,
          label: `${this.form.get('streetName')?.value}, ${this.form.get('number')?.value} · ${cidade!.name}`,
        });
        this.toastr.success('Endereço salvo com sucesso!');
        this.router.navigate(['/inicio']);
      },
      error: (err) => {
        this.isSaving = false;
        this.toastr.error(`Erro ao salvar endereço: ${err.error?.message || err.message}`);
      },
    });
  }
}
