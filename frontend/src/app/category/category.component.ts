import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../service/api.service';
import { NotificationService } from '../service/notification.service';

interface Category{
  id: string,
  name: string
}

@Component({
  selector: 'app-category',
  imports: [CommonModule, FormsModule],
  templateUrl: './category.component.html',
  styleUrl: './category.component.css'
})
export class CategoryComponent {
  categories: Category[] = [];
  categoryName: string = '';
  isEditing: boolean = false;
  editingCategoryId: string | null = null;

  constructor(
    private apiService: ApiService,
    private notificationService: NotificationService
  ){};

  ngOnInit(){
    this.getCategories();
  }

  getCategories():void{
    this.apiService.getAllCategory().subscribe({
      next:(res: any)=>{
        if(res.status==200) {
          this.categories = res.categories;
        }
      },
      error: (error:any)=>{
        this.notificationService.showError('Error', error?.error?.message||error?.message||"Unable to show categories");
      }
    })
  }

  addCategory(){
    if(!this.categoryName){
      this.notificationService.showError('Error', "Category name is required");
      return;
    }
    this.apiService.createCategory({name: this.categoryName}).subscribe({
      next: (res: any)=>{
        if(res.status==200){
          this.notificationService.showSuccess('Success', "Category add successfully");
          this.categoryName='';
          this.getCategories();
        }
      },
      error:(error:any)=>{
        this.notificationService.showError('Error', error?.error?.message||error?.error||"Unable to add category: "+error);
      }
    })
  }

  cancel() {
    this.isEditing = false;
    this.categoryName = '';
    this.editingCategoryId = null;
  }

  editCategory(){
    if(!this.editingCategoryId||!this.categoryName){
      return;
    }
    this.apiService.updateCategory(this.editingCategoryId, {name: this.categoryName}).subscribe({
      next: (res: any)=>{
        if(res.status==200){
          this.notificationService.showSuccess('Success', "Category updated successfully");
          this.categoryName='';
          this.isEditing=false;
          this.getCategories();
        }
      },
      error: (error: any)=>{
        this.notificationService.showError('Error', error?.error?.message||error?.error||"Unable to update category: "+error);
      }
    })
  }

  //dat category de thuc hien edit
  handleEditCategory(category: Category){
    this.isEditing=true;
    this.editingCategoryId=category.id;
    this.categoryName = category.name;
  }

  handleDeleteCategory(categoryId: string){
    if(window.confirm("Are you sure you want to delete this category?")){
      this.apiService.deleteCategory(categoryId).subscribe({
        next: (res:any)=>{
          if(res.status==200){
           this.notificationService.showSuccess('Success', "Category deleted successfully!");
           this.getCategories(); 
          }
        },
        error: (error:any)=>{
          this.notificationService.showError('Error', error?.error?.message||error?.error||"Unable to delete category: "+error);
        }
      })
    }

  }
}
