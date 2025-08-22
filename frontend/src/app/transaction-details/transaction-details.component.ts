import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../service/api.service';
import { NotificationService } from '../service/notification.service';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-transaction-details',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './transaction-details.component.html',
  styleUrl: './transaction-details.component.css',
})
export class TransactionDetailsComponent implements OnInit {
 
  constructor(
    private apiService: ApiService,
    private route: ActivatedRoute,
    private router: Router,
    private notificationService: NotificationService
  ) {}

  transactionId:string | null = '';
  transaction: any = null;
  status:string = '';

  ngOnInit(): void {
    //extract transaction id from routes
    this.route.params.subscribe(params =>{
      this.transactionId = params['transactionId'];
      this.getTransactionDetails();
    })
  }

  getTransactionDetails():void{
    if (this.transactionId) {
      this.apiService.getTransactionById(this.transactionId).subscribe({
        next:(transactionData: any) =>{
          if (transactionData.status === 200) {
            this.transaction = transactionData.transaction;
            this.status = this.transaction.status;
          }
        },
        error:(error)=>{
          this.notificationService.showError('Error',
            error?.error?.message ||
              error?.message ||
              'Unable to Get Transaction by id: ' + error
          );
        }
      })
    }
  }

  //UPDATE STATUS
  handleUpdateStatus():void{
    if (this.transactionId && this.status) {
      this.apiService.updateTransactionStatus(this.transactionId, this.status).subscribe({
        next:(result)=>{
          this.notificationService.showSuccess('Success', 'Transaction status updated successfully');
          this.router.navigate(['/transaction']);
        },
        error:(error)=>{
          this.notificationService.showError('Error',
            error?.error?.message ||
              error?.message ||
              'Unable to Update Transaction: ' + error
          );
        }
      })
    }
  }

  // Specific button actions
  confirmPayment(): void {
    this.status = 'COMPLETED';
    this.updateTransactionStatus('COMPLETED');
  }

  cancelPayment(): void {
    this.status = 'CANCELED';
    this.updateTransactionStatus('CANCELED');
  }

  rerequestPayment(): void {
    this.status = 'PENDING';
    this.updateTransactionStatus('PENDING');
  }

  private updateTransactionStatus(newStatus: string): void {
    if (this.transactionId) {
      this.apiService.updateTransactionStatus(this.transactionId, newStatus).subscribe({
        next:(result)=>{
          this.notificationService.showSuccess('Success', `Transaction status updated to ${newStatus}`);
          this.getTransactionDetails(); // Refresh the transaction data
        },
        error:(error)=>{
          this.notificationService.showError('Error',
            error?.error?.message ||
              error?.message ||
              'Unable to Update Transaction: ' + error
          );
        }
      })
    }
  }

  generatePDF(): void {
    this.notificationService.showInfo('Info', 'PDF generation feature will be implemented soon');
  }
}
