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
  currentPage: number = 1;
  totalPages: number = 0;
  itemsPerPage: number = 10;
  userId: number = -1;
  supplierId: number = -1;
  private wsSubscription!: Subscription;

  ngOnInit(): void {
    this.loadTransactions();
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

  private handleWebSocketMessage(message: any): void {    
    switch (message.type) {
      case "TRANSACTION_UPDATED_RENDER":
        this.updateTransactionInList(message.data);
        break;
        
      case 'TRANSACTION_PURCHASED_RENDER':
        this.addTransactionToList(message.data);
        break;
        
      case 'TRANSACTION_SOLD_RENDER':
        this.addTransactionToList(message.data);
        break;
        
      case 'TRANSACTION_RETURNED_RENDER':
        this.addTransactionToList(message.data);
        break;
              
    }
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
      this.transactions.push(newTransaction);
    }
  }
  //FETCH Transactions

  loadTransactions(): void {
    this.apiService.getLoggedInUserInfo().subscribe({
          next:(res)=>{
            if(this.isAdmin()) this.userId = -1;
            else this.userId = res.id;
            this.supplierId = res.supplier?res.supplier.id:-1;
            this.apiService.getAllTransactions(this.valueToSearch, this.userId, this.supplierId).subscribe({
              next: (res: any) => {
                const transactions = res.transactions || [];

                this.totalPages = Math.ceil(transactions.length / this.itemsPerPage);

                this.transactions = transactions.slice(
                  (this.currentPage - 1) * this.itemsPerPage,
                  this.currentPage * this.itemsPerPage
                );
              },
              error: (error) => {
                this.notificationService.showError('Error',
                  error?.error?.message ||
                    error?.message ||
                    'Unable to Get all Transactions: ' + error
                );
              },
            });
          },
          error: (error) => {
            this.notificationService.showError('Error',
              error?.error?.message ||
                error?.message ||
                'Unable to Get Profile Info: ' + error
            );
          }
        })
  }

  //HANDLE SEARCH
  handleSearch():void{
    this.currentPage = 1; // Reset to first page when searching
    this.valueToSearch = this.searchInput;
    this.loadTransactions();
  }

  //NAVIGATE TO TRANSACTIONS DETAILS PAGE
  navigateTOTransactionsDetailsPage(transactionId: string):void{
    this.router.navigate([`/transaction/${transactionId}`]);
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
      this.loadTransactions();
    }

    //EXPORT TRANSACTIONS
    exportTransactions(type: string): void {
      const baseUrl = 'http://localhost:8080/api'; 
      // Tao link de down file ve
      const url = `${baseUrl}/transactions/export?type=${type}`;      
      // Add authorization header by creating a form and submitting it
      this.apiService.downloadFile(url, type);
    }

}
