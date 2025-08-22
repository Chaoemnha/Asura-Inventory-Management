import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../service/api.service';
import { NotificationService } from '../service/notification.service';
import { firstValueFrom } from 'rxjs';
import { AppComponent } from '../app.component';
 
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
constructor(
  private apiService: ApiService, 
  private router: Router,
  private notificationService: NotificationService
){};
formData:any = {
  email: '',
  password: '',
};
async handleSubmit(){
 if(!this.formData.email||!this.formData.password){
  this.notificationService.showError('Error', "All fields are required!");
  return;
 }
 try {
 const response = await firstValueFrom(this.apiService.loginUser(this.formData));
 if(response.status==200 ){
  this.notificationService.showSuccess('Success', response.message);
  this.apiService.encryptAndSaveToStorage('token', response.token);
  this.apiService.encryptAndSaveToStorage('role', response.role);
  this.apiService.logged();
  this.router.navigate(["/dashboard"]);
 }
 } catch (error:any) {
  console.log(error);
  const errorMessage = error?.error?.message||error?.message||"Unable to login";
  this.notificationService.showError('Error', errorMessage);
 }
}
}
