import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import { ApiService } from '../service/api.service';
import { NotificationService } from '../service/notification.service';
import { WebSocketService } from '../service/websocket.service';

@Component({
  selector: 'app-sell',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './sell.component.html',
  styleUrl: './sell.component.css'
})
export class SellComponent implements OnInit, OnDestroy {
  private webSocketSubscription?: Subscription;

  constructor(
    private apiService: ApiService,
    private notificationService: NotificationService,
    private route: ActivatedRoute,
    private webSocketService: WebSocketService
  ){}

  products: any[] = []
  users: any[] = []
  user: any =null
  productId:string = ''
  userId:string = ''
  description:string = ''
  quantity:string = ''
  userRole: string = ''
  isCustomer: boolean = false
  isAdminOrStaff: boolean = false
  selectedProduct: any = null
  unitPrice: number = 0
  totalPrice: number = 0
  createdAt:string = ''
  status:string = ''



  ngOnInit(): void {
    this.checkUserRole();
    this.fetchProducts();
    if (this.isAdminOrStaff) {
      this.fetchUsers();
    }
    
    // Check for productId in query params and pre-select it
    this.route.queryParams.subscribe(params => {
      if (params['productId']) {
        this.productId = params['productId'];
      }
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
    if (message.type === 'TRANSACTION_SOLD_RENDER') {
      this.notificationService.showSuccess('Thành công', `Giao dịch bán ID: ${message.data.id} đã được tạo`);
    }
  }

  checkUserRole(): void {
    this.userRole = this.apiService.getUserRole() || '';
    this.isCustomer = this.userRole === 'CUSTOMER';
    this.isAdminOrStaff = this.userRole === 'ADMIN' || this.userRole === 'STOCKSTAFF';
  }

  fetchUsers(): void {
    this.apiService.getAllUsers().subscribe({
      next: (res: any) => {
        if (res.status === 200) {
          this.users = res.users.filter((user: any) => user.role === 'CUSTOMER');
        }
      },
      error: (error: any) => {
        this.notificationService.showError('Error',
          error?.error?.message ||
            error?.message ||
            'Không thể lấy danh sách người dùng: ' + error
        );
      },
    });
    this.apiService.getLoggedInUserInfo().subscribe({
       next: (res) => {
        this.user = res;
      },
      error: (error) => {
        this.notificationService.showError('Error',
          error?.error?.message ||
          error?.message ||
          'Không thể lấy thông tin người dùng: ' + error
        );
      }
    })
  }

  fetchProducts():void{
    this.apiService.getAllProducts("").subscribe({
      next: (res: any) => {
        if (res.status === 200) {
          this.products = res.products;
          // If we have a pre-selected productId, trigger price calculation
          if (this.productId) {
            this.onProductChange();
          }
        }
      },
      error: (error) => {
        this.notificationService.showError('Lỗi',
          error?.error?.message ||
            error?.message ||
            'Không thể lấy danh sách sản phẩm: ' + error
        );
      },
    });

  }

  onProductChange(): void {
    if (this.productId) {
      this.selectedProduct = this.products.find(p => p.id == this.productId);
      if (this.selectedProduct) {
        this.unitPrice = this.selectedProduct.price;
        this.calculateTotalPrice();
      }
    } else {
      this.selectedProduct = null;
      this.unitPrice = 0;
      this.totalPrice = 0;
    }
  }

  onQuantityChange(): void {
    this.calculateTotalPrice();
  }

  calculateTotalPrice(): void {
    const qty = parseInt(this.quantity) || 0;
    this.totalPrice = this.unitPrice * qty;
  }

  handleSubmit():void{
    if (!this.productId || !this.quantity) {
      this.notificationService.showError('Lỗi', "Vui lòng điền đầy đủ thông tin");
      return;
    }

    if (this.isAdminOrStaff && !this.userId) {
      this.notificationService.showError('Lỗi', "Vui lòng chọn khách hàng");
      return;
    }

    const body: any = {
      productId: this.productId,
      quantity:  parseInt(this.quantity, 10),
      description: this.description
    }

    if (this.isAdminOrStaff) {
      body.userId = this.userId;
      body.senderId = this.user.id;
    }

    // Add admin-only fields if user is admin
    if (this.userRole === 'ADMIN') {
      if (this.createdAt) {
        body.createdAt = this.createdAt;
      }
      if (this.status) {
        body.status = this.status;
      }
    }

    this.apiService.sellProduct(body).subscribe({
      next: (res: any) => {
        if (res.status === 200) {
          this.notificationService.showSuccess('Success', res.message);
          this.resetForm();
        }
      },
      error: (error: any) => {
        this.notificationService.showError('Lỗi',
          error?.error?.message ||
            error?.message ||
            'Không thể bán sản phẩm: ' + error
        );
      },
    })

  }

  
  resetForm():void{
    this.productId = '';
    this.userId = '';
    this.description = '';
    this.quantity = '';
    this.selectedProduct = null;
    this.unitPrice = 0;
    this.totalPrice = 0;
    this.createdAt = '';
    this.status = '';
  }

  isAdmin(): boolean {
    return this.userRole === 'ADMIN';
  }
}

