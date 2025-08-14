import { Routes } from '@angular/router';
import { LoginComponent } from './components/pages/login/login.component';
import { InitialScreenComponent } from './components/pages/initial-screen/initial-screen.component';
import { RegisterComponent } from './components/pages/register/register.component';
import { RegisterSupplierComponent } from './components/pages/register-supplier/register-supplier.component';
import { CityRegisterComponent } from './components/pages/city-register/city-register.component';
import { authGuard } from './guards/auth.guard';
import { SupplierApprovalComponent } from './components/pages/supplier-approval/supplier-approval.component';
import { DistributorManagementComponent } from './components/pages/distributor-management/distributor-management.component';
import { DistribuitorFormComponent } from './components/pages/distribuitor-form/distribuitor-form.component';
import { CategoryProductsComponent } from './components/pages/category-products/category-products.component';
import { ProductItemComponent } from './components/pages/product-item/product-item.component';
import { ProductItemFormComponent } from './components/pages/product-item-form/product-item-form.component';
import { UnauthorizedComponent } from './components/pages/unauthorized/unauthorized.component';
import { SupplierListComponent } from './components/pages/supplier-list/supplier-list.component';
import { ProductListComponent } from './components/pages/product-list/product-list.component';
import { OrderResumeComponent } from './components/pages/order-resume/order-resume.component';

export const routes: Routes = [
    {path: '', component: InitialScreenComponent},
    {path: 'login', component: LoginComponent},
    {path: 'register', component: RegisterComponent},
    { path: 'unauthorized', component: UnauthorizedComponent },
    {
        path: 'register-supplier', 
        component: RegisterSupplierComponent,
        canActivate: [authGuard],
        data: {roles: ['ADMIN', 'SUPPLIER']}
    },
    {
        path: 'management-distributor', 
        component: DistributorManagementComponent,
        canActivate: [authGuard],
        data: {roles: ['ADMIN', 'SUPPLIER']}
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
        path: 'fornecedores', 
        component: SupplierListComponent,
        canActivate: [authGuard],
        data: {roles: ['ADMIN']}
    },
    {
        path: 'fornecedores/:id/produtos', 
        component: ProductListComponent,
        canActivate: [authGuard],
        data: {roles: ['ADMIN']}
    },
    {
        path: 'resumo-pedido', 
        component: OrderResumeComponent,
        canActivate: [authGuard],
        data: {roles: ['ADMIN']}
    },
];