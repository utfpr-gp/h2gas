import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { NgxMaskDirective } from 'ngx-mask';
import { AddressService } from '../../services/address.service';
import { CityService } from '../../services/city.service';
import { Address } from '../../models/address';
import { City } from '../../models/city';

@Component({
  selector: 'app-address-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NgxMaskDirective],
  templateUrl: './address-form.component.html'
})
export class AddressFormComponent implements OnInit {
  @Output() saved = new EventEmitter<Address>();

  form: FormGroup;
  cities?: City[];
  cidadeEncontrada = false;
  isLoadingCep = false;
  isSaving = false;

  readonly sugestoesRotulo = ['Casa', 'Trabalho'];

  constructor(
    private fb: FormBuilder,
    private addressService: AddressService,
    private cityService: CityService,
    private toastr: ToastrService
  ) {
    this.form = this.fb.group({
      addressName: [''],
      zipCode: ['', [Validators.required]],
      streetName: ['', [Validators.required]],
      neighborhood: ['', [Validators.required]],
      number: ['', [Validators.required]],
      complement: [''],
      cityId: [null, [Validators.required]],
    });
  }

  ngOnInit(): void {
    this.cityService.findAll().subscribe({
      next: (response) => (this.cities = response),
      error: () => this.toastr.error('Erro ao buscar cidades'),
    });
  }

  usarRotulo(rotulo: string) {
    this.form.get('addressName')?.setValue(rotulo);
  }

  buscarCep() {
    const cep = this.form.get('zipCode')?.value;
    if (!cep) {
      return;
    }

    this.isLoadingCep = true;
    this.addressService.buscaViaCep(cep).subscribe({
      next: (response: any) => {
        this.isLoadingCep = false;
        if (!response || response.erro || !response.localidade) {
          this.cidadeEncontrada = false;
          this.toastr.error('CEP não encontrado. Confira e tente novamente.');
          return;
        }

        const cidade = this.cities?.find(
          (c) => c.name!.toLowerCase() === response.localidade.toLowerCase()
        );
        this.cidadeEncontrada = !!cidade;

        if (!cidade) {
          this.toastr.warning(`No momento ainda não atendemos a cidade de ${response.localidade}.`);
          return;
        }

        this.form.get('streetName')?.setValue(response.logradouro);
        this.form.get('neighborhood')?.setValue(response.bairro);
        this.form.get('cityId')?.setValue(cidade.id);
        this.toastr.success('Cep buscado com sucesso');
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

    const cidade = this.cities?.find((c) => c.id === this.form.get('cityId')?.value);
    const payload: Address = { ...this.form.value };

    this.isSaving = true;
    this.addressService.create(payload).subscribe({
      next: (response) => {
        this.isSaving = false;
        this.toastr.success('Endereço salvo com sucesso!');
        this.saved.emit({ ...payload, ...response, cityName: cidade?.name });
      },
      error: (err) => {
        this.isSaving = false;
        this.toastr.error(`Erro ao salvar endereço: ${err.error?.message || err.message}`);
      },
    });
  }
}
