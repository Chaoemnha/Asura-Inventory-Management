import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../service/api.service';
import { NotificationService } from '../service/notification.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-forgot-password',
  imports: [FormsModule, CommonModule, RouterLink],
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.css',
})
export class ForgotPasswordComponent {
  constructor(
    private apiService: ApiService,
    private router: Router,
    private notificationService: NotificationService
  ) {}
  formData: any = {
    email: '',
  };

  handleSubmit() {
    console.log("first");
    if (!this.formData.email) {
      this.notificationService.showError('Error', 'Email is required!');
      return;
    }
    const response = this.apiService
      .forgotPassword(this.formData.email)
      .subscribe({
        next: (res: any) => {
          if (res.status == 200) {
            this.notificationService.showSuccess('Success', res.message);
            // Navigate to login after successful email send
            this.router.navigate(["/login"]);
          } else {
            this.notificationService.showError('Error', res.eTag);
          }
        },
        error: (err: any) => {
          console.log(err);
          const errorMessage = err?.error?.message || err?.message || 'Failed to send reset email';
          this.notificationService.showError('Error', errorMessage);
        }
      });
      
  }
}
