import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { UserService } from '../../../services/user.service';
import { ToastrService } from 'ngx-toastr';
import { CityService } from '../../../services/city.service';
import { City } from '../../../models/city';
import { DeleteConfirmationModalComponent } from '../../delete-confirmation-modal/delete-confirmation-modal.component';

@Component({
  selector: 'app-city-register',
  imports: [
    ReactiveFormsModule,
    CommonModule,
    FormsModule,
    DeleteConfirmationModalComponent
  ],
  templateUrl: './city-register.component.html',
  styleUrl: './city-register.component.css'
})
export class CityRegisterComponent implements OnInit {
  isMenuOpen = false;

  cityForm!: FormGroup;
  city?: City;

  cityTabble?: City[];
  idSelectCity?: number;
  cityDeleteName: string = "";

  constructor(
    private router: Router,
    private fb: FormBuilder, 
    private cityService: CityService,
    private toastr: ToastrService,
  ) {
    this.cityForm = this.fb.group({
      name: ['', [Validators.required]],
      state: ['', [Validators.required]],
    })
  }
  ngOnInit(): void {
    this.city = new City(0, '', '');
    this.getCities();
  }

  toggleMenu() {
    this.isMenuOpen = !this.isMenuOpen;
  }

  modalAberta = false;

  openModal() {
    this.modalAberta = true;
  }

  closeModal() {
    this.modalAberta = false;
  }

  getCities() {
    debugger
    this.cityService.findAll().subscribe({
      next: (response) => {
        console.log('Buscado com sucesso', response);
        this.cityTabble = response
        console.log("cidade braba table");
        console.log(this.cityTabble);
        
        this.toastr.success('Busca de cidades completas!');
      },
      error: (error) => {
        console.error('Erro ao buscar:', error);
        this.toastr.error(`Erro ao buscar cidades: ${error}`)
      },
    })
  }

  createCity() {
    this.city = this.cityForm?.value
    this.cityService.create(this.city!!).subscribe({
      next: (response) => {
        console.log('Criado cidade com sucesso', response);
        this.toastr.success('Criado cidade com sucesso!');
        this.getCities();
        this.closeModal();
      },
      error: (error) => {
        console.error('Erro ao criar cidade:', error);
        this.toastr.error(`Erro ao criar cidade: ${error}`)
      },
    })
  }

  openModalDelete(city: City) {
    this.idSelectCity = city.id;
    this.cityDeleteName = city.name!;
    setTimeout(() => {
      const checkbox: any = document.getElementById('delete-modal');
      if (checkbox) checkbox.checked = true;
    });
  }
  
  deleteConfirmed() {
    if (this.idSelectCity) {
      this.cityService.delete(this.idSelectCity).subscribe({
        next: (response) => {
          console.log('Cidade excluída com sucesso.', response);
          this.toastr.success('Cidade excluída com sucesso!');
          this.getCities();
        },
        error: (error) => {
          console.error('Erro ao excluir cidade: ', error);
          this.toastr.error(`Erro ao excluir cidade: ${error}`)
        },
      })
    }
  }

}
