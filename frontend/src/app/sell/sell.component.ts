import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../service/api.service';
import { NotificationService } from '../service/notification.service';

@Component({
  selector: 'app-sell',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './sell.component.html',
  styleUrl: './sell.component.css'
})
export class SellComponent implements OnInit {

  constructor(
    private apiService: ApiService,
    private notificationService: NotificationService
  ){}

  products: any[] = []
  productId:string = ''
  description:string = ''
  quantity:string = ''



  ngOnInit(): void {
    this.fetchProducts();
  }

  fetchProducts():void{
    this.apiService.getAllProducts("").subscribe({
      next: (res: any) => {
        if (res.status === 200) {
          this.products = res.products;
        }
      },
      error: (error) => {
        this.notificationService.showError('Error',
          error?.error?.message ||
            error?.message ||
            'Unable to get Products: ' + error
        );
      },
    });

  }

  //Handle form submission
  handleSubmit():void{
    if (!this.productId || !this.quantity) {
      this.notificationService.showError('Error', "Please fill all fields");
      return;
    }
    const body = {
      productId: this.productId,
      quantity:  parseInt(this.quantity, 10),
      description: this.description
    }

    this.apiService.sellProduct(body).subscribe({
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
            'Unable to sell a product: ' + error
        );
      },
    })

  }

  
  resetForm():void{
    this.productId = '';
    this.description = '';
    this.quantity = '';
  }
}

