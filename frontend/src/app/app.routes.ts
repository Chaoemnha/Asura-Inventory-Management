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
import { StaffProfileComponent } from './staff-profile/staff-profile.component';
import { ForgotPasswordComponent } from './forgot-password/forgot-password.component';
import { ResetPasswordComponent } from './reset-password/reset-password.component';
import { ReturnComponent } from './return/return.component';
import { UserManagementComponent } from './user-management/user-management.component';
import { AddEditUserComponent } from './add-edit-user/add-edit-user.component';
import { EditTransactionComponent } from './edit-transaction/edit-transaction.component';

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
    {path: "product", component: ProductComponent},
    {path: "product/:productId", component: AddEditProductComponent},
    {path: "edit-product/:productId", component: AddEditProductComponent, canActivate: [GuardService], data: {deniedRoles: ['STOCKSTAFF', 'SUPPLIER', 'CUSTOMER']}},
    {path: "add-product", component: AddEditProductComponent, canActivate: [GuardService], data: {deniedRoles: ['STOCKSTAFF', 'SUPPLIER', 'CUSTOMER']}},
    
    // Purchase - CUSTOMER bị cấm
    {path: "purchase", component: PurchaseComponent, canActivate: [GuardService], data: {deniedRoles: ['CUSTOMER, SUPPLIER']}},
    
    // Sell - SUPPLIER bị cấm
    {path: "sell", component: SellComponent, canActivate: [GuardService], data: {deniedRoles: ['SUPPLIER, STOCKSTAFF']}},
    
    // Transaction - tất cả vai trò được phép
    {path: "transaction", component: TransactionComponent, canActivate: [GuardService]},
    {path: "transaction/:transactionId", component: TransactionDetailsComponent, canActivate: [GuardService]},
    {path: "edit-transaction/:id", component: EditTransactionComponent, canActivate: [GuardService], data: {deniedRoles: ['STOCKSTAFF', 'SUPPLIER', 'CUSTOMER']}},
    
    // Return - chỉ user có thể return transaction của chính họ
    {path: "transaction/return/:transactionId", component: ReturnComponent, canActivate: [GuardService]},
    
    // User Management - chỉ ADMIN được truy cập
    {path: "users", component: UserManagementComponent, canActivate: [GuardService], data: {deniedRoles: ['STOCKSTAFF', 'SUPPLIER', 'CUSTOMER']}},
    {path: "add-user", component: AddEditUserComponent, canActivate: [GuardService], data: {deniedRoles: [ 'STOCKSTAFF', 'SUPPLIER', 'CUSTOMER']}},
    {path: "edit-user/:userId", component: AddEditUserComponent, canActivate: [GuardService], data: {deniedRoles: ['STOCKSTAFF', 'STOCKSTAFF', 'SUPPLIER', 'CUSTOMER']}},
    
    // Profile và Dashboard - tất cả vai trò được phép
    {path: "profile", component: ProfileComponent, canActivate: [GuardService]},
    {path: "staff-profile/:staffId", component: StaffProfileComponent, canActivate: [GuardService]},
    {path: "dashboard", component: DashboardComponent, canActivate: [GuardService]},
    
    {path: '', redirectTo:'/login', pathMatch: 'full'},
    {
        path: '**', redirectTo: '/dashboard'
    },
];
