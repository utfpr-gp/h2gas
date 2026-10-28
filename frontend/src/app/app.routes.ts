import { Routes } from '@angular/router';
import { LoginComponent } from './components/pages/login/login.component';
import { InitialScreenComponent } from './components/pages/initial-screen/initial-screen.component';
import { RegisterComponent } from './components/pages/register/register.component';
import { RegisterSupplierComponent } from './components/pages/register-supplier/register-supplier.component';
import { supplierStoreGuard } from './guards/supplier-store.guard';
import { authGuard } from './guards/auth.guard';
import { activeDeliveryGuard } from './guards/active-delivery.guard';
import { addressDefinedGuard } from './guards/address-defined.guard';
import { FirstAccessAddressComponent } from './components/pages/first-access-address/first-access-address.component';
import { AdminDashboardComponent } from './components/pages/admin-dashboard/admin-dashboard.component';
import { AdminUsersComponent } from './components/pages/admin-users/admin-users.component';
import { AdminProductTypesComponent } from './components/pages/admin-product-types/admin-product-types.component';
import { DistribuitorFormComponent } from './components/pages/distribuitor-form/distribuitor-form.component';
import { ProductItemFormComponent } from './components/pages/product-item-form/product-item-form.component';
import { UnauthorizedComponent } from './components/pages/unauthorized/unauthorized.component';
import { SupplierListComponent } from './components/pages/supplier-list/supplier-list.component';
import { ProductListComponent } from './components/pages/product-list/product-list.component';
import { OrderResumeComponent } from './components/pages/order-resume/order-resume.component';
import { OrderListComponent } from './components/pages/order-list/order-list.component';
import { CustomerPanelComponent } from './components/pages/customer-panel/customer-panel.component';
import { ManageAddressesComponent } from './components/pages/manage-addresses/manage-addresses.component';
import { ReceivedOrdersComponent } from './components/pages/received-orders/received-orders.component';
import { AwaitingApprovalComponent } from './components/pages/awaiting-approval/awaiting-approval.component';
import { ManageDeliverersComponent } from './components/pages/manage-deliverers/manage-deliverers.component';
import { DelivererRoutesComponent } from './components/pages/deliverer-routes/deliverer-routes.component';
import { DelivererRouteDetailComponent } from './components/pages/deliverer-route-detail/deliverer-route-detail.component';
import { DelivererHistoryComponent } from './components/pages/deliverer-history/deliverer-history.component';
import { SupplierDashboardComponent } from './components/pages/supplier-dashboard/supplier-dashboard.component';
import { LoginTokenComponent } from './components/pages/login-token/login-token.component';
import { ForgotPasswordComponent } from './components/pages/forgot-password/forgot-password.component';
import { CheckEmailComponent } from './components/pages/check-email/check-email.component';
import { ResetPasswordComponent } from './components/pages/reset-password/reset-password.component';
import { EmailSentComponent } from './components/pages/email-sent/email-sent.component';
import { AboutComponent } from './components/pages/about/about.component';
import { ContactComponent } from './components/pages/contact/contact.component';

