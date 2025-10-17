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
  ) {}
  userLogado?: User;


  async ngOnInit() {
    debugger
    this.userLogado = this.userService.getUserLocalStorage()
    await this.findProdutos();
    this.countingDisponivel();
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
    debugger
    this.productItemService.getProductsBySupllierId(this.userLogado?.supplierId!).subscribe({
      next: (response) => {
        console.log('Buscado com sucesso', response);
        this.itemProducts = response.data
        console.log(this.itemProducts);
        if (this.itemProducts.length > 0) {
          this.toastr.success('Produtos buscado com sucesso!');          
        }
      },
      error: (err) => {
        debugger
        console.error('Erro ao buscar:', err.error.message);
        this.toastr.error(`Erro ao buscar produtos: ${err.error.message}`)
      },
    })
    // try {
    //   debugger
    //   this.itemProducts = await firstValueFrom(this.productItemService.findAll());
    //   console.log(this.itemProducts);
    //   this.toastr.success("Buscado com sucesso os produtos!!");
    // } catch (err: any) {
    //   this.toastr.error(`Erro ao buscar os produtos!! ${err.error?.message}`);
    //   console.error(err);
    // }
  }

  navigateEdit(id?: number) {
    this.router.navigate(['/product-items/edit', id])
  }

  deleteConfirmed() {
    debugger
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

  countingDisponivel() {
    debugger
    this.availableProduct = 0
    this.esgotadoProducts = 0
    this.lowProducts = 0

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
