import { Routes } from '@angular/router';
import { LoginComponent } from './components/pages/login/login.component';
import { InitialScreenComponent } from './components/pages/initial-screen/initial-screen.component';
import { RegisterComponent } from './components/pages/register/register.component';
import { RegisterSupplierComponent } from './components/pages/register-supplier/register-supplier.component';
import { CityRegisterComponent } from './components/pages/city-register/city-register.component';
import { authGuard } from './guards/auth.guard';
import { addressDefinedGuard } from './guards/address-defined.guard';
import { FirstAccessAddressComponent } from './components/pages/first-access-address/first-access-address.component';
import { SupplierApprovalComponent } from './components/pages/supplier-approval/supplier-approval.component';
import { DistribuitorFormComponent } from './components/pages/distribuitor-form/distribuitor-form.component';
import { CategoryProductsComponent } from './components/pages/category-products/category-products.component';
import { ProductItemComponent } from './components/pages/product-item/product-item.component';
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
import { OrderHistoryComponent } from './components/pages/order-history/order-history.component';
import { ManageDeliverersComponent } from './components/pages/manage-deliverers/manage-deliverers.component';
import { ForgotPasswordComponent } from './components/pages/forgot-password/forgot-password.component';
import { CheckEmailComponent } from './components/pages/check-email/check-email.component';
import { ResetPasswordComponent } from './components/pages/reset-password/reset-password.component';
import { EmailSentComponent } from './components/pages/email-sent/email-sent.component';
import { ConfigurationComponent } from './components/pages/configuration/configuration.component';
import { AboutComponent } from './components/pages/about/about.component';
import { ContactComponent } from './components/pages/contact/contact.component';

export const routes: Routes = [
    {path: '', component: InitialScreenComponent},
    {path: 'login', component: LoginComponent},
    {path: 'register', component: RegisterComponent},
    { path: 'unauthorized', component: UnauthorizedComponent },
    {
        path: 'register-supplier', 
        component: RegisterSupplierComponent
    },
    {
        path: 'distributor/new', 
        component: DistribuitorFormComponent,
        canActivate: [authGuard],
        data: {roles: ['ADMIN', 'SUPPLIER']}
    },
    {
        path: 'distributor/edit/:id', 
        component: DistribuitorFormComponent,
        canActivate: [authGuard],
        data: {roles: ['ADMIN', 'SUPPLIER']}
    },
    {
        path: 'category-products', 
        component: CategoryProductsComponent,
        canActivate: [authGuard],
        data: {roles: ['ADMIN', 'SUPPLIER']}
    },
    {
        path: 'product-items', 
        component: ProductItemComponent,
        canActivate: [authGuard],
        data: {roles: ['ADMIN', 'SUPPLIER']}
    },
    {
        path: 'product-items/new', 
        component: ProductItemFormComponent,
        canActivate: [authGuard],
        data: {roles: ['ADMIN', 'SUPPLIER']}
    },
    {
        path: 'product-items/edit/:id', 
        component: ProductItemFormComponent,
        canActivate: [authGuard],
        data: {roles: ['ADMIN', 'SUPPLIER']}
    },
    {
        path: 'register-city', 
        component: CityRegisterComponent,
        canActivate: [authGuard],
        data: {roles: ['ADMIN']}
    },
    {
        path: 'supplier-approval', 
        component: SupplierApprovalComponent,
        canActivate: [authGuard],
        data: {roles: ['ADMIN']}
    },
    {
        path: 'inicio',
        component: SupplierListComponent,
        canActivate: [authGuard, addressDefinedGuard],
        data: {roles: ['ADMIN', 'SUPPLIER', 'CUSTOMER']}
    },
    {
        path: 'primeiro-acesso-endereco',
        component: FirstAccessAddressComponent,
        canActivate: [authGuard]
    },
    {
        path: 'fornecedores/:id/produtos', 
        component: ProductListComponent,
        canActivate: [authGuard],
        data: {roles: ['ADMIN', 'SUPPLIER', 'CUSTOMER']}
    },
    {
        path: 'resumo-pedido', 
        component: OrderResumeComponent,
        canActivate: [authGuard],
        data: {roles: ['ADMIN', 'CUSTOMER']}
    },
    {
        path: 'meus-pedidos', 
        component: OrderListComponent,
        canActivate: [authGuard],
        data: {roles: ['ADMIN', 'CUSTOMER']}
    },
    {
        path: 'painel-cliente', 
        component: CustomerPanelComponent,
        canActivate: [authGuard],
        data: {roles: ['ADMIN', 'CUSTOMER']}
    },
    {
        path: 'meus-enderecos',
        component: ManageAddressesComponent,
        canActivate: [authGuard],
        data: {roles: ['ADMIN', 'CUSTOMER']}
    },
    {
        path: 'dashboard-fornecedor',
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
        path: 'entregadores',
        component: ManageDeliverersComponent,
        canActivate: [authGuard],
        data: {roles: ['SUPPLIER']}
    },
    {
        path: 'historico-pedidos', 
        component: OrderHistoryComponent,
        canActivate: [authGuard],
        data: {roles: ['ADMIN', 'SUPPLIER']}
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
        path: 'email-sent', 
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
        path: 'configuracao', 
        component: ConfigurationComponent,
        canActivate: [authGuard],
        data: {roles: ['ADMIN', 'SUPPLIER']}
    },

];