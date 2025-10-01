import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { ApiService } from '../service/api.service';
import { NotificationService } from '../service/notification.service';

@Component({
  selector: 'app-add-edit-user',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './add-edit-user.component.html',
  styleUrl: './add-edit-user.component.css',
})
export class AddEditUserComponent implements OnInit {
  isEditing: boolean = false;
  userId: string | null = null;

  formData = {
    name: '',
    email: '',
    phoneNumber: '',
    address: '',
    role: '',
    password: ''
  };

  roles = [
    { value: 'ADMIN', label: 'Admin' },
    { value: 'STOCKSTAFF', label: 'Stock Staff' },
    { value: 'CUSTOMER', label: 'Customer' },
    { value: 'SUPPLIER', label: 'Supplier' }
  ];

  constructor(
    private apiService: ApiService,
    private router: Router,
    private route: ActivatedRoute,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.userId = this.route.snapshot.paramMap.get('userId');
    if (this.userId) {
      this.isEditing = true;
      this.fetchUser();
    }
  }

  // Fetch user data for editing
  fetchUser(): void {
    if (!this.userId) return;

    this.apiService.getUserById(this.userId).subscribe({
      next: (res: any) => {
        this.formData = {
          name: res.name || '',
          email: res.email || '',
          phoneNumber: res.phoneNumber || '',
          address: res.address || '',
          role: res.role || '',
          password: '' // Never pre-fill password
        };
      },
      error: (error) => {
        this.notificationService.showError('Error', 
          error?.error?.message ||
          error?.message ||
          'Unable to fetch user data'
        );
        this.router.navigate(['/users']);
      }
    });
  }

  // Validate form data
  validateForm(): boolean {
    if (!this.formData.name.trim()) {
      this.notificationService.showWarning('Warning', 'Tên là bắt buộc');
      return false;
    }

    if (!this.formData.email.trim()) {
      this.notificationService.showWarning('Warning', 'Email là bắt buộc');
      return false;
    }

    if (!this.formData.phoneNumber.trim()) {
      this.notificationService.showWarning('Warning', 'Số điện thoại là bắt buộc');
      return false;
    }

    if (!this.formData.role) {
      this.notificationService.showWarning('Warning', 'Quyền là bắt buộc');
      return false;
    }

    if (!this.isEditing && !this.formData.password.trim()) {
      this.notificationService.showWarning('Warning', 'Mật khẩu là cần thiết cho người dùng mới');
      return false;
    }

    // Email validation
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(this.formData.email)) {
      this.notificationService.showWarning('Warning', 'Hãy nhập một email hợp lệ');
      return false;
    }

    // Phone validation
    const phoneRegex = /^[\d\-\+\(\)\s]+$/;
    if (!phoneRegex.test(this.formData.phoneNumber)) {
      this.notificationService.showWarning('Warning', 'Hãy nhập một số điện thoại hợp lệ');
      return false;
    }

    // Password validation for new users
    if (!this.isEditing && this.formData.password.length < 5) {
      this.notificationService.showWarning('Warning', 'Mật khẩu phải có độ dài lớn hơn 5');
      return false;
    }

    return true;
  }

  // Handle form submission
  handleSubmit(): void {
    if (!this.validateForm()) {
      return;
    }

    const userData = {
      name: this.formData.name.trim(),
      email: this.formData.email.trim(),
      phoneNumber: this.formData.phoneNumber.trim(),
      address: this.formData.address.trim(),
      role: this.formData.role
    };

    // Add password for new users
    if (!this.isEditing) {
      (userData as any).password = this.formData.password;
    }

    if (this.isEditing) {
      // Update existing user
      this.apiService.updateUser(this.userId!, userData).subscribe({
        next: (res: any) => {
          if (res.status === 200) {
            this.notificationService.showSuccess('Success', 'Người dùng cập nhật thành công');
            this.router.navigate(['/users']);
          }
        },
        error: (error) => {
          this.notificationService.showError('Error',
            error?.error?.message ||
            error?.message ||
            'Không thể cập nhật người dùng'
          );
        }
      });
    } else {
      // Create new user (using register endpoint)
      this.apiService.registerUser(userData).subscribe({
        next: (res: any) => {
          if (res.status === 200 || res.statusCode === 200) {
            this.notificationService.showSuccess('Success', 'Người dùng thêm mới thành công');
            this.router.navigate(['/users']);
          }
        },
        error: (error) => {
          this.notificationService.showError('Error',
            error?.error?.message ||
            error?.message ||
            'Không thể cập nhật người dùng'
          );
        }
      });
    }
  }

  // Reset form
  resetForm(): void {
    if (this.isEditing) {
      this.fetchUser(); // Reload original data
    } else {
      this.formData = {
        name: '',
        email: '',
        phoneNumber: '',
        address: '',
        role: '',
        password: ''
      };
    }
  }

  // Cancel and go back
  cancel(): void {
    this.router.navigate(['/users']);
  }

  // Check if current user is admin
  isAdmin(): boolean {
    return this.apiService.isAdmin();
  }
}