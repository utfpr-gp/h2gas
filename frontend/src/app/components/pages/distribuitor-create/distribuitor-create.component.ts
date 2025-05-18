import { Component, OnInit } from '@angular/core';
import { MenuComponent } from '../../menu/menu.component';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgxMaskDirective } from 'ngx-mask';
import { CommonModule } from '@angular/common';
import { ToastrService } from 'ngx-toastr';
import { Router } from '@angular/router';
import { SupplierService } from '../../../services/supplier.service';
import { Supplier } from '../../../models/supplier';
import { CityService } from '../../../services/city.service';
import { City } from '../../../models/city';
import { Address } from '../../../models/address';
import { AddressService } from '../../../services/address.service';
import { UserService } from '../../../services/user.service';
import { User } from '../../../models/user';

@Component({
  selector: 'app-distribuitor-create',
  imports: [
    MenuComponent,
    ReactiveFormsModule,
    CommonModule,
    FormsModule,
    NgxMaskDirective
  ],
  templateUrl: './distribuitor-create.component.html',
  styleUrl: './distribuitor-create.component.css'
})
export class DistribuitorCreateComponent implements OnInit {
  registerFormSupplier!: FormGroup;

  registerFormAddress!: FormGroup;


  supplier?: Supplier; 

  cities?: City[];

  address?: Address;

  userLogado?: User;


  constructor(
    private fb: FormBuilder, 
    private toastr: ToastrService,
    private router: Router,
    private supplierService: SupplierService,
    private cityService: CityService,
    private addressSevice: AddressService,
    private userService: UserService
  ) {
    this.registerFormSupplier = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      phone: ['', [Validators.required]],
      active: [false],
      cityId: [null, [Validators.required]],
      addressId: [null],
      openingTime: ['', Validators.required],
      closingTime: ['', Validators.required],
      averageDeliveryTime: [null, [Validators.required, Validators.min(1)]],
      userId: [null]
    })


    this.registerFormAddress = this.fb.group({
      streetName: ['', [Validators.required]],
      number: ['', [Validators.required]],
      complement: ['', Validators.required],
      neighborhood: ['', Validators.required],
      zipCode: ['', Validators.required],
      cityName: ['', [Validators.required]],
      userId: [null],
    })
  }

  // selectedAddress: boolean = null;

  showAddressModal = false;

  ngOnInit(): void {
    this.getCities();
    this.userLogado = this.userService.getUserLocalStorage();
    console.log(this.userLogado);
    console.log("testando local storage");
    this.buscaAddressIdUser();
  }

  modalAberta = false;

  openModal() {
    this.modalAberta = true;
  }


  openAddressModal() {
    this.showAddressModal = true;
  }

  closeAddressModal() {
    this.showAddressModal = false;
    // this.tempAddress = { street: '', city: '', state: '', cep: '' };
  }

  // addAddress() {
  //   // Aqui você pode substituir por uma chamada ao seu backend (ex: this.addressService.create(this.tempAddress))
  //   // Para fins de exemplo, vamos simular que salvou e retornou um ID:

  //   const fakeSavedAddress = { id: 123, ...this.tempAddress }; // substitua pelo retorno real da API
  //   this.selectedAddress = fakeSavedAddress;
  //   this.registerFormSupplier.patchValue({ addressId: fakeSavedAddress.id });

  //   this.closeAddressModal();
  // }

  buscarCep() {
    this.addressSevice.buscaViaCep(this.registerFormAddress.get('zipCode')?.value).subscribe({
      next: (response: any) => {
        console.log(response);
        this.toastr.success('Cep buscado com sucesso');
        this.registerFormAddress.get('streetName')?.setValue(response.logradouro)
        this.registerFormAddress.get('cityName')?.setValue(response.localidade)
        this.registerFormAddress.get('neighborhood')?.setValue(response.bairro)

      }, 
      error: (err) => {
        console.log(err);
        this.toastr.error(`Erro ao buscar cep: ${err}`)
      }
    })
  }

  salvarAddress() {
    debugger
    if (this.registerFormAddress.valid) {
      this.registerFormAddress.get('userId')?.setValue(this.userLogado?.id)
      this.address = this.registerFormAddress.value;
      this.addressSevice.create(this.address!!).subscribe({
        next: (response) => {
          console.log(response);
          this.toastr.success('Seu enderelo foi salvo com sucesso');
          this.closeAddressModal();
        }, 
        error: (err) => {
          console.log(err);
          this.toastr.error(`Erro ao salvar endereço: ${err.error.message}`);
        },
      })  
    }
  }

  buscaAddressIdUser() {
    this.addressSevice.findByUserId(this.userLogado?.id!!).subscribe({
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

  onSubmit() {
    if (this.registerFormSupplier.valid) {
      console.log('Dados do distribuidor:', this.registerFormSupplier.value);
      // envie para o backend
    }
  }


  register() {
    debugger
    if (this.registerFormSupplier.invalid) {
      this.registerFormSupplier.markAllAsTouched();
      this.toastr.warning('Conferir informações!!')
      return; 
    }
    debugger

    this.supplier = this.registerFormSupplier?.value;
    this.supplier!!.addressId = this.address?.id
    this.supplier!!.userId = this.userLogado?.id
    

    this.supplierService.create(this.supplier!!).subscribe({
      next: (response) => {
        debugger
        console.log('Usuário criado com sucesso!', response);
        this.toastr.success('Usuário criado com sucesso!');
        this.navigateRoute('/management-distributor')
      },
      error: (err) => {
        console.error('Erro ao buscar:', err.error.message);
        this.toastr.error(`Erro ao buscar cidades: ${err.error.message}`)
      }
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

  navigateRoute(route: string) {
    this.router.navigate([route]);
  }

}
