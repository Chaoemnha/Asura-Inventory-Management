import { Component, OnDestroy, OnInit } from '@angular/core';
import { CustomPaginationComponent } from '../custom-pagination/custom-pagination.component';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ApiService } from '../service/api.service';
import { Router } from '@angular/router';
import { NotificationService } from '../service/notification.service';
import { Subscription } from 'rxjs';
import { WebSocketService } from '../service/websocket.service';

interface Transaction {
  id: string;
  transactionType: string;
  status: string;
  totalPrice: number;
  totalProducts: number;
  createdAt: string;
  product: {
    id: string;
    name: string;
    sku: string;
  };
}

@Component({
  selector: 'app-transaction',
  standalone: true,
  imports: [CustomPaginationComponent, FormsModule, CommonModule],
  templateUrl: './transaction.component.html',
  styleUrl: './transaction.component.css'
})
export class TransactionComponent implements OnInit, OnDestroy {
  constructor(
    private apiService: ApiService, 
    private router: Router,
    private notificationService: NotificationService,
    private webSocketService: WebSocketService,
  ){}

  transactions: Transaction[] = [];  
  searchInput:string = '';
  valueToSearch:string = '';
  
  // Search by ID properties
  searchId: string = '';
  isSearching: boolean = false;
  
  // Advanced search properties
  searchType: string = '';
  searchStatus: string = '';
  searchProductName: string = '';
  searchFromDate: string = '';
  searchToDate: string = '';
  showAdvancedSearch: boolean = false;
  
  currentPage: number = 1;
  totalPages: number = 0;
  itemsPerPage: number = 10;
  userId: number = -1;
  private wsSubscription!: Subscription;
  
  // Alert properties for role-based warnings
  userRole: string = '';
  alertCounts: any = {};

  ngOnInit(): void {
    this.userRole = this.apiService.getUserRole() || '';
    this.loadTransactions();
    this.calculateAlertCounts();
    this.wsSubscription = this.webSocketService.getMessages().subscribe({
      next: (message: any) => {
        this.handleWebSocketMessage(message);
      },
      error: (error: any) => {
        console.error('WebSocket error in SupplierComponent:', error);
      }
    });
  }

  isAdmin(): boolean {
    return this.apiService.isAdmin();
  }

  calculateAlertCounts(): void {
    this.apiService.getLoggedInUserInfo().subscribe({
      next: (res) => {
        const userId = this.isAdmin() ? -1 : res.id;
        this.apiService.getAllTransactions(userId, '', '', '', '', '').subscribe({
          next: (response: any) => {
            const allTransactions = response.transactions || [];
            this.alertCounts = this.countTransactionsByStatus(allTransactions);
          },
          error: (error) => {
            console.error('Error fetching transactions for alerts:', error);
          }
        });
      },
      error: (error) => {
        console.error('Error fetching user info for alerts:', error);
      }
    });
  }

  countTransactionsByStatus(transactions: Transaction[]): any {
    const counts = {
      PENDING: 0,
      SENDER_DECIDING: 0,
      ADMIN_DECIDING: 0,
      SENDER_DECLINED: 0
    };

    transactions.forEach(transaction => {
      if (counts.hasOwnProperty(transaction.status)) {
        counts[transaction.status as keyof typeof counts]++;
      }
    });

    return counts;
  }

  getAlertMessage(): string {
    const role = this.userRole;
    let messages: string[] = [];

    switch (role) {
      case 'SUPPLIER':
        if (this.alertCounts.SENDER_DECIDING > 0) {
          messages.push(`${this.alertCounts.SENDER_DECIDING} giao dịch cần quyết định (SENDER_DECIDING)`);
        }
        if (this.alertCounts.PENDING > 0) {
          messages.push(`${this.alertCounts.PENDING} giao dịch đang làm (PENDING)`);
        }
        break;
      case 'STOCKSTAFF':
        if (this.alertCounts.SENDER_DECIDING > 0) {
          messages.push(`${this.alertCounts.SENDER_DECIDING} giao dịch cần quyết định (SENDER_DECIDING)`);
        }
        if (this.alertCounts.PENDING > 0) {
          messages.push(`${this.alertCounts.PENDING} giao dịch đang làm (PENDING)`);
        }
        break;
      case 'CUSTOMER':
        if (this.alertCounts.PENDING > 0) {
          messages.push(`${this.alertCounts.PENDING} giao dịch đang làm (PENDING)`);
        }
        break;
      case 'ADMIN':
        if (this.alertCounts.ADMIN_DECIDING > 0) {
          messages.push(`${this.alertCounts.ADMIN_DECIDING} giao dịch cần quyết định (ADMIN_DECIDING)`);
        }
        if (this.alertCounts.SENDER_DECLINED > 0) {
          messages.push(`${this.alertCounts.SENDER_DECLINED} giao dịch bị từ chối (SENDER_DECLINED)`);
        }
        if (this.alertCounts.PENDING > 0) {
          messages.push(`${this.alertCounts.PENDING} giao dịch đang làm (PENDING)`);
        }
        break;
    }

    return messages.join(' | ');
  }

