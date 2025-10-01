import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../service/api.service';
import { NotificationService } from '../service/notification.service';
import { Subscription } from 'rxjs';
import { WebSocketService } from '../service/websocket.service';

@Component({
  selector: 'app-edit-transaction',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './edit-transaction.component.html',
  styleUrl: './edit-transaction.component.css'
})
export class EditTransactionComponent implements OnInit, OnDestroy {
  transactionId: string = '';
  transaction: any = {};
  
  private wsSubscription!: Subscription;
  formData = {
    productId: '',
    quantity: '',
    supplierId: '',
    userId: '',
    senderId: '',
    description: '',
    totalPrice: '',
    status: ''
  };

  products: any[] = [];
  suppliers: any[] = [];
  users: any[] = [];
  availableSenders: any[] = [];
  
  statusOptions = [
    { value: 'PENDING', label: 'Pending' },
    { value: 'COMPLETED', label: 'Completed' },
    { value: 'CANCELED', label: 'Canceled' },
    { value: 'SENDER_DECIDING', label: 'Sender Deciding' },
    { value: 'ADMIN_DECIDING', label: 'Admin Deciding' },
    { value: 'SENDER_DECLINED', label: 'Sender Declined' }
  ];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private apiService: ApiService,
    private notificationService: NotificationService,
    private webSocketService: WebSocketService
  ) {}

  ngOnInit(): void {
    this.transactionId = this.route.snapshot.paramMap.get('id') || '';
    if (this.transactionId) {
      this.loadTransaction();
      this.loadProducts();
      this.loadSuppliers();
      this.loadUsers();
    }
    this.wsSubscription = this.webSocketService.getMessages().subscribe({
      next: (message: any) => {
        this.handleWebSocketMessage(message);
      },
      error: (error: any) => {
        console.error('WebSocket error in TransactionDetailsComponent:', error);
      }
    });
  }

  loadTransaction(): void {
    this.apiService.getTransactionById(this.transactionId).subscribe({
      next: (response) => {
        if (response.status === 200) {
          this.transaction = response.transaction;
          this.populateForm();
        }
      },
      error: (error) => {
        this.notificationService.showError('Error', 'Failed to load transaction details');
        console.error('Error loading transaction:', error);
      }
    });
  }

  populateForm(): void {
    this.formData = {
      productId: this.transaction.product?.id || '',
      quantity: this.transaction.totalProducts || '',
      supplierId: this.transaction.supplier?.id || '',
      userId: this.transaction.user?.id || '',
      senderId: this.transaction.sender?.id || '',
      description: this.transaction.description || '',
      totalPrice: this.transaction.totalPrice || '',
      status: this.transaction.status || ''
    };

    // Load senders for the current supplier if exists
    if (this.formData.supplierId) {
      this.apiService.getUserBySupplierId(this.formData.supplierId).subscribe({
        next: (response) => {
          if (response.status === 200) {
            this.availableSenders = response.users || [];
          }
        },
        error: (error) => {
          console.error('Error loading senders for supplier:', error);
        }
      });
    }
  }

  loadProducts(): void {
    this.apiService.getAllProducts('').subscribe({
      next: (response) => {
        this.products = response.products || [];
      },
      error: (error) => {
        console.error('Error loading products:', error);
      }
    });
  }

  loadSuppliers(): void {
    this.apiService.getAllSuppliers().subscribe({
      next: (response) => {
        this.suppliers = response.suppliers || [];
      },
      error: (error) => {
        console.error('Error loading suppliers:', error);
      }
    });
  }

  loadUsers(): void {
    this.apiService.getAllUsers().subscribe({
      next: (response) => {
        this.users = response.users || [];
      },
      error: (error) => {
        console.error('Error loading users:', error);
      }
    });
  }

  onProductChange(): void {
    const selectedProduct = this.products.find(p => p.id == this.formData.productId);
    if (selectedProduct && this.formData.quantity) {
      this.formData.totalPrice = (selectedProduct.price * parseInt(this.formData.quantity)).toString();
    }
  }

  onQuantityChange(): void {
    this.onProductChange();
  }

  onSupplierChange(): void {
    this.availableSenders = [];
    this.formData.senderId = '';

    if (this.formData.supplierId) {
      this.apiService.getUserBySupplierId(this.formData.supplierId).subscribe({
        next: (response) => {
          if (response.status === 200) {
            this.availableSenders = response.users || [];
          }
        },
        error: (error) => {
          console.error('Error loading senders for supplier:', error);
        }
      });
    }
  }

  onSubmit(): void {
    if (this.validateForm()) {
      const updateData = {
        productId: this.formData.productId ? parseInt(this.formData.productId) : null,
        quantity: this.formData.quantity ? parseInt(this.formData.quantity) : null,
        supplierId: this.formData.supplierId ? parseInt(this.formData.supplierId) : null,
        userId: this.formData.userId ? parseInt(this.formData.userId) : null,
        senderId: this.formData.senderId ? parseInt(this.formData.senderId) : null,
        description: this.formData.description,
        totalPrice: this.formData.totalPrice ? parseFloat(this.formData.totalPrice) : null,
        status: this.formData.status
      };

      this.apiService.updateTransactionByAdmin(this.transactionId, updateData).subscribe({
        next: (response) => {
          if (response.status === 200) {
            this.notificationService.showSuccess('Success', 'Transaction updated successfully');
            this.router.navigate(['/transaction']);
          }
        },
        error: (error) => {
          this.notificationService.showError('Error', 
            error?.error?.message || 'Failed to update transaction');
          console.error('Error updating transaction:', error);
        }
      });
    }
  }

  validateForm(): boolean {
    if (!this.formData.productId) {
      this.notificationService.showError('Validation Error', 'Please select a product');
      return false;
    }
    if (!this.formData.quantity || parseInt(this.formData.quantity) <= 0) {
      this.notificationService.showError('Validation Error', 'Please enter a valid quantity');
      return false;
    }
    return true;
  }

  private handleWebSocketMessage(message: any): void {
    // Chỉ xử lý message cho transaction hiện tại
    if (message.data && message.data.id.toString() === this.transactionId) {
      switch (message.type) {
        case "TRANSACTION_UPDATED_RENDER":
      this.loadTransaction();
      this.loadProducts();
      this.loadSuppliers();
      this.loadUsers();
          this.notificationService.showInfo('Cập nhật giao dịch', `Giao dịch ID: ${message.data.id} đã được cập nhật trạng thái`);
          break;
          case 'TRANSACTION_DELETED_RENDER':
        this.router.navigate(['/transation']);
        this.notificationService.showWarning('Giao dịch đã xóa', `Giao dịch ID: ${message.data.id} đã bị xóa`);
        break;
      }
    }
  }

  cancel(): void {
    this.router.navigate(['/transaction']);
  }

  isAdmin(): boolean {
    return this.apiService.isAdmin();
  }

  ngOnDestroy(): void {
    if (this.wsSubscription) {
      this.wsSubscription.unsubscribe();
    }
  }
}