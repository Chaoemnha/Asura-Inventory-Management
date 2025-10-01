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
  searchText: string = '';
  sortBy: string = 'stockQuantity';
  sortDirection: string = 'ASC';
  currentPage: number = 1;
  totalPages: number = 0;
  itemsPerPage: number = 9;
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

  isAdmin(): boolean {
    return this.apiService.isAdmin();
  }

  isAuthenticated(): boolean{
    return this.apiService.isAuthenticated();
  }

  //FETCH PRODUCTS
  fetchProducts(): void {
    if (this.selectedCategory!) {
      this.apiService.getAllProductsByCategoryName(this.selectedCategory, this.searchText, this.sortBy, this.sortDirection).subscribe({
        next: (res: any) => {
          this.allProducts = res.products || [];
          this.totalPages = Math.ceil(this.allProducts.length / this.itemsPerPage);
          this.products = this.allProducts.slice(
            (this.currentPage - 1) * this.itemsPerPage,
            this.currentPage * this.itemsPerPage
          );
        },
        error: (error: any) => {
          this.notificationService.showError('Error',
            error?.error?.message ||
            error?.message ||
            'Không thể tải sản phẩm: ' + error
          );
        },
      });
    } else {
      this.apiService.getAllProducts(this.searchText, this.sortBy, this.sortDirection).subscribe({
        next: (res: any) => {
          this.allProducts = res.products || [];
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
            'Không thể tải sản phẩm: ' + error
          );
        },
      });
    }
  }

  //APPLY FILTERS AND SEARCH
  applyFilters(): void {
    this.currentPage = 1; // Reset to first page
    this.fetchProducts();
  }

  //CLEAR SEARCH AND RESET FILTERS
  clearSearch(): void {
    this.searchText = '';
    this.sortBy = 'stockQuantity';
    this.sortDirection = 'ASC';
    this.currentPage = 1;
    this.fetchProducts();
  }

  //SET SORTING OPTIONS
  setSorting(field: string, direction: string): void {
    this.sortBy = field;
    this.sortDirection = direction;
    this.currentPage = 1;
    this.fetchProducts();
  }

  //DELETE A PRODUCT
  handleProductDelete(productId: string): void {
    if (window.confirm('Bạn có chắc chắn muốn xóa sản phẩm này không?')) {
      this.apiService.deleteProduct(productId).subscribe({
        next: (res: any) => {
          if (res.status === 200) {
            this.notificationService.showSuccess('Success', 'Xóa sản phẩm thành công');
            this.fetchProducts(); //reload the products
          }
        },
        error: (error) => {
          this.notificationService.showError('Error',
            error?.error?.message ||
            error?.message ||
            'Không thể xóa sản phẩm: ' + error
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

  //NAVIGATE TO VIEW PRODUCT PAGE
  navigateToViewProductPage(productId: string): void {
    this.router.navigate([`/product/${productId}`]);
  }

  //NAVIGATE TO SELL PAGE
  navigateToSell(productId: string): void {
    this.router.navigate(['/sell'], { queryParams: {productId: productId } });
  }

  //NAVIGATE TO PURCHASE PAGE
  navigateToPurchase(productId: string): void {
    this.router.navigate(['/purchase'], { queryParams: { productId: productId } });
  }

  //CHECK IF USER IS STOCK STAFF
  isStockStaff(): boolean {
    return this.apiService.isStockStaff();
  }

  //CHECK IF USER IS CUSTOMER
  isCustomer(): boolean {
    return this.apiService.isCustomer();
  }

  getRowClass(input: number){
    if(input<=2&&input>0&&(this.isAdmin()||this.isStockStaff())) return "bg-warning";
    if(input<=0&&(this.isAdmin()||this.isStockStaff())) return "bg-danger";
    return "";
  }
}
