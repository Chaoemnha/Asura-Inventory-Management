import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../service/api.service';
import { NotificationService } from '../service/notification.service';
import { ActivatedRoute, Router } from '@angular/router';
import { QrScannerComponent } from '../qr-scanner/qr-scanner.component';
import { WebSocketService } from '../service/websocket.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-transaction-details',
  standalone: true,
  imports: [CommonModule, FormsModule, QrScannerComponent],
  templateUrl: './transaction-details.component.html',
  styleUrl: './transaction-details.component.css',
})
export class TransactionDetailsComponent implements OnInit, OnDestroy {
 
  constructor(
    private apiService: ApiService,
    private route: ActivatedRoute,
    private router: Router,
    private notificationService: NotificationService,
    private webSocketService: WebSocketService
  ) {}

  transactionId:string | null = '';
  transaction: any = null;
  status:string = '';
  user:any=null;
  supplierId=-1;
  type:string='';
  isCustomer=false;
  isAdmin=false;
  isStockstaff=false;
  showQrScanner: boolean = false;
  canGenQR:boolean=false;
  canScanQR:boolean=false;
  canReturn:boolean=false;
  private wsSubscription!: Subscription;

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
                'Không thể lấy thông tin người dùng: ' + error
            );
          }
        });
    });

    // Subscribe to WebSocket messages
    this.wsSubscription = this.webSocketService.getMessages().subscribe({
      next: (message: any) => {
        this.handleWebSocketMessage(message);
      },
      error: (error: any) => {
        console.error('WebSocket error in TransactionDetailsComponent:', error);
      }
    });
  }

  getTransactionDetails():void{
    this.canGenQR=false;
  this.canScanQR=false;
  this.canReturn=false;
    if (this.transactionId) {
      this.apiService.getTransactionById(this.transactionId).subscribe({
        next:(transactionData: any) =>{
          if (transactionData.status === 200) {
            this.transaction = transactionData.transaction;
            this.status = this.transaction.status;
            this.type = this.transaction.transactionType;
            this.isAdmin = this.user.role=="ADMIN"?true:false;
            this.isCustomer = this.user.role=="CUSTOMER"?true:false;
            this.isStockstaff = this.user.role=="STOCKSTAFF"?true:false;
            if(this.transaction.transactionType!="RETURN_TO_SUPPLIER"&&this.transaction.supplier.id==this.user.supplier?.id&&this.transaction.status=='PENDING'){
              this.canGenQR=true;
            }
            if(this.transaction.transactionType=="RETURN_TO_SUPPLIER"&&(this.transaction.user.id==this.user.id||this.isAdmin)&&this.transaction.status=='PENDING'){
              this.canGenQR=true;
            }
            if(!this.isCustomer&&((this.transaction.supplier.id==this.user.supplier.id&&this.transaction.transactionType=="RETURN_TO_SUPPLIER")||(this.transaction.user.id==this.user.id&&this.transaction.transactionType!="RETURN_TO_SUPPLIER"))&&this.transaction.status=="PENDING"){
              this.canScanQR=true;
            }
            if(this.isCustomer&&this.transaction.transactionType=="SALE"&&this.transaction.user.id==this.user.id&&this.transaction.status=="PENDING"){
              this.canScanQR=true;
            }
            if((this.isAdmin||(this.isStockstaff&&this.user.id==this.transaction.user.id))&&this.transaction.transactionType=="PURCHASE"&&this.transaction.status=="COMPLETED"){
              this.canReturn=true;
            }
            if(this.isCustomer&&this.transaction.transactionType=="SALE"&&this.transaction.status=="COMPLETED"&&this.transaction.user.id==this.user.id){
              this.canReturn=true;
            }
          }
        },
        error:(error)=>{
          this.notificationService.showError('Error',
            error?.error?.message ||
              error?.message ||
              'Không thể lấy giao dịch: ' + error
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
          this.notificationService.showSuccess('Success', 'Cập nhật trạng thái giao dịch thành công');
          this.router.navigate(['/transaction']);
        },
        error:(error)=>{
          this.notificationService.showError('Error',
            error?.error?.message ||
              error?.message ||
              'Không thể cập nhật giao dịch: ' + error
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

  confirmReturn():void{
    this.status = 'PENDING';
    this.updateTransactionStatus('PENDING');
  }

  rejectReturn(): void{
    this.status='CANCELED';
    this.updateTransactionStatus('CANCELED');
  }

  confirmSender(): void {
    this.status = 'ADMIN_DECIDING';
    this.updateTransactionStatus('ADMIN_DECIDING');
  }

  rejectTransaction(): void {
    this.status = 'CANCELED';
    this.updateTransactionStatus('CANCELED');
  }

  confirmTransaction(): void {
    this.status = 'PENDING';
    this.updateTransactionStatus('PENDING');
  }

  // Sender actions for RETURN transactions
  senderAgree(): void {
    this.status = 'ADMIN_DECIDING';
    this.updateTransactionStatus('ADMIN_DECIDING');
  }

  senderDecline(): void {
    this.status = 'SENDER_DECLINED';
    this.updateTransactionStatus('SENDER_DECLINED');
  }
  
  navigateToReturnPage():void{
    this.router.navigate([`/transaction/return/${this.transaction.id}`]);
  }

  private updateTransactionStatus(newStatus: string): void {
    if (this.transactionId) {
      this.apiService.updateTransactionStatus(this.transactionId, newStatus).subscribe({
        next:(result)=>{
          this.notificationService.showSuccess('Success', `Đã cập nhật trạng thái: ${newStatus}`);
          this.getTransactionDetails(); // Refresh the transaction data
        },
        error:(error)=>{
          this.notificationService.showError('Error',
            error?.error?.message ||
              error?.message ||
              'Không thể cập nhật giao dịch: ' + error
          );
        }
      })
    }
  }

  confirmArrival(): void{
    // Mở QR Scanner để quét mã QR
    console.log('Mở trình quét QR cho giao dịch:', this.transaction?.id);
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
                'Không thể lấy thông tin nhà cung cấp: ' + error
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

  // Navigate to staff profile
  navigateToStaffProfile(userId: string): void {
    this.router.navigate(['/staff-profile', userId]);
  }

  // Check if user is staff (STOCKSTAFF)
  isStaffUser(user: any): boolean {
    return user && user.role === 'STOCKSTAFF';
  }

  // Handle WebSocket messages
  private handleWebSocketMessage(message: any): void {
    // Chỉ xử lý message cho transaction hiện tại
    if (message.data && message.data.id.toString() === this.transactionId) {
      switch (message.type) {
        case "TRANSACTION_UPDATED_RENDER":
          this.getTransactionDetails(); // Refresh transaction details
          this.notificationService.showInfo('Cập nhật giao dịch', `Giao dịch ID: ${message.data.id} đã được cập nhật trạng thái`);
          break;
          case 'TRANSACTION_DELETED_RENDER':
        this.router.navigate(['/transation']);
        this.notificationService.showWarning('Giao dịch đã xóa', `Giao dịch ID: ${message.data.id} đã bị xóa`);
        break;
      }
    }
  }

  ngOnDestroy(): void {
    if (this.wsSubscription) {
      this.wsSubscription.unsubscribe();
    }
  }
}
