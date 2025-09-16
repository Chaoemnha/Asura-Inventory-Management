import { Routes } from '@angular/router';
import { DashboardComponent } from './dashboard/dashboard.component';
import { GuardService } from './service/guard.service';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';
import { CategoryComponent } from './category/category.component';
import { SupplierComponent } from './supplier/supplier.component';
import { AddEditSupplierComponent } from './add-edit-supplier/add-edit-supplier.component';
import { ProductComponent } from './product/product.component';
import { AddEditProductComponent } from './add-edit-product/add-edit-product.component';
import { PurchaseComponent } from './purchase/purchase.component';
import { SellComponent } from './sell/sell.component';
import { TransactionComponent } from './transaction/transaction.component';
import { TransactionDetailsComponent } from './transaction-details/transaction-details.component';
import { ProfileComponent } from './profile/profile.component';
import { ForgotPasswordComponent } from './forgot-password/forgot-password.component';
import { ResetPasswordComponent } from './reset-password/reset-password.component';

export const routes: Routes = [
    {path: "login", component: LoginComponent},
    {path: "forgot-password", component: ForgotPasswordComponent},
    {path: "reset-password", component: ResetPasswordComponent, canActivate: undefined},
    {path: "register", component: RegisterComponent},
    
    // Category - chỉ ADMIN được truy cập
    {path: "category", component: CategoryComponent, canActivate: [GuardService]},
    
    // Supplier routes - STOCKSTAFF, SUPPLIER, CUSTOMER bị cấm
    {path: "supplier", component: SupplierComponent, canActivate: [GuardService], data: {deniedRoles: ['STOCKSTAFF', 'SUPPLIER', 'CUSTOMER']}},
    {path: "edit-supplier/:supplierId", component: AddEditSupplierComponent, canActivate: [GuardService], data: {deniedRoles: ['STOCKSTAFF', 'SUPPLIER', 'CUSTOMER']}},
    {path: "add-supplier", component: AddEditSupplierComponent, canActivate: [GuardService], data: {deniedRoles: ['STOCKSTAFF', 'SUPPLIER', 'CUSTOMER']}},
    
    // Product routes - STOCKSTAFF, SUPPLIER, CUSTOMER bị cấm
    {path: "product", component: ProductComponent, canActivate: [GuardService]},
    {path: "edit-product/:productId", component: AddEditProductComponent, canActivate: [GuardService], data: {deniedRoles: ['STOCKSTAFF', 'SUPPLIER', 'CUSTOMER']}},
    {path: "add-product", component: AddEditProductComponent, canActivate: [GuardService], data: {deniedRoles: ['STOCKSTAFF', 'SUPPLIER', 'CUSTOMER']}},
    
    // Purchase - CUSTOMER bị cấm
    {path: "purchase", component: PurchaseComponent, canActivate: [GuardService], data: {deniedRoles: ['CUSTOMER, SUPPLIER']}},
    
    // Sell - SUPPLIER bị cấm
    {path: "sell", component: SellComponent, canActivate: [GuardService], data: {deniedRoles: ['SUPPLIER, STOCKSTAFF']}},
    
    // Transaction - tất cả vai trò được phép
    {path: "transaction", component: TransactionComponent, canActivate: [GuardService]},
    {path: "transaction/:transactionId", component: TransactionDetailsComponent, canActivate: [GuardService]},
    
    // Profile và Dashboard - tất cả vai trò được phép
    {path: "profile", component: ProfileComponent, canActivate: [GuardService]},
    {path: "dashboard", component: DashboardComponent, canActivate: [GuardService]},
    
    {path: '', redirectTo:'/login', pathMatch: 'full'},
    {
        path: '**', redirectTo: '/dashboard'
    },
];
