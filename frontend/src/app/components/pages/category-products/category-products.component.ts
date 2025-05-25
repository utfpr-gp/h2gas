import { Component } from '@angular/core';
import { MenuComponent } from '../../menu/menu.component';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-category-products',
  imports: [
    MenuComponent,
    ReactiveFormsModule,
    CommonModule,
    FormsModule,
  ],
  templateUrl: './category-products.component.html',
  styleUrl: './category-products.component.css'
})
export class CategoryProductsComponent {


  mostrarFormulario = false;

  constructor(
    
  ) {

  }


  toggleFormulario() {
    this.mostrarFormulario = !this.mostrarFormulario;
  }
}
