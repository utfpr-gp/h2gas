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

export const routes: Routes = [
    {path: '', component: InitialScreenComponent},
    {path: 'login', component: LoginComponent},
    {path: 'register', component: RegisterComponent},
    {path: 'register-supplier', component: RegisterSupplierComponent},
    {path: 'management-distributor', component: DistributorManagementComponent},
    {path: 'distributor/new', component: DistribuitorFormComponent},
    {path: 'distributor/edit/:id', component: DistribuitorFormComponent},
    {path: 'category-products', component: CategoryProductsComponent},
    {path: 'product-items', component: ProductItemComponent},
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