import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { navigateKeepingReturnUrl } from '../../utils/return-url';

interface TabBarItem {
  id: string;
  label: string;
  icon: string;
  path: string;
  queryParams: Record<string, string>;
}

@Component({
  selector: 'app-bottom-tab-bar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './bottom-tab-bar.component.html'
})
export class BottomTabBarComponent {
  // HU13: o visitante vê Início e Entrar (pedidos e perfil pedem conta).
  // HU15 (revisão mobile): a aba "Categorias" saiu — Produtos fica no seletor Lojas/Produtos da vitrine
  @Input() visitor = false;

  private readonly shopTabs: TabBarItem[] = [
    { id: 'inicio', label: 'Início', icon: 'home', path: '/inicio', queryParams: {} },
  ];
  private readonly customerTabs: TabBarItem[] = [
    ...this.shopTabs,
    { id: 'pedidos', label: 'Pedidos', icon: 'shopping_bag', path: '/meus-pedidos', queryParams: {} },
    { id: 'perfil', label: 'Perfil', icon: 'person', path: '/meus-dados', queryParams: {} },
  ];
  private readonly visitorTabs: TabBarItem[] = [
    ...this.shopTabs,
    { id: 'entrar', label: 'Entrar', icon: 'login', path: '/login', queryParams: {} },
  ];

  get tabs(): TabBarItem[] {
    return this.visitor ? this.visitorTabs : this.customerTabs;
  }

  constructor(private router: Router) {}

  navigateTo(tab: TabBarItem) {
    if (tab.id === 'entrar') {
      navigateKeepingReturnUrl(this.router, '/login', this.router.url);
      return;
    }
    this.router.navigate([tab.path], { queryParams: tab.queryParams });
  }
}
