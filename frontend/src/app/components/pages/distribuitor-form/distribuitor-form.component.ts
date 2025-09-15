import { Component, OnInit } from '@angular/core';
import { MenuComponent } from '../../menu/menu.component';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgxMaskDirective } from 'ngx-mask';
import { CommonModule } from '@angular/common';
import { ToastrService } from 'ngx-toastr';
import { ActivatedRoute, Router } from '@angular/router';
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
    ReactiveFormsModule,
    CommonModule,
    FormsModule,
    NgxMaskDirective
  ],
  templateUrl: './distribuitor-form.component.html',
  styleUrl: './distribuitor-form.component.css'
})
export class DistribuitorFormComponent implements OnInit {
  registerFormSupplier!: FormGroup;

  registerFormAddress!: FormGroup;


  supplier?: Supplier; 

  cities?: City[];

  address?: Address;

  userLogado?: User;

  isEdit: boolean = false;
  idEdit: number = 0;

  constructor(
    private fb: FormBuilder, 
    private toastr: ToastrService,
    private router: Router,
    private supplierService: SupplierService,
    private cityService: CityService,
    private addressService: AddressService,
    private userService: UserService,
    private routeActive: ActivatedRoute
  ) {
    this.registerFormSupplier = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      phone: ['', [Validators.required]],
      description: ['', [Validators.required]],
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
    this.idEdit = parseInt(this.routeActive.snapshot.paramMap.get('id') || '', 10);

    if (this.idEdit) {
      this.isEdit = true;
      this.getSupplierId(this.idEdit);
    } 
    this.getCities();
    this.userLogado = this.userService.getUserLocalStorage();
    this.buscaAddressIdUser();
  }

  modalAberta = false;

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

  buscarCep() {
    this.addressService.buscaViaCep(this.registerFormAddress.get('zipCode')?.value).subscribe({
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
    if (this.registerFormAddress.valid && !this.address) {
      this.registerFormAddress.get('userId')?.setValue(this.userLogado?.id)
      this.address = this.registerFormAddress.value;
      this.addressService.create(this.address!).subscribe({
        next: (response) => {
          console.log(response);
          this.toastr.success('Seu endereço foi salvo com sucesso');
          this.closeAddressModal();
        }, 
        error: (err) => {
          console.log(err);
          this.toastr.error(`Erro ao salvar endereço: ${err.error.message}`);
        },
      })  
    } else if (this.registerFormAddress.valid && this.address) {
      this.editAddress();
    }
  }

  editAddress() {
    this.addressService.update(this.address?.id!, this.registerFormAddress.value).subscribe({
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

  onSubmit() {
    if (this.registerFormSupplier.valid) {
      console.log('Dados do distribuidor:', this.registerFormSupplier.value);
      // envie para o backend
    }
  }

  getSupplierId(id: number) {
    debugger
    
    this.supplierService.findById(id).subscribe({
      next: (response) => {
        debugger
        console.log('Distribuidor buscado com sucesso!', response);
        this.registerFormSupplier.patchValue(response);
        this.toastr.success('Distribuidor buscado com sucesso!');
      },
      error: (err) => {
        console.error('Erro ao buscar:', err.error.message);
        this.toastr.error(`Erro ao buscar: ${err.error.message}`)
      }
    })
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

  update() {
    debugger
    if (this.registerFormSupplier.invalid) {
      this.registerFormSupplier.markAllAsTouched();
      this.toastr.warning('Conferir informações!!')
      return; 
    }

    this.supplier = this.registerFormSupplier?.value;
    this.supplier!!.addressId = this.address?.id
    debugger

    this.supplierService.update(this.idEdit, this.supplier!!).subscribe({
      next: (response) => {
        debugger
        console.log('Dsitriuidora editada com sucesso!', response);
        this.toastr.success('Distriuidora editada com sucesso!!');
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
