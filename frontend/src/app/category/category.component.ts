import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../service/api.service';
import { NotificationService } from '../service/notification.service';
import { Subscription } from 'rxjs';
import { WebSocketService } from '../service/websocket.service';

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
export class CategoryComponent implements OnInit, OnDestroy{
  categories: Category[] = [];
  categoryName: string = '';
  isEditing: boolean = false;
  editingCategoryId: string | null = null;
  private wsSubscription!: Subscription;

  constructor(
    private apiService: ApiService,
    private notificationService: NotificationService,
    private webSocketService: WebSocketService
  ){};

  ngOnInit(){
    this.getCategories();

    //ket noi ws
    this.wsSubscription = this.webSocketService.getMessages().subscribe({
      next: (message:any)=>{
        this.handleWebSocketMessage(message);
      },
      error: (error:any)=>{
        console.error('Ws ms err: ',error);
      }
    })
  }

  ngOnDestroy(){
    if(this.wsSubscription){
      this.wsSubscription.unsubscribe();
    }
    this.webSocketService.disconnect();
  }

  //Xu ly tin nhan tu ws
  private handleWebSocketMessage(message: any): void{
    switch (message.type) {
      case "CATEGORY_UPDATED":
        this.updateCategoryInList(message.data);
        break;
        
      case 'CATEGORY_ADDED':
        this.addCategoryToList(message.data);
        break;
        
      case 'CATEGORY_DELETED':
        this.removeCategoryFromList(message.categoryId);
        // Reset form nếu đang edit category bị xóa
        if (this.editingCategoryId === message.categoryId) {
          this.cancel();
        }
        break;
    }
  }

  private updateCategoryInList(updatedCategory: Category): void{
    const index = this.categories.findIndex(cat=>cat.id.toString()===updatedCategory.id)
    if(index!=-1){
      this.categories[index] = updatedCategory;
      if(this.editingCategoryId===updatedCategory.id){
        this.categoryName = updatedCategory.name;
      }
    }
  }
  
  private addCategoryToList(newCategory: Category): void {
    const exists = this.categories.some(cat => cat.id.toString() === newCategory.id);
    if (!exists) {
      this.categories.push(newCategory);
    }
  }

  private removeCategoryFromList(categoryId: string): void {
    this.categories = this.categories.filter(cat => cat.id !== categoryId);
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
          }
        },
        error: (error:any)=>{
          this.notificationService.showError('Error', error?.error?.message||error?.error||"Unable to delete category: "+error);
        }
      })
    }

  }
}