export const routes: Routes = [
    {path: '', component: InitialScreenComponent},
    {path: 'login', component: LoginTokenComponent},
    {path: 'login/token', redirectTo: 'login', pathMatch: 'full'},
    {path: 'login/senha', component: LoginComponent},
    {path: 'cadastro', component: RegisterComponent},
    { path: 'acesso-negado', component: UnauthorizedComponent },
    {
        path: 'cadastro/fornecedor', 
        component: RegisterSupplierComponent
    },
    {
        path: 'admin/fornecedores/novo', 
        component: DistribuitorFormComponent,
        canActivate: [authGuard],
        data: {roles: ['ADMIN', 'SUPPLIER']}
    },
    // links antigos da tela da loja (o id da URL não é mais usado)
    {path: 'distributor/edit/:id', redirectTo: 'minha-loja'},
    {
        path: 'minha-loja',
        // F7 fatia 6: tela única do fornecedor (conta e loja são o mesmo cadastro)
        loadComponent: () => import('./components/pages/store-profile/store-profile.component').then((m) => m.StoreProfileComponent),
        canActivate: [authGuard],
        data: {roles: ['SUPPLIER']}
    },
    {
        // HU19: as categorias são globais — só o admin cadastra (tela fora do bundle inicial)
        path: 'admin/categorias',
        loadComponent: () => import('./components/pages/admin-categories/admin-categories.component').then((m) => m.AdminCategoriesComponent),
        canActivate: [authGuard],
        data: {roles: ['ADMIN']}
    },
    {
        path: 'minha-loja/produtos', 
        // tela só do fornecedor: fora do bundle inicial
        loadComponent: () => import('./components/pages/product-item/product-item.component').then((m) => m.ProductItemComponent),
        canActivate: [authGuard],
        data: {roles: ['SUPPLIER']}
    },
    {
        path: 'minha-loja/produtos/novo', 
        component: ProductItemFormComponent,
        canActivate: [authGuard],
        data: {roles: ['SUPPLIER']}
    },
    {
        path: 'minha-loja/produtos/:id/editar', 
        component: ProductItemFormComponent,
        canActivate: [authGuard],
        data: {roles: ['SUPPLIER']}
    },
    {
        path: 'admin',
        component: AdminDashboardComponent,
        canActivate: [authGuard],
        data: {roles: ['ADMIN']}
    },
    {
        path: 'admin/usuarios',
        component: AdminUsersComponent,
        canActivate: [authGuard],
        data: {roles: ['ADMIN']}
    },
    {
        path: 'admin/tipos',
        component: AdminProductTypesComponent,
        canActivate: [authGuard],
        data: {roles: ['ADMIN']}
    },
    {
        path: 'admin/cidades', 
        // tela só do admin: fora do bundle inicial
        loadComponent: () => import('./components/pages/city-register/city-register.component').then((m) => m.CityRegisterComponent),
        canActivate: [authGuard],
        data: {roles: ['ADMIN']}
    },
    {
        path: 'admin/fornecedores',
        // tela só do admin: fora do bundle inicial
        loadComponent: () => import('./components/pages/supplier-approval/supplier-approval.component')
            .then((m) => m.SupplierApprovalComponent),
        canActivate: [authGuard],
        data: {roles: ['ADMIN']}
    },
    {
        path: 'inicio',
        component: SupplierListComponent,
        canActivate: [authGuard, addressDefinedGuard],
        // HU13: vitrine aberta a visitantes (a cidade vem do CEP)
        data: {roles: ['ADMIN', 'SUPPLIER', 'CUSTOMER'], allowVisitor: true}
    },
    {
        path: 'onde-voce-esta',
        loadComponent: () => import('./components/pages/visitor-location/visitor-location.component').then((m) => m.VisitorLocationComponent),
    },
    {
        path: 'primeiro-acesso-endereco',
        component: FirstAccessAddressComponent,
        canActivate: [authGuard]
    },
    {
        path: 'lojas/:id', 
        component: ProductListComponent,
        canActivate: [authGuard],
        data: {roles: ['ADMIN', 'SUPPLIER', 'CUSTOMER'], allowVisitor: true}
    },
    {
        path: 'resumo-pedido', 
        component: OrderResumeComponent,
        // HU13: cliente sem endereço passa pelo primeiro endereço e volta ao checkout
        canActivate: [authGuard, addressDefinedGuard],
        data: {roles: ['ADMIN', 'CUSTOMER']}
    },
    {
        path: 'meus-pedidos', 
        component: OrderListComponent,
        canActivate: [authGuard],
        data: {roles: ['ADMIN', 'CUSTOMER']}
    },
    {
        path: 'meus-dados',
        component: CustomerPanelComponent,
        canActivate: [authGuard, supplierStoreGuard],
        data: {roles: ['ADMIN', 'CUSTOMER', 'SUPPLIER', 'DELIVERER']}
    },
    // rota antiga (favoritos e links): segue funcionando
    {path: 'painel-cliente', redirectTo: 'meus-dados', pathMatch: 'full'},
    {
        path: 'meus-enderecos',
        component: ManageAddressesComponent,
        canActivate: [authGuard],
        data: {roles: ['ADMIN', 'CUSTOMER']}
    },
    {
        path: 'minha-loja/painel',
        component: SupplierDashboardComponent,
        canActivate: [authGuard],
        data: {roles: ['SUPPLIER']}
    },
    {
        path: 'minha-loja/pedidos',
        component: ReceivedOrdersComponent,
        canActivate: [authGuard],
        data: {roles: ['ADMIN', 'SUPPLIER']}
    },
    {
        path: 'aguardando-aprovacao', 
        component: AwaitingApprovalComponent,
        canActivate: [authGuard],
        data: {roles: ['ADMIN', 'SUPPLIER']}
    },
    {
        path: 'entregas',
        component: DelivererRoutesComponent,
        canActivate: [authGuard, activeDeliveryGuard],
        data: {roles: ['ADMIN', 'DELIVERER']}
    },
    {
        path: 'entregas/historico',
        component: DelivererHistoryComponent,
        canActivate: [authGuard, activeDeliveryGuard],
        data: {roles: ['ADMIN', 'DELIVERER']}
    },
    {
        path: 'entregas/:id',
        component: DelivererRouteDetailComponent,
        canActivate: [authGuard, activeDeliveryGuard],
        data: {roles: ['ADMIN', 'DELIVERER']}
    },
    {
        path: 'minha-loja/entregadores',
        component: ManageDeliverersComponent,
        canActivate: [authGuard],
        data: {roles: ['SUPPLIER']}
    },
    {
        path: 'minha-loja/pedidos/historico',
        // tela só do fornecedor (o histórico vem do token): fora do bundle inicial
        loadComponent: () => import('./components/pages/order-history/order-history.component').then((m) => m.OrderHistoryComponent),
        canActivate: [authGuard],
        data: {roles: ['SUPPLIER']}
    },
    {
        path: 'recuperar-senha', 
        component: ForgotPasswordComponent
    },
    {
        path: 'checagem-email', 
        component: CheckEmailComponent
    },
    {
        path: 'resetar-senha', 
        component: ResetPasswordComponent
    },
    {
        path: 'email-enviado', 
        component: EmailSentComponent
    },
    {
        path: 'sobre', 
        component: AboutComponent
    },
    {
        path: 'contato', 
        component: ContactComponent
    },
    {
        path: 'minha-loja/configuracoes',
        // tela só do fornecedor (o admin não tem loja): fora do bundle inicial
        // F9: configurações da loja numa tela só (perfil + horário + estoque)
        loadComponent: () => import('./components/pages/store-profile/store-profile.component').then((m) => m.StoreProfileComponent),
        canActivate: [authGuard],
        data: {roles: ['SUPPLIER']}
    },

    // F8: URLs antigas (favoritos e links) seguem funcionando — redirecionam para a nova, mantendo o :id
    {path: 'register', redirectTo: 'cadastro', pathMatch: 'full'},
    {path: 'unauthorized', redirectTo: 'acesso-negado', pathMatch: 'full'},
    {path: 'register-supplier', redirectTo: 'cadastro/fornecedor', pathMatch: 'full'},
    {path: 'distributor/new', redirectTo: 'admin/fornecedores/novo', pathMatch: 'full'},
    {path: 'product-items', redirectTo: 'minha-loja/produtos', pathMatch: 'full'},
    {path: 'product-items/new', redirectTo: 'minha-loja/produtos/novo', pathMatch: 'full'},
    {path: 'product-items/edit/:id', redirectTo: 'minha-loja/produtos/:id/editar', pathMatch: 'full'},
    {path: 'register-city', redirectTo: 'admin/cidades', pathMatch: 'full'},
    {path: 'supplier-approval', redirectTo: 'admin/fornecedores', pathMatch: 'full'},
    {path: 'fornecedores/:id/produtos', redirectTo: 'lojas/:id', pathMatch: 'full'},
    {path: 'dashboard', redirectTo: 'minha-loja/painel', pathMatch: 'full'},
    {path: 'dashboard-fornecedor', redirectTo: 'minha-loja/pedidos', pathMatch: 'full'},
    {path: 'entregadores', redirectTo: 'minha-loja/entregadores', pathMatch: 'full'},
    {path: 'historico-pedidos', redirectTo: 'minha-loja/pedidos/historico', pathMatch: 'full'},
    {path: 'email-sent', redirectTo: 'email-enviado', pathMatch: 'full'},
    {path: 'configuracao', redirectTo: 'minha-loja/configuracoes', pathMatch: 'full'},
];
