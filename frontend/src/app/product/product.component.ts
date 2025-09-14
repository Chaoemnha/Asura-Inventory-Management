import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CustomPaginationComponent } from '../custom-pagination/custom-pagination.component';
import { ApiService } from '../service/api.service';
import { Router, ActivatedRoute } from '@angular/router';
import { NotificationService } from '../service/notification.service';

@Component({
  selector: 'app-product',
  standalone: true,
  imports: [CommonModule, FormsModule, CustomPaginationComponent],
  templateUrl: './product.component.html',
  styleUrl: './product.component.css',
})
export class ProductComponent implements OnInit {
  constructor(
    private apiService: ApiService, 
    private router: Router,
    private route: ActivatedRoute,
    private notificationService: NotificationService
  ) {}
  products: any[] = [];
  searchInput:string = '';
  valueToSearch:string = '';
  currentPage: number = 1;
  totalPages: number = 0;
  itemsPerPage: number = 10;
  selectedCategory: string = '';
  allProducts: any[] = []; // Store all products for filtering

  ngOnInit(): void {
    // Listen to query params changes
    this.route.queryParams.subscribe(params => {
      this.selectedCategory = params['category'] || '';
      this.currentPage = 1; // Reset to first page when category changes
      this.fetchProducts();
    });
  }

  //FETCH PRODUCTS
  fetchProducts(): void {
    if(this.selectedCategory!=''){
      this.apiService.getAllProductsByCategoryName(this.selectedCategory).subscribe({
      next: (res: any) => {
        this.allProducts = res.products || [];
        if (this.valueToSearch) {
          this.allProducts = this.allProducts.filter(product =>
            product.name.toLowerCase().includes(this.valueToSearch.toLowerCase()) ||
            product.sku.toLowerCase().includes(this.valueToSearch.toLowerCase())
          );
        }

        this.totalPages = Math.ceil(this.allProducts.length / this.itemsPerPage);

        this.products = this.allProducts.slice(
          (this.currentPage - 1) * this.itemsPerPage,
          this.currentPage * this.itemsPerPage
        );
        
      },
      error: (error:any) => {
        this.notificationService.showError('Error',
          error?.error?.message ||
            error?.message ||
            'Unable to fetch products: ' + error
        );
      },
    });
    }
    else
    this.apiService.getAllProducts().subscribe({
      next: (res: any) => {
        this.allProducts = res.products || [];
        if (this.valueToSearch) {
          this.allProducts = this.allProducts.filter(product =>
            product.name.toLowerCase().includes(this.valueToSearch.toLowerCase()) ||
            product.sku.toLowerCase().includes(this.valueToSearch.toLowerCase())
          );
        }

        this.totalPages = Math.ceil(this.allProducts.length / this.itemsPerPage);

        this.products = this.allProducts.slice(
          (this.currentPage - 1) * this.itemsPerPage,
          this.currentPage * this.itemsPerPage
        );
        
      },
      error: (error) => {
        this.notificationService.showError('Error',
          error?.error?.message ||
            error?.message ||
            'Unable to fetch products: ' + error
        );
      },
    });
  }

  //HANDLE SEARCH
  handleSearch():void{
    this.currentPage = 1; // Reset to first page when searching
    this.valueToSearch = this.searchInput;
    this.fetchProducts();
  }

  //DELETE A PRODUCT
  handleProductDelete(productId: string): void {
    if (window.confirm('Are you sure you want to delete this product?')) {
      this.apiService.deleteProduct(productId).subscribe({
        next: (res: any) => {
          if (res.status === 200) {
            this.notificationService.showSuccess('Success', 'Product deleted successfully');
            this.fetchProducts(); //reload the products
          }
        },
        error: (error) => {
          this.notificationService.showError('Error',
            error?.error?.message ||
              error?.message ||
              'Unable to Delete product: ' + error
          );
        },
      });
    }
  }

  //HANDLE PAGE CHANGE. NAVIGATE TO NEXT< PREVIOUS OR SPECIFIC PAGE CHANGE
  onPageChange(page: number): void {
    this.currentPage = page;
    this.fetchProducts();
  }

  //NAVIGATE TO ADD PRODUCT PAGE
  navigateToAddProductPage(): void {
    this.router.navigate(['/add-product']);
  }

  //NAVIGATE TO EDIT PRODUCT PAGE
  navigateToEditProductPage(productId: string): void {
    this.router.navigate([`/edit-product/${productId}`]);
  }

}