  shouldShowAlert(): boolean {
    const role = this.userRole;
    switch (role) {
      case 'SUPPLIER':
        return this.alertCounts.SENDER_DECIDING > 0 || this.alertCounts.PENDING > 0;
      case 'STOCKSTAFF':
        return this.alertCounts.SENDER_DECIDING > 0 || this.alertCounts.PENDING > 0;
      case 'CUSTOMER':
        return this.alertCounts.PENDING > 0;
      case 'ADMIN':
        return this.alertCounts.ADMIN_DECIDING > 0 || this.alertCounts.SENDER_DECLINED > 0 || this.alertCounts.PENDING > 0;
      default:
        return false;
    }
  }

  private handleWebSocketMessage(message: any): void {    
    switch (message.type) {
      case "TRANSACTION_UPDATED_RENDER":
        this.updateTransactionInList(message.data);
        this.notificationService.showInfo('Cập nhật giao dịch', `Giao dịch ID: ${message.data.id} đã được cập nhật`);
        break;
        
      case 'TRANSACTION_PURCHASED_RENDER':
        this.addTransactionToList(message.data);
        this.notificationService.showSuccess('Giao dịch mới', `Giao dịch nhập hàng ID: ${message.data.id} đã được tạo`);
        break;
        
      case 'TRANSACTION_SOLD_RENDER':
        this.addTransactionToList(message.data);
        this.notificationService.showSuccess('Giao dịch mới', `Giao dịch bán hàng ID: ${message.data.id} đã được tạo`);
        break;
        
      case 'TRANSACTION_RETURNED_RENDER':
        this.addTransactionToList(message.data);
        this.notificationService.showInfo('Giao dịch trả hàng', `Giao dịch trả hàng ID: ${message.data.id} đã được tạo`);
        break;
        
      case 'TRANSACTION_DELETED_RENDER':
        this.removeTransactionFromList(message.data.id);
        this.notificationService.showWarning('Giao dịch đã xóa', `Giao dịch ID: ${message.data.id} đã bị xóa`);
        break;
              
    }
    
    // Refresh alert counts sau khi có thay đổi
    this.calculateAlertCounts();
  }

  ngOnDestroy(): void {
    if (this.wsSubscription) {
      this.wsSubscription.unsubscribe();
    }
    this.webSocketService.disconnect();
  }

  private updateTransactionInList(updatedTransaction: Transaction): void{
    const index = this.transactions.findIndex(trans => trans.id === updatedTransaction.id);
    if(index !== -1){
      this.transactions[index] = updatedTransaction;
    }
  }
  
  private addTransactionToList(newTransaction: Transaction): void {
    const exists = this.transactions.some(trans => trans.id === newTransaction.id);
    if (!exists) {
      this.transactions.unshift(newTransaction);
      this.transactions.pop();
    }
  }

  private removeTransactionFromList(transactionId: string): void {
    this.transactions = this.transactions.filter(trans => trans.id !== transactionId);
  }
  //FETCH Transactions

  loadTransactions(): void {
    this.apiService.getLoggedInUserInfo().subscribe({
          next:(res)=>{
            if(this.isAdmin()) {this.userId = -1} 
            else{this.userId = res.id;} 
            // Gọi getAllTransactions sau khi đã có thông tin user
            this.apiService.getAllTransactions(
              this.userId, 
              this.searchType,
              this.searchStatus,
              this.searchProductName,
              this.searchFromDate,
              this.searchToDate
            ).subscribe({
              next: (res: any) => {
                const transactions = res.transactions || [];

                this.totalPages = Math.ceil(transactions.length / this.itemsPerPage);

                this.transactions = transactions.slice(
                  (this.currentPage - 1) * this.itemsPerPage,
                  this.currentPage * this.itemsPerPage
                );
                
                // Recalculate alerts after loading transactions
                this.calculateAlertCounts();
              },
              error: (error) => {
                this.notificationService.showError('Lỗi', 
                  error?.error?.message ||
                  error?.message ||
                  'Không thể tải giao dịch: ' + error
                );
              },
            });
          },
          error: (error) => {
            this.notificationService.showError('Lỗi', 
              error?.error?.message ||
              error?.message ||
              'Không thể lấy thông tin người dùng: ' + error
            );
          }
        })
  }

