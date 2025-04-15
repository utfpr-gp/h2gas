import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { UserService } from '../../../services/user.service';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-city-register',
  imports: [
    ReactiveFormsModule,
    CommonModule,
    FormsModule,
  ],
  templateUrl: './city-register.component.html',
  styleUrl: './city-register.component.css'
})
export class CityRegisterComponent {
  isMenuOpen = false;

  cityForm!: FormGroup;

  constructor(
    private router: Router,
    private fb: FormBuilder, 
    private userService: UserService,
    private toastr: ToastrService,
  ) {

  }

  toggleMenu() {
    this.isMenuOpen = !this.isMenuOpen;
  }

  modalAberta = false;

  openModal() {
    this.modalAberta = true
  }

}
