import { Component } from '@angular/core';
import { MenuComponent } from '../../menu/menu.component';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { DeleteConfirmationModalComponent } from '../../delete-confirmation-modal/delete-confirmation-modal.component';

@Component({
  selector: 'app-product-item',
  imports: [
    MenuComponent,
    ReactiveFormsModule,
    CommonModule,
    FormsModule,
    DeleteConfirmationModalComponent
  ],
  templateUrl: './product-item.component.html',
  styleUrl: './product-item.component.css'
})
export class ProductItemComponent {
  busca: string = '';

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
}
