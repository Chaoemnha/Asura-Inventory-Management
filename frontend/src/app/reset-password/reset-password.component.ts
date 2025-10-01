import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NotificationService } from '../service/notification.service';
import { ApiService } from '../service/api.service';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-reset-password',
  imports: [FormsModule, CommonModule, RouterLink],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.css',
})
export class ResetPasswordComponent implements OnInit {
  constructor(
    private apiService: ApiService,
    private router: Router,
    private notificationService: NotificationService,
    private route: ActivatedRoute
  ) {}
  formData: any = {
    token: '',
    password: '',
    confirmPassword: '',
  };
  ngOnInit() {
    this.route.queryParams.subscribe((params) => {
      this.formData.token = params['token'] || '';
      if (!this.formData.token) {
        this.notificationService.showError('Error', 'Yêu cầu token!');
      }
      else{
        this.apiService.validateResetToken(this.formData.token).subscribe({
          next: (res: any) => {
          if (res.status != 200) {
            this.router.navigate(["/forgot-password"]);
          }
        },
        error: (err: any) => {
          console.log(err);
          this.notificationService.showError('Error', 'Lỗi kết nối API');
          this.router.navigate(["/forgot-password"]);
        },
        })
      }
    });
    
  }
  handleSubmit() {
    if (!this.formData.password || !this.formData.confirmPassword) {
      this.notificationService.showError('Error', 'Vui lòng nhập đầy đủ thông tin!');
      return;
    }
    if (this.formData.password !== this.formData.confirmPassword) {
      this.notificationService.showError('Error', 'Mật khẩu không khớp!');
      return;
    }
    const response = this.apiService
      .resetPassword(this.formData.token, this.formData.password)
      .subscribe({
        next: (res: any) => {
          if (res.status == 200) {
            this.notificationService.showSuccess('Success', res.message);
            this.router.navigate(['/login']);
          } else {
            this.notificationService.showError('Error', res.eTag);
            this.router.navigate(['/forgot-password']);
          }
        },
        error: (err: any) => {},
      });
  }
}
