import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../service/api.service';
import { NotificationService } from '../service/notification.service';
import { ActivatedRoute, Router } from '@angular/router';
import { QrScannerComponent } from '../qr-scanner/qr-scanner.component';

@Component({
  selector: 'app-transaction-details',
  standalone: true,
  imports: [CommonModule, FormsModule, QrScannerComponent],
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
  user:any=null;
  supplierId=-1;
  isCustomer=false;
  type:string='';
  isAdmin=false;
  showQrScanner: boolean = false;
  canGenQR:boolean=false;
  canScanQR:boolean=false;

  ngOnInit(): void {
    //extract transaction id from routes
    this.route.params.subscribe(params =>{
      this.transactionId = params['transactionId'];
      this.apiService.getLoggedInUserInfo().subscribe({
          next:(res)=>{
            this.user = res;
            this.getTransactionDetails();
          },
          error: (error) => {
            this.notificationService.showError('Error',
              error?.error?.message ||
                error?.message ||
                'Unable to Get Profile Info: ' + error
            );
          }
        });
    });
  }

  getTransactionDetails():void{
    if (this.transactionId) {
      this.apiService.getTransactionById(this.transactionId).subscribe({
        next:(transactionData: any) =>{
          if (transactionData.status === 200) {
            this.transaction = transactionData.transaction;
            this.status = this.transaction.status;
            this.type = this.transaction.transactionType;
            this.isAdmin = (this.transaction.user)?(this.transaction.user.role=="ADMIN"?true:false):false;
            this.isCustomer = (this.transaction.user)?(this.transaction.user.role=="CUSTOMER"?true:false):false;
            if(this.transaction.transactionType!="RETURN_TO_SUPPLIER"&&this.transaction.supplier.id==this.user.supplier.id&&this.transaction.status=='PENDING'){
              this.canGenQR=true;
            }
            if(this.transaction.transactionType=="RETURN_TO_SUPPLIER"&&this.transaction.user.id==this.user.id&&this.transaction.status=='PENDING'){
              this.canGenQR=true;
            }
            if(!this.isCustomer&&this.transaction.user.supplier.id==this.user.supplier.id&&this.transaction.status!="COMPLETED"){
              this.canScanQR=true;
            }
            if(this.isCustomer&&this.transaction.user.id==this.user.id){
              this.canScanQR=true;
            }
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

  confirmReturn():void{
    this.status = 'PENDING';
    this.updateTransactionStatus('PENDING');
  }

  rejectReturn(): void{
    this.status='CANCELED';
    this.updateTransactionStatus('CANCELED');
  }
  
  navigateToReturnPage():void{
    this.router.navigate([`/transaction/return/${this.transaction.id}`]);
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

  confirmArrival(): void{
    // Mở QR Scanner để quét mã QR
    this.showQrScanner = true;
  }

  generatePDF(): void {
    if(this.isCustomer){
      if(this.transaction.transactionType=="RETURN_TO_SUPPLIER"&&this.transaction.status=="PENDING"&&this.transaction.user.id==this.user.id){
        this.apiService.exportInvoiceWithQR(this.transaction.id, this.transaction.status);
      }
    }
    else{
      this.apiService.getLoggedInUserInfo().subscribe({
          next:(res:any)=>{
            if(res.status==200)
            this.supplierId = res.supplier.id;
          },
          error: (error) => {
            this.notificationService.showError('Error',
              error?.error?.message ||
                error?.message ||
                'Unable to Get Supplier info: ' + error
            );
          }
        })
      if(this.canGenQR){
        this.apiService.exportInvoiceWithQR(this.transaction.id, this.transaction.status);
      }
    }
  }

  // Đóng QR Scanner
  onCloseQrScanner(): void {
    this.showQrScanner = false;
  }
}
