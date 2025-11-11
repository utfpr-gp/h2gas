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
import { User } from '../../../models/user';
import { UserService } from '../../../services/user.service';
import { ConfigurationService } from '../../../services/configuration.service';

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

  deleteNameProduct?: string;
  idProductRow?: number;

  produtos: ProductItem[] = []

  constructor(
    private router: Router,
    private productItemService: ProductItemService,
    private toastr: ToastrService,
    private userService: UserService,
    private configurationService: ConfigurationService
  ) {}
  userLogado?: User;


  async ngOnInit() {
    this.userLogado = this.userService.getUserLocalStorage()
    const settings = this.configurationService.getSettings();
    if (settings?.estoqueMin) {
      this.estoqueMinimoConfig = settings.estoqueMin;
    }
    await this.findProdutos();
  }

  openModalDelete(productItem: ProductItem) {
    this.deleteNameProduct = `${productItem.brand} - ${productItem.category}`
    this.idProductRow = productItem.id
    
    setTimeout(() => {
      const checkbox: any = document.getElementById('delete-modal');
      if (checkbox) checkbox.checked = true;
    });
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
    
    this.productItemService.getProductsBySupllierId(this.userLogado?.supplierId!).subscribe({
      next: (response) => {
        console.log('Buscado com sucesso', response);
        this.itemProducts = response.data
        if (this.itemProducts.length > 0) {
          this.toastr.success('Produtos buscado com sucesso!');          
        }
        this.countingDisponivel();
      },
      error: (err) => {
        
        console.error('Erro ao buscar:', err.error.message);
        this.toastr.error(`Erro ao buscar produtos: ${err.error.message}`)
      },
    })
  }

  navigateEdit(id?: number) {
    this.router.navigate(['/product-items/edit', id])
  }

  deleteConfirmed() {
    
    this.productItemService.delete(this.idProductRow!).subscribe({
      next: async (response) => {
        this.toastr.success("Produto deletado com sucesso!!");
        await this.findProdutos();
        this.countingDisponivel();
      },
      error: (err) => {
        this.toastr.error(`Erro ao deletar produto!! ${err.error?.message}`);
        console.error(err);
      },
    })
  }
  estoqueMinimoConfig: number = 8;
  
  countingDisponivel() {
    
    this.availableProduct = 0;
    this.esgotadoProducts = 0;
    this.lowProducts = 0;

    for (let element of this.itemProducts ?? []) {
      const qtd = element.stockQuantity ?? 0;

      if (qtd > 0) this.availableProduct++;
      if (qtd === 0) this.esgotadoProducts++;
      if (qtd > 0 && qtd <= this.estoqueMinimoConfig) this.lowProducts++;
    }

    console.log({
      disponiveis: this.availableProduct,
      baixoEstoque: this.lowProducts,
      esgotado: this.esgotadoProducts,
      limiteConfig: this.estoqueMinimoConfig
    });
  }

  


}
