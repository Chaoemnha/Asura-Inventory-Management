import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../service/api.service';
import { ActivatedRoute, Router } from '@angular/router';
import { NotificationService } from '../service/notification.service';

@Component({
  selector: 'app-add-edit-product',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './add-edit-product.component.html',
  styleUrl: './add-edit-product.component.css',
})
export class AddEditProductComponent implements OnInit {
  constructor(
    private apiService: ApiService,
    private route: ActivatedRoute,
    private router: Router,
    private notificationService: NotificationService
  ){}

  productId: string | null = null
  name:string = ''
  sku:string = ''
  price:string = ''
  stockQuantity:string = ''
  categoryId:string = ''
  description:string = ''
  imageFile:File | null = null
  imageUrl:string = ''
  isEditing:boolean = false
  isViewing:boolean = false
  categories:any[] = []



  ngOnInit(): void {
    this.productId = this.route.snapshot.paramMap.get('productId');
    const mode = this.route.snapshot.queryParams['mode'];
    const currentRoute = this.router.url;
    
    this.fetchCategories();
    if (this.productId) {
      // Nếu route là /product/:productId thì mặc định là view mode
      if (currentRoute.includes('/product/') && !currentRoute.includes('/edit-product/')) {
        this.isViewing = true;
      } else if (mode === 'view') {
        this.isViewing = true;
      } else {
        this.isEditing = true;
      }
      this.fetchProductById(this.productId)
    }
  }


  //GET ALL CATEGORIES
  fetchCategories():void{
    this.apiService.getAllCategory().subscribe({
      next:(res:any) =>{
        if (res.status === 200) {
          this.categories = res.categories
        }
      },
      error:(error) =>{
        this.notificationService.showError('Error', error?.error?.message || error?.message || "Unable to get all categories: " + error);
      }})
  }

  isAdmin(): boolean {
    return this.apiService.isAdmin();
  }

  isAuthenticated(): boolean{
    return this.apiService.isAuthenticated();
  }

  isStockstaff(): boolean {
    return this.apiService.isStockStaff();
  }

  isCustomer(): boolean {
    return this.apiService.isCustomer();
  }
  //GET CATEGORY BY ID

  fetchProductById(productId: string):void{
    this.apiService.getProductById(productId).subscribe({
      next:(res:any) =>{
        if (res.status === 200) {
          const product = res.product;
          this.name = product.name;
          this.sku = product.sku;
          this.price = product.price;
          this.stockQuantity = product.stockQuantity;
          this.categoryId = product.categoryId;
          this.description = product.description;
          this.imageUrl = product.imageUrl;
        }else{
          this.notificationService.showError('Error', res.message);
        }
      },
      error:(error) =>{
        this.notificationService.showError('Error', error?.error?.message || error?.message || "Unable to get product by ID: " + error);
      }})
  }

  handleImageChange(event: Event):void{
    const input = event.target as HTMLInputElement;
    if (input?.files?.[0]) {
      this.imageFile = input.files[0]
      const reader = new FileReader();
      reader.onloadend = () =>{
        this.imageUrl = reader.result as string
      }
      reader.readAsDataURL(this.imageFile);
    }
  }

  handleSubmit(event : Event):void{
    event.preventDefault()
    const formData = new FormData();
    formData.append("name", this.name);
    formData.append("sku", this.sku);
    formData.append("price", this.price);
    formData.append("stockQuantity", this.stockQuantity);
    formData.append("categoryId", this.categoryId);
    formData.append("description", this.description);

    if (this.imageFile) {
      formData.append("imageFile", this.imageFile);
    }

    if (this.isEditing) {
      formData.append("productId", this.productId!);
      this.apiService.updateProduct(formData).subscribe({
        next:(res:any) =>{
          if (res.status === 200) {
            this.notificationService.showSuccess('Success', "Sản phẩm đã cập nhật thành công");
            this.router.navigate(['/product'])
          }
        },
        error:(error) =>{
          this.notificationService.showError('Error', error?.error?.message || error?.message || "Không thể cập nhật product: " + error);
        }})
    }else{
      this.apiService.addProduct(formData).subscribe({
        next:(res:any) =>{
          if (res.status === 200) {
            this.notificationService.showSuccess('Success', "Sản phẩm đã thêm mới thành công");
            this.router.navigate(['/product'])
          }
        },
        error:(error) =>{
          this.notificationService.showError('Error', error?.error?.message || error?.message || "Unable to save product: " + error);
        }})
    }
  }

  cancel(){
    this.router.navigate(['/product']);
  }

  //NAVIGATE TO SELL PAGE
  navigateToSell(productId: string): void {
    this.router.navigate(['/sell'], { queryParams: {productId: productId } });
  }

  //NAVIGATE TO PURCHASE PAGE
  navigateToPurchase(productId: string): void {
    this.router.navigate(['/purchase'], { queryParams: { productId: productId } });
  }

  //NAVIGATE TO LOGIN PAGE
  navigateToLogin(): void {
    this.router.navigate(['/login']);
  }

}