  //HANDLE SEARCH BY ID
  handleSearch(): void {
    if (!this.searchId.trim()) {
      this.notificationService.showError('Lỗi', 'Vui lòng nhập mã giao dịch');
      return;
    }

    this.isSearching = true;
    this.currentPage = 1;

    // Tim kiem giao dich qua id
    this.apiService.getTransactionById(this.searchId.trim()).subscribe({
      next: (res: any) => {
        if (res.status === 200 && res.transaction) {
          this.transactions = [res.transaction];
          this.totalPages = 1;
          this.notificationService.showSuccess('Success', 'Tìm thấy giao dịch!');
        } else {
          this.transactions = [];
          this.totalPages = 0;
          this.notificationService.showError('Error', 'Transaction not found');
        }
      },
      error: (error) => {
        this.transactions = [];
        this.totalPages = 0;
        this.isSearching = false;
        this.notificationService.showError('Error',
          error?.error?.message ||
          error?.message ||
          'Không tìm thấy hoặc không thể tìm giao dịch: ' + error
        );
      }
    });
  }

  //HANDLE CANCEL SEARCH
  handleCancel(): void {
    this.searchId = '';
    this.isSearching = false;
    this.currentPage = 1;
    this.loadTransactions(); 
  }

  handleAdvancedSearch(): void {
    this.currentPage = 1;
    this.loadTransactions();
  }

  toggleAdvancedSearch(): void {
    this.showAdvancedSearch = !this.showAdvancedSearch;
    if (!this.showAdvancedSearch) {
      this.clearAdvancedSearch();
    }
  }

  // Clear advanced search filters
  clearAdvancedSearch(): void {
    this.searchType = '';
    this.searchStatus = '';
    this.searchProductName = '';
    this.searchFromDate = '';
    this.searchToDate = '';
    // Only reload if not searching by ID
    if (!this.isSearching) {
      this.loadTransactions();
    }
  }

  //NAVIGATE TO TRANSACTIONS DETAILS PAGE
  navigateTOTransactionsDetailsPage(transactionId: string):void{
    this.router.navigate([`/transaction/${transactionId}`]);
  }

  //NAVIGATE TO EDIT TRANSACTION PAGE
  navigateToEditTransaction(transactionId: string): void {
    this.router.navigate([`/edit-transaction/${transactionId}`]);
  }

  //FORMAT CURRENCY
  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(amount);
  }

    //HANDLE PAGE CHANGE. NAVIGATE TO NEXT, PREVIOUS OR SPECIFIC PAGE CHANGE
    onPageChange(page: number): void {
      this.currentPage = page;
      // Don't reload if searching by ID (single result)
      if (!this.isSearching) {
        this.loadTransactions();
      }
    }

    //EXPORT TRANSACTIONS
    exportTransactions(type: string): void {
      const baseUrl = 'http://localhost:8080/api'; 
      // Tạo URL với query parameters
      let url = `${baseUrl}/transactions/export?type=${type}`;
      
      // Thêm các filter parameters nếu có
      if (this.searchType) {
        url += `&searchType=${encodeURIComponent(this.searchType)}`;
      }
      if (this.searchStatus) {
        url += `&searchStatus=${encodeURIComponent(this.searchStatus)}`;
      }
      if (this.searchProductName) {
        url += `&searchProductName=${encodeURIComponent(this.searchProductName)}`;
      }
      if (this.searchFromDate) {
        url += `&searchFromDate=${encodeURIComponent(this.searchFromDate)}`;
      }
      if (this.searchToDate) {
        url += `&searchToDate=${encodeURIComponent(this.searchToDate)}`;
      }
      
      // Add user/supplier filters
      if (this.userId !== -1) {
        url += `&userId=${this.userId}`;
      }
      
      // Download file với các filter đã áp dụng
      this.apiService.downloadFile(url, type);
    }

}
