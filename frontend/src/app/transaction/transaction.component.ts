import { Component, OnInit } from '@angular/core';
import { CustomPaginationComponent } from '../custom-pagination/custom-pagination.component';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ApiService } from '../service/api.service';
import { Router } from '@angular/router';
import { NotificationService } from '../service/notification.service';

@Component({
  selector: 'app-transaction',
  standalone: true,
  imports: [CustomPaginationComponent, FormsModule, CommonModule],
  templateUrl: './transaction.component.html',
  styleUrl: './transaction.component.css'
})
export class TransactionComponent implements OnInit {
  constructor(
    private apiService: ApiService, 
    private router: Router,
    private notificationService: NotificationService
  ){}

  transactions: any[] = [];  
  searchInput:string = '';
  valueToSearch:string = '';
  currentPage: number = 1;
  totalPages: number = 0;
  itemsPerPage: number = 10;

  ngOnInit(): void {
    this.loadTransactions();
  }


  //FETCH Transactions

  loadTransactions(): void {
    this.apiService.getAllTransactions(this.valueToSearch).subscribe({
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
