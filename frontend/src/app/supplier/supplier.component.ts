import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../service/api.service';
import { Router } from '@angular/router';
import { NotificationService } from '../service/notification.service';

interface Supplier {
  id: string;
  name: string;
  email: string;
  phone: string;
  address: string;
}

@Component({
  selector: 'app-supplier',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './supplier.component.html',
  styleUrl: './supplier.component.css',
})
export class SupplierComponent implements OnInit {
  suppliers: Supplier[] = [];
  supplierName: string = '';
  supplierEmail: string = '';
  supplierPhone: string = '';
  supplierAddress: string = '';
  isEditing: boolean = false;
  editingSupplierId: string | null = null;

  constructor(
    private apiService: ApiService, 
    private router: Router,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.getSuppliers();
  }

  getSuppliers(): void {
    this.apiService.getAllSuppliers().subscribe({
      next: (res: any) => {
        if (res.status === 200) {
          this.suppliers = res.suppliers;
        } else {
          this.notificationService.showError('Error', res.message);
        }
      },
      error: (error) => {
        this.notificationService.showError('Error', 
          error?.error?.message ||
            error?.message ||
            'Unable to get suppliers'
        );
      },
    });
  }

  navigateToAddSupplierPage(): void {
    this.router.navigate(['/add-supplier']);
  }

  navigateToEditSupplierPage(supplierId: string): void {
    this.router.navigate([`/edit-supplier/${supplierId}`]);
  }

  addSupplier() {
    if (!this.supplierName) {
      this.notificationService.showError('Error', "Supplier name is required");
      return;
    }
    if (!this.supplierEmail) {
      this.notificationService.showError('Error', "Supplier email is required");
      return;
    }
    
    const supplierData = {
      name: this.supplierName,
      email: this.supplierEmail,
      phone: this.supplierPhone,
      address: this.supplierAddress
    };

    this.apiService.createSupplier(supplierData).subscribe({
      next: (res: any) => {
        if (res.status == 200) {
          this.notificationService.showSuccess('Success', "Supplier added successfully");
          this.clearForm();
          this.getSuppliers();
        }
      },
      error: (error: any) => {
        this.notificationService.showError('Error', error?.error?.message || error?.error || "Unable to add supplier");
      }
    });
  }

  editSupplier() {
    if (!this.editingSupplierId || !this.supplierName) {
      return;
    }
    
    const supplierData = {
      name: this.supplierName,
      email: this.supplierEmail,
      phone: this.supplierPhone,
      address: this.supplierAddress
    };

    this.apiService.updateSupplier(this.editingSupplierId, supplierData).subscribe({
      next: (res: any) => {
        if (res.status == 200) {
          this.notificationService.showSuccess('Success', "Supplier updated successfully");
          this.clearForm();
          this.isEditing = false;
          this.editingSupplierId = null;
          this.getSuppliers();
        }
      },
      error: (error: any) => {
        this.notificationService.showError('Error', error?.error?.message || error?.error || "Unable to update supplier");
      }
    });
  }

  handleEditSupplier(supplier: Supplier) {
    this.isEditing = true;
    this.editingSupplierId = supplier.id;
    this.supplierName = supplier.name;
    this.supplierEmail = supplier.email;
    this.supplierPhone = supplier.phone;
    this.supplierAddress = supplier.address;
  }

  cancel() {
    this.isEditing = false;
    this.editingSupplierId = null;
    this.clearForm();
  }

  clearForm() {
    this.supplierName = '';
    this.supplierEmail = '';
    this.supplierPhone = '';
    this.supplierAddress = '';
  }

  //Delete a supplier
  handleDeleteSupplier(supplierId: string): void {
    if (window.confirm("Are you sure you want to delete this supplier?")) {
      this.apiService.deleteSupplier(supplierId).subscribe({
        next: (res: any) => {
          if (res.status === 200) {
            this.notificationService.showSuccess('Success', "Supplier deleted successfully");
            this.getSuppliers(); //reload the suppliers
          }
        },
        error: (error) => {
          this.notificationService.showError('Error', 
            error?.error?.message || error?.message || "Unable to Delete Supplier"
          );
        }
      });
    }
  }
}
