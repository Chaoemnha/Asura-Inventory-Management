import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { ApiService } from '../service/api.service';
import { NotificationService } from '../service/notification.service';
import { WebSocketService } from '../service/websocket.service';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-return',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './return.component.html',
  styleUrl: './return.component.css'
})
export class ReturnComponent implements OnInit, OnDestroy {
  private webSocketSubscription?: Subscription;
  
  constructor(
    private apiService: ApiService,
    private route: ActivatedRoute,
    private router: Router,
    private notificationService: NotificationService,
    private webSocketService: WebSocketService
  ) {}

  transactionId: string | null = '';
  transaction: any = null;
  returnReason: string = '';
  returnQuantity: number = 0;
  maxReturnQuantity: number = 0;
  user: any = null;
  isLoading: boolean = false;

  ngOnInit(): void {
    // Extract transaction id from routes
    this.route.params.subscribe(params => {
      this.transactionId = params['transactionId'];
      this.loadUserInfoAndTransaction();
    });
    
    // Subscribe to WebSocket messages
    this.webSocketSubscription = this.webSocketService.getMessages().subscribe(
      (message: any) => {
        this.handleWebSocketMessage(message);
      }
    );
  }

  ngOnDestroy(): void {
    if (this.webSocketSubscription) {
      this.webSocketSubscription.unsubscribe();
    }
  }

  private handleWebSocketMessage(message: any): void {
    if (message.type === 'TRANSACTION_RETURNED_RENDER') {
      this.notificationService.showSuccess('Thành công', `Giao dịch trả hàng ID: ${message.data.id} đã được tạo`);
    }
  }

  loadUserInfoAndTransaction(): void {
    this.apiService.getLoggedInUserInfo().subscribe({
      next: (res) => {
        this.user = res;
        this.getTransactionDetails();
      },
      error: (error) => {
        this.notificationService.showError('Error',
          error?.error?.message ||
          error?.message ||
          'Không thể lấy thông tin tài khoản: ' + error
        );
      }
    });
  }

  getTransactionDetails(): void {
    if (this.transactionId) {
      this.apiService.getTransactionById(this.transactionId).subscribe({
        next: (transactionData: any) => {
          if (transactionData.status === 200) {
            this.transaction = transactionData.transaction;
            this.maxReturnQuantity = this.transaction.totalProducts;
            this.returnQuantity = this.maxReturnQuantity; // Default to full quantity
          }
        },
        error: (error) => {
          this.notificationService.showError('Error',
            error?.error?.message ||
            error?.message ||
            'Không thể lấy thông tin giao dịch: ' + error
          );
        }
      });
    }
  }

  // Validate if user can return this transaction
  canReturn(): boolean {
    if (!this.transaction || !this.user) return false;
    
    // Only the buyer can return
    if (this.transaction.user.id !== this.user.id&&this.user.role!='ADMIN') return false;
    
    // Only completed transactions can be returned
    if (this.transaction.status !== 'COMPLETED') return false;
    if (this.transaction.transactionType !== 'PURCHASE'&&this.transaction.transactionType !== 'SALE') return false;
    
    return true;
  }

  // Handle form submission
  handleReturnSubmit(): void {
    if (!this.returnReason.trim()) {
      this.notificationService.showError('Error', 'Vui lòng nhập lý do trả hàng');
      return;
    }

    if (this.returnQuantity <= 0 || this.returnQuantity > this.maxReturnQuantity) {
      this.notificationService.showError('Error', `Số lượng trả phải từ 1 đến ${this.maxReturnQuantity}`);
      return;
    }

    if (!this.canReturn()) {
      this.notificationService.showError('Error', 'Bạn không có quyền trả giao dịch này');
      return;
    }

    const returnData = {
      productId: this.transaction.product.id,
      quantity: this.returnQuantity,
      description: this.returnReason,
      supplierId: this.transaction.supplier?.id || null
    };

    this.isLoading = true;

    this.apiService.returnProduct(returnData).subscribe({
      next: (res: any) => {
        this.isLoading = false;
        if (res.status === 200) {
          this.notificationService.showSuccess('Success', 'Gửi yêu cầu trả hàng thành công');
          this.router.navigate(['/transaction']);
        }
      },
      error: (error: any) => {
        this.isLoading = false;
        this.notificationService.showError('Error',
          error?.error?.message ||
          error?.message ||
          'Không thể gửi yêu cầu trả hàng: ' + error
        );
      }
    });
  }

  // Cancel and go back
  cancelReturn(): void {
    this.router.navigate(['/transaction', this.transactionId]);
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(amount);
  }

  // Calculate return amount
  getReturnAmount(): number {
    if (!this.transaction) return 0;
    const pricePerItem = this.transaction.totalPrice / this.transaction.totalProducts;
    return pricePerItem * this.returnQuantity;
  }
}