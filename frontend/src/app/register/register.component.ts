import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../service/api.service';
import { NotificationService } from '../service/notification.service';
import { firstValueFrom } from 'rxjs';
//Khi ta su dung cac Angular derectives NgIf, NgForOf thi can import commonModule
//Neu trong comp su dung route thi khai bao no
@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
}) 
export class RegisterComponent {
  constructor(
    private apiService: ApiService, 
    private router: Router,
    private notificationService: NotificationService
  ){}

  formData: any={
    email: '',
    name: '',
    phoneNumber: '',
    password: '',
    address: ''
  };

  async handleSubmit(){
    if(!this.formData.email || !this.formData.name || !this.formData.phoneNumber || !this.formData.password){
      this.notificationService.showError('Error', "Vui lòng điền đầy đủ thông tin bắt buộc");
      return;
    }

    try {
      const response: any = await firstValueFrom(this.apiService.registerUser(this.formData));
      if(response.status === 200){
        this.notificationService.showSuccess('Success', response.message);
        this.router.navigate(["/login"]);
      }
    } catch (error: any) {
      console.log(error);
      this.notificationService.showError('Error', error?.error?.message || error?.message || "Không thể đăng ký tài khoản: " + error);
    }
  }
}
