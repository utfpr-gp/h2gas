import { Component, OnInit } from '@angular/core';
import { UserService } from '../../../services/user.service';
import { User } from '../../../models/user';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { NgxMaskDirective } from 'ngx-mask';
import { Address } from '../../../models/address';
import { CityService } from '../../../services/city.service';
import { AddressService } from '../../../services/address.service';
import { ToastrService } from 'ngx-toastr';
import { City } from '../../../models/city';

@Component({
  selector: 'app-customer-panel',
  imports: [
    ReactiveFormsModule,
    CommonModule,
    FormsModule,
    // NgxMaskDirective
  ],
  templateUrl: './customer-panel.component.html',
  styleUrl: './customer-panel.component.css'
})
export class CustomerPanelComponent implements OnInit {


  constructor(
    private userService: UserService,
    private fb: FormBuilder,
    private cityService: CityService,
    private addressService: AddressService,
    private toastr: ToastrService,
  ) {
    this.registerFormAddress = this.fb.group({
      streetName: ['', [Validators.required]],
      number: ['', [Validators.required]],
      complement: ['', Validators.required],
      neighborhood: ['', Validators.required],
      zipCode: ['', Validators.required],
      cityId: ['', [Validators.required]],
      userId: [null],
    })

    this.registerFormUser = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      name: ['', [Validators.required, Validators.minLength(3)]],
      phoneNumber: ['', [Validators.required]],
      cnpj: [''],
    })
  }


  ngOnInit(): void {
    this.userLogado = this.userService.getUserLocalStorage()
    console.log(this.userLogado);
    this.registerFormUser.patchValue(this.userLogado!);
    console.log(this.registerFormUser.value);

    this.getCities();
    this.buscaAddressIdUser();
  }

  userLogado?: User;
  showAddressModal = false;
  modalAberta = false;
  registerFormAddress!: FormGroup;
  registerFormUser!: FormGroup;
  address?: Address;
  cities?: City[];

  openModal() {
    this.modalAberta = true;
    if (this.address) {
      this.registerFormAddress.patchValue(this.address)
    }
  }

  openAddressModal() {
    this.showAddressModal = true;
  }

  closeAddressModal() {
    this.showAddressModal = false;
  }

  update() {

    if (this.registerFormUser.invalid) {
      this.registerFormUser.markAllAsTouched();
      this.toastr.warning('Conferir informações!!')
      return;
    }

    // this.userLogado = this.registerFormUser?.value;
    let userEdit: User = this.registerFormUser?.value
    // this.userLogado!!.addressId = this.address?.id


    this.userService.updateUser(this.userLogado?.id!, userEdit).subscribe({
      next: (response) => {
        console.log(response);
        localStorage.setItem('user', JSON.stringify(response))
        this.toastr.success("Informações editadas com sucesso!")
      },
      error: (err) => {
        console.error(err);
        this.toastr.error(`Erro ao editar informações de Usuário: ${err.error.message}`);
      },
    })
  }

  salvarAddress() {
    if (this.registerFormAddress.valid && !this.address) {
      this.registerFormAddress.get('userId')?.setValue(this.userLogado?.id);
      const payload = { ...this.registerFormAddress.value };
      this.addressService.create(payload as Address).subscribe({
        next: (response) => {
          console.log(response);
          this.address = response;
          this.toastr.success('Seu enderelo foi salvo com sucesso');
          this.closeAddressModal();
        },
        error: (err) => {
          console.log(err);
          this.toastr.error(`Erro ao salvar endereço: ${err.error.message}`);
        },
      });
    } else if (this.registerFormAddress.valid && this.address) {
      this.editAddress();
    }
  }

  editAddress() {
    const id = this.address?.id;
    if (id == null) {
      this.toastr.error('Endereço sem identificador; salve como novo ou recarregue a página.');
      return;
    }
    this.addressService.update(id, this.registerFormAddress.value).subscribe({
      next: (response) => {
        console.log(response);
        this.toastr.success('Seu endereço foi editado com sucesso');
        this.closeAddressModal();
      },
      error: (err) => {
        console.log(err);
        this.toastr.error(`Erro ao editar endereço: ${err.error.message}`);
      },
    })
  }


  getCities() {
    this.cityService.findAll().subscribe({
      next: (response) => {
        console.log('Buscado com sucesso', response);
        this.cities = response;
        console.log(response);
      },
      error: (err) => {
        console.error('Erro ao buscar:', err.error.message);
      }
    })
  }

  buscarCep() {
    this.addressService.buscaViaCep(this.registerFormAddress.get('zipCode')?.value).subscribe({
      next: (response: any) => {
        console.log(response);
        this.toastr.success('Cep buscado com sucesso');
        this.registerFormAddress.get('streetName')?.setValue(response.logradouro);
        const found = this.cities?.find(
          (c) => c.name?.toLowerCase() === String(response.localidade).toLowerCase()
        );
        if (found?.id != null) {
          this.registerFormAddress.get('cityId')?.setValue(found.id);
        }
        this.registerFormAddress.get('neighborhood')?.setValue(response.bairro);

      },
      error: (err) => {
        console.log(err);
        this.toastr.error(`Erro ao buscar cep: ${err}`)
      }
    })
  }


  buscaAddressIdUser() {
    this.addressService.findByUserId(this.userLogado?.id!!).subscribe({
      next: (response) => {
        console.log(response);
        this.address = response;
        console.log("aeee");

        console.log(this.address);

      },
      error: (err) => {
        console.log(err);
        this.toastr.error(`Erro ao buscar endereço: ${err.error.message}`);
      },
    })
  }

}
