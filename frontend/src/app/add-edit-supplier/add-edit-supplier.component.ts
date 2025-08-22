import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService } from '../service/api.service';
import { NotificationService } from '../service/notification.service';

@Component({
  selector: 'app-add-edit-supplier',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './add-edit-supplier.component.html',
  styleUrl: './add-edit-supplier.component.css',
})
export class AddEditSupplierComponent implements OnInit {
  constructor(private apiService: ApiService, private router: Router, private notificationService: NotificationService) {}
  message: string = '';
  isEditing: boolean = false;
  supplierId: string | null = null;

  formData: any = {
    name: '',
    email: '',
    phone: '',
    address: '',
  };

  ngOnInit(): void {
    this.supplierId = this.router.url.split('/')[2]; //extracting supplier id from url
    if (this.supplierId) {
      this.isEditing = true;
      this.fetchSupplier();
    }
  }

  fetchSupplier(): void {
    this.apiService.getSupplierById(this.supplierId!).subscribe({
      next: (res: any) => {
        if (res.status === 200) {
          this.formData = {
            name: res.supplier.name,
            email: res.supplier.email,
            phone: res.supplier.phone,
            address: res.supplier.address,
          };
        }
      },
      error: (error) => {
        this.notificationService.showError("Error",
          error?.error?.message ||
            error?.message ||
            'Unable to get supplier by id' + error
        );
      },
    });
  }

  // HANDLE FORM SUBMISSION
  handleSubmit() {
    if (!this.formData.name) {
      this.notificationService.showWarning('Caution!', "Supplier name is required");
      return;
    }
    if (!this.formData.phone) {
      this.notificationService.showWarning('Caution!', "Supplier phone is required");
      return;
    }

    if (!this.formData.address) {
      this.notificationService.showWarning('Caution!', "Supplier address is required");
      return;
    }
    //prepare data for submission
    const supplierData = {
      name: this.formData.name,
      email: this.formData.email,
      phone: this.formData.phone,
      address: this.formData.address,
    };
    

    if (this.isEditing) {
      this.apiService.updateSupplier(this.supplierId!, supplierData).subscribe({
        next:(res:any) =>{
          if (res.status === 200) {
            this.notificationService.showSuccess("Success","Supplier updated successfully");
            this.router.navigate(['/supplier'])
          }
        },
        error:(error) =>{
          this.notificationService.showError("Error",error?.error?.message || error?.message || "Unable to edit supplier" + error)
        }
      })
    } else {
      this.apiService.addSupplier(supplierData).subscribe({
        next:(res:any) =>{
          if (res.status === 200) {
            this.notificationService.showSuccess("Success","Supplier Added successfully");
            this.router.navigate(['/supplier'])
          }
        },
        error:(error) =>{
          this.notificationService.showError("Error",error?.error?.message || error?.message || "Unable to Add supplier" + error)
        }
      })
    }
  }

  cancel(){
    this.router.navigate(['/supplier']);
  }

}
