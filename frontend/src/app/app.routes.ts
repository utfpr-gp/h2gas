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

export const routes: Routes = [
    {path: '', component: InitialScreenComponent},
    {path: 'login', component: LoginComponent},
    {path: 'register', component: RegisterComponent},
    { path: 'unauthorized', component: UnauthorizedComponent },
    {
        path: 'register-supplier', 
        component: RegisterSupplierComponent,
        canActivate: [authGuard],
    },
    {
        path: 'management-distributor', 
        component: DistributorManagementComponent,
        canActivate: [authGuard],
    },
    {
        path: 'distributor/new', 
        component: DistribuitorFormComponent,
        canActivate: [authGuard],
    },
    {
        path: 'distributor/edit/:id', 
        component: DistribuitorFormComponent,
        canActivate: [authGuard],
    },
    {
        path: 'category-products', 
        component: CategoryProductsComponent,
        canActivate: [authGuard],
    },
    {
        path: 'product-items', 
        component: ProductItemComponent,
        canActivate: [authGuard],
    },
    {
        path: 'product-items/new', 
        component: ProductItemFormComponent,
        canActivate: [authGuard],
    },
    {
        path: 'product-items/edit/:id', 
        component: ProductItemFormComponent,
        canActivate: [authGuard],

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
];