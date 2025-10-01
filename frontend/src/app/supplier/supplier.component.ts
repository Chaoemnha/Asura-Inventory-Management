import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../service/api.service';
import { Router } from '@angular/router';
import { NotificationService } from '../service/notification.service';
import { WebSocketService } from '../service/websocket.service';
import { Subscription } from 'rxjs';

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
export class SupplierComponent implements OnInit, OnDestroy {
  suppliers: Supplier[] = [];
  supplierName: string = '';
  supplierEmail: string = '';
  supplierPhone: string = '';
  supplierAddress: string = '';
  isEditing: boolean = false;
  editingSupplierId: string | null = null;
  private wsSubscription!: Subscription;
  constructor(
    private apiService: ApiService, 
    private router: Router,
    private notificationService: NotificationService,
    private webSocketService: WebSocketService
  ) {}

  ngOnInit(): void {
    this.getSuppliers();
    this.wsSubscription = this.webSocketService.getMessages().subscribe({
      next: (message: any) => {
        this.handleWebSocketMessage(message);
      },
      error: (error: any) => {
        console.error('WebSocket error in SupplierComponent:', error);
      }
    });
  }

  private handleWebSocketMessage(message: any): void {    
    switch (message.type) {
      case "SUPPLIER_UPDATED_RENDER":
        this.updateSupplierInList(message.data);
        break;
        
      case 'SUPPLIER_ADDED_RENDER':
        this.addSupplierToList(message.data);
        break;
        
      case 'SUPPLIER_DELETED_RENDER':
        this.removeSupplierFromList(message.supplierId);
        if (this.editingSupplierId === message.supplierId) {
          this.cancel();
        }
        break;
    }
  }

  ngOnDestroy(): void {
    if (this.wsSubscription) {
      this.wsSubscription.unsubscribe();
    }
    this.webSocketService.disconnect();
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
            'Không thể lấy danh sách nhà cung cấp'
        );
      },
    });
  }

  private updateSupplierInList(updatedSupplier: Supplier): void{
    const index = this.suppliers.findIndex(cat=>cat.id===updatedSupplier.id)
    if(index!=-1){
      this.suppliers[index] = updatedSupplier;
    }
  }
  
  private addSupplierToList(newSupplier: Supplier): void {
    const exists = this.suppliers.some(cat => cat.id === newSupplier.id);
    if (!exists) {
      this.suppliers.push(newSupplier);
    }
  }

  private removeSupplierFromList(supplierId: string): void {
    this.suppliers = this.suppliers.filter(cat => cat.id !== supplierId.toString());
  }

  navigateToAddSupplierPage(): void {
    this.router.navigate(['/add-supplier']);
  }

  navigateToEditSupplierPage(supplierId: string): void {
    this.router.navigate([`/edit-supplier/${supplierId}`]);
  }

  addSupplier() {
    if (!this.supplierName) {
      this.notificationService.showError('Lỗi', 'Tên nhà cung cấp là bắt buộc');
      return;
    }
    if (!this.supplierEmail) {
      this.notificationService.showError('Lỗi', 'Email nhà cung cấp là bắt buộc');
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
          this.notificationService.showSuccess('Success', 'Thêm nhà cung cấp thành công');
          this.clearForm();
          this.getSuppliers();
        }
      },
      error: (error: any) => {
        this.notificationService.showError('Lỗi', 
          error?.error?.message || error?.error || 'Không thể thêm nhà cung cấp'
        );
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
          this.notificationService.showSuccess('Success', 'Cập nhật nhà cung cấp thành công');
          this.clearForm();
          this.isEditing = false;
          this.editingSupplierId = null;
          this.getSuppliers();
        }
      },
      error: (error: any) => {
        this.notificationService.showError('Lỗi', 
          error?.error?.message || error?.error || 'Không thể cập nhật nhà cung cấp'
        );
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
    if (window.confirm("Bạn có chắc chắn muốn xóa nhà cung cấp này?")) {
      this.apiService.deleteSupplier(supplierId).subscribe({
        next: (res: any) => {
          if (res.status === 200) {
            this.notificationService.showSuccess('Success', 'Xóa nhà cung cấp thành công');
            this.getSuppliers(); //reload the suppliers
          }
        },
        error: (error) => {
          this.notificationService.showError('Lỗi', 
            error?.error?.message || error?.message || 'Không thể xóa nhà cung cấp'
          );
        }
      });
    }
  }
}
