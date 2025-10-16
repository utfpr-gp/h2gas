import { Component, Input, OnInit } from '@angular/core';
// import { UserService } from '../../../services/user.service';
import {FormBuilder, FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import { ToastrService, ToastrModule } from 'ngx-toastr';
// import { User } from '../../../models/user';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { User } from '../../models/user';
import { UserService } from '../../services/user.service';
import { NgxMaskDirective } from 'ngx-mask';
import { AddressService } from '../../services/address.service';
import { City } from '../../models/city';
import { Address } from '../../models/address';
import { CityService } from '../../services/city.service';

@Component({
  selector: 'app-register-user',
  imports: [
    ReactiveFormsModule,
    CommonModule,
    FormsModule,
    NgxMaskDirective
  ],
  templateUrl: './register-user.component.html',
  styleUrl: './register-user.component.css'
})
export class RegisterUserComponent implements OnInit {

  registerForm!: FormGroup;
  user?: User;
  registerFormAddress!: FormGroup;
  address?: Address;

  confirmPassword: string =''; 

  cidadeEncontrada: any;
  cities?: City[];
  isLoadingCep = false;

  @Input()
  userType?: string;

  @Input()
  tittleRegister?: string;


  constructor(
    private fb: FormBuilder, 
    private userService: UserService,
    private toastr: ToastrService,
    private router: Router,
    private addressService: AddressService,
    private cityService: CityService,
  ) {
    this.registerForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      name: ['', [Validators.required, Validators.minLength(3)]],
      phoneNumber: ['', [Validators.required]],
      cpf: [''],
      cnpj: [''],
      password: ['', [Validators.required, Validators.minLength(6)]],
      userType: [''],
    })

    this.registerFormAddress = this.fb.group({
      streetName: ['', [Validators.required]],
      number: ['', [Validators.required]],
      complement: ['', Validators.required],
      neighborhood: ['', Validators.required],
      zipCode: ['', Validators.required],
      cityId: [null, [Validators.required]],
    })
  }
  
  ngOnInit(): void {
    this.user = new User('', '','', '', '', '', '', '');
    this.address = undefined;
    this.getCities();

    console.log(this.address);
    
  }

  modalAberta = false;


  openModal() {
    this.modalAberta = true;
  }

  register() {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      this.toastr.warning('Conferir informações!!')
      return; 
    }
    debugger
    if (!this.address) {
      this.toastr.warning('Por favor adicione seu endereço!!')
      return; 
    }

    debugger
    if (this.registerForm.get('password')?.value == this.confirmPassword) {
      this.registerForm.get('userType')?.setValue(this.userType);

      this.user = this.registerForm?.value
      let objRegister = {
        ...this.user,
        "address": this.address
      }
      console.log(objRegister);
      
      this.userService.createUser(objRegister).subscribe({
        next: (response) => {
          console.log('Usuário criado com sucesso!', response);
          this.toastr.success('Usuário criado com sucesso!');
          this.navigateRoute('/login')
        }, 
        error: (error) => {
          console.error('Erro ao criar usuário:', error);
          this.toastr.error(`Erro ao criar usuário: ${error.error.message}`)
        }
      })
    }    
  }

  buscarCep() {
    this.isLoadingCep = true;
    this.addressService.buscaViaCep(this.registerFormAddress.get('zipCode')?.value).subscribe({
      next: (response: any) => {
        this.cidadeEncontrada = this.cities?.some(c => c.name!.toLowerCase() === response.localidade.toLowerCase());

        if (!this.cidadeEncontrada) {
          this.toastr.warning(`No momento ainda não atendemos a cidade de ${response.localidade}.`);
          return; 
        }
        this.toastr.success('Cep buscado com sucesso');
        this.registerFormAddress.get('streetName')?.setValue(response.logradouro)
        console.log("cidades aqui");
        
        console.log(this.cities);
        this.isLoadingCep = false;
        // this.registerFormAddress.get('cityName')?.setValue(response.localidade)
        this.registerFormAddress.get('neighborhood')?.setValue(response.bairro)

      }, 
      error: (err) => {
        console.log(err);
        this.isLoadingCep = false;
        this.toastr.error(`Erro ao buscar cep: ${err}`)
      }
    })
  }

  addresSave: boolean = false;

  salvarAddress() {
    debugger
    if (!this.cidadeEncontrada) {
      this.toastr.warning(`Infelizmente ainda não atendemos sua cidade.`);
      return;
    }

    if (this.registerFormAddress.valid && !this.address) {
      // this.registerFormAddress.get('userId')?.setValue(this.userLogado?.id)
      this.address = this.registerFormAddress.value;
      this.closeAddressModal();
      this.addresSave = true;

      debugger
      // this.addressService.create(this.address!).subscribe({
      //   next: (response) => {
      //     console.log(response);
      //     this.toastr.success('Seu endereço foi salvo com sucesso');
      //     this.closeAddressModal();
      //   }, 
      //   error: (err) => {
      //     console.log(err);
      //     this.toastr.error(`Erro ao salvar endereço: ${err.error.message}`);
      //   },
      // })  
    }
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

  showAddressModal = false;

  closeAddressModal() {
    this.modalAberta = false;
  }


  isInvalid(field: string): boolean {
    return this.registerForm.controls[field].touched && this.registerForm.controls[field].invalid;
  }


  getErrorMessage(field: string): string {
    const control = this.registerForm.controls[field];

    if (control.errors?.['required']) return 'Este campo é obrigatório.';
    if (control.errors?.['minlength']) return `Mínimo ${control.errors['minlength'].requiredLength} caracteres.`;
    if (control.errors?.['email']) return 'Formato de email inválido.';
    if (control.errors?.['pattern']) return 'Formato inválido.';

    return '';
  }

  navigateRoute(route: string) {
    this.router.navigate([route])
  }

}
