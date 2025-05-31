import { Component, OnInit } from '@angular/core';
import { MenuComponent } from '../../menu/menu.component';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { DeleteConfirmationModalComponent } from '../../delete-confirmation-modal/delete-confirmation-modal.component';
import { Router } from '@angular/router';
import { ProductItemService } from '../../../services/product-item.service';
import { ProductItem } from '../../../models/product-item';
import { ToastrService } from 'ngx-toastr';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-product-item',
  imports: [
    ReactiveFormsModule,
    CommonModule,
    FormsModule,
    DeleteConfirmationModalComponent
  ],
  templateUrl: './product-item.component.html',
  styleUrl: './product-item.component.css'
})
export class ProductItemComponent implements OnInit {
  busca: string = '';

  availableProduct: number = 0;

  itemProducts?: ProductItem[]; 

  esgotadoProducts: number = 0;

  lowProducts: number = 0;

  produtos = [
    {
      nome: 'Botijão P13 - Ultragaz',
      fornecedor: 'Ultragaz',
      preco: 85.9,
      estoque: 45,
      ultimaAtualizacao: '14/01/2024',
      status: 'Disponível',
      imagem: 'https://example.com/p13.jpg'
    },
    {
      nome: 'Botijão P20 - Copagaz',
      fornecedor: 'Copagaz',
      preco: 125.5,
      estoque: 12,
      ultimaAtualizacao: '13/01/2024',
      status: 'Baixo Estoque',
      imagem: 'https://example.com/p20.jpg'
    },
    {
      nome: 'Botijão P45 - Liquigás',
      fornecedor: 'Liquigás',
      preco: 280.0,
      estoque: 0,
      ultimaAtualizacao: '09/01/2024',
      status: 'Esgotado',
      imagem: ''
    }
  ];

  constructor(
    private router: Router,
    private productItemService: ProductItemService,
    private toastr: ToastrService,
  ) {}


  async ngOnInit() {
    debugger
    await this.findProdutos();
    this.countingDisponivel();
  }

  produtosFiltrados() {
    if (!this.busca) return this.produtos;
    return this.produtos.filter(p =>
      p.nome.toLowerCase().includes(this.busca.toLowerCase())
    );
  }

  contarPorStatus(status: string): number {
    return this.produtos.filter(p => p.status === status).length;
  }

  badgeClass(status: string): string {
    switch (status) {
      case 'Disponível':
        return 'bg-green-200 text-green-700';
      case 'Baixo Estoque':
        return 'bg-yellow-200 text-yellow-700';
      case 'Esgotado':
        return 'bg-red-200 text-red-700';
      default:
        return 'bg-gray-200 text-gray-700';
    }
  }

  navigateRoute(routeName: string) {
    this.router.navigate([routeName]);
  }

  async findProdutos() {
    debugger
    try {
      debugger
      this.itemProducts = await firstValueFrom(this.productItemService.findAll());
      console.log(this.itemProducts);
      this.toastr.success("Buscado com sucesso os produtos!!");
    } catch (err: any) {
      this.toastr.error(`Erro ao buscar os produtos!! ${err.error?.message}`);
      console.error(err);
    }
  }

  countingDisponivel() {
    debugger
    for (let index = 0; index < this.itemProducts!.length; index++) {
      const element = this.itemProducts![index];
      debugger

      if (element.stockQuantity! > 0) {
        this.availableProduct++
      } 

      if (element.stockQuantity! == 0 || element.stockQuantity == null) {
        this.esgotadoProducts++
      }

      if (element.stockQuantity! <= 8) {
        this.lowProducts++
      }

    }

    console.log(this.availableProduct);
    
  }


}
