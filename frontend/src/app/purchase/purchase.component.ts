import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import { ApiService } from '../service/api.service';
import { NotificationService } from '../service/notification.service';
import { WebSocketService } from '../service/websocket.service';

@Component({
  selector: 'app-purchase',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './purchase.component.html',
  styleUrl: './purchase.component.css'
})
export class PurchaseComponent implements OnInit, OnDestroy {
  private webSocketSubscription?: Subscription;
  
  constructor(
    private apiService: ApiService,
    private route: ActivatedRoute,
    private notificationService: NotificationService,
    private webSocketService: WebSocketService
  ){}

  products: any[] = []
  suppliers: any[] = []
  users: any[] = []
  productId:string = ''
  supplierId:string = ''
  senderId:string = ''
  description:string = ''
  quantity:number = 0
  price:number=0
  message:string = ''
  selectedProduct: any = null
  maxPrice: number = 0
  createdAt:string = ''
  status:string = ''
  availableSenders: any[] = []
  

  ngOnInit(): void {
    // Check for productId in query params and pre-select it
    this.route.queryParams.subscribe(params => {
      if (params['productId']) {
        this.productId = params['productId'];
      }
    });
    
    this.fetchProductsAndSuppliers();
    
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
    if (message.type === 'TRANSACTION_PURCHASED_RENDER') {
      this.notificationService.showSuccess('Thành công', `Giao dịch mua ID: ${message.data.id} đã được tạo`);
    }
  }

  onProductChange(): void {
    if (this.productId) {
      this.selectedProduct = this.products.find(p => p.id == this.productId);
      if (this.selectedProduct) {
        this.maxPrice = Math.max(0, this.selectedProduct.price - 10000);
        this.price = this.selectedProduct.price*0.8;
      }
    } else {
      this.selectedProduct = null;
      this.maxPrice = 0;
      this.price = 0;
    }
  }

  onSupplierChange(): void {
    if (this.supplierId && this.apiService.isAdmin()) {
      // Filter users who have the selected supplier ID
    if (this.apiService.isAdmin()) {
      this.apiService.getUserBySupplierId(this.supplierId).subscribe({
        next: (res: any) => {
          if (res.status === 200) {
            this.availableSenders = res.users;
          }
        },
        error: (error) => {
          this.notificationService.showError('Error',
            error?.error?.message ||
              error?.message ||
              'Không thể lấy danh sách người dùng: ' + error
          );
        },
      });
    }
      this.senderId = ''; // Reset sender selection
    } else {
      this.availableSenders = [];
      this.senderId = '';
    }
  }

  isAdmin(): boolean {
    return this.apiService.isAdmin();
  }

  fetchProductsAndSuppliers():void{
    this.apiService.getAllProducts("").subscribe({
      next: (res: any) => {
        if (res.status === 200) {
          this.products = res.products;
          // If we have a pre-selected productId, trigger onProductChange
          if (this.productId) {
            this.onProductChange();
          }
        }
      },
      error: (error) => {
        this.notificationService.showError('Error',
          error?.error?.message ||
            error?.message ||
            'Không thể lấy danh sách sản phẩm: ' + error
        );
      },
    });

    this.apiService.getAllSuppliers().subscribe({
      next: (res: any) => {
        if (res.status === 200) {
          // Filter out supplier with id = 7
          this.suppliers = res.suppliers.filter((supplier: any) => supplier.id != 7);
        }
      },
      error: (error) => {
        this.notificationService.showError('Error',
          error?.error?.message ||
            error?.message ||
            'Không thể lấy danh sách nhà cung cấp: ' + error
        );
      },
    });
  }

  //Handle form submission
  handleSubmit():void{
    if (!this.productId || !this.supplierId || !this.quantity || !this.price) {
      this.notificationService.showError('Error', "Vui lòng điền đầy đủ thông tin");
      return;
    }

    const priceValue = this.price;
    if (priceValue < 1 || priceValue > this.maxPrice) {
      this.notificationService.showError('Error', `Giá phải nằm trong khoảng 1 và ${this.maxPrice}`);
      return;
    }

    const body: any = {
      productId: this.productId,
      supplierId: this.supplierId,
      quantity:  this.quantity,
      price: priceValue,
      description: this.description
    }

    // Add admin-only fields if user is admin
    if (this.isAdmin()) {
      if (this.senderId) {
        body.senderId = this.senderId;
      }
      if (this.createdAt) {
        body.createdAt = this.createdAt;
      }
      if (this.status) {
        body.status = this.status;
      }
    }

    this.apiService.purchaseProduct(body).subscribe({
      next: (res: any) => {
        if (res.status === 200) {
          this.notificationService.showSuccess('Success', res.message);
          this.resetForm();
        }
      },
      error: (error) => {
        this.notificationService.showError('Error',
          error?.error?.message ||
            error?.message ||
            'Không thể nhập hàng: ' + error
        );
      },
    })

  }

  get totalPrice(): number{
    return this.price*this.quantity;
  }
  
  resetForm():void{
    this.productId = '';
    this.supplierId = '';
    this.senderId = '';
    this.description = '';
    this.quantity = 0;
    this.price = 0;
    this.selectedProduct = null;
    this.maxPrice = 0;
    this.createdAt = '';
    this.status = '';
    this.availableSenders = [];
  }
}
