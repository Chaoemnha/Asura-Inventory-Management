import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../service/api.service';
import { NotificationService } from '../service/notification.service';
import {sha256} from 'js-sha256';
@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css'
})
export class ProfileComponent implements OnInit {
  constructor(
    private apiService: ApiService,
    private notificationService: NotificationService
  ){}
  
  user: any = {
    name: 'Guest...',
    email: 'user@example.com',
    phoneNumber: '+84 123 456 789',
    role: 'User'
  }
  
  hashEmail = '';
  isEditMode = false;
  originalUser: any = {};
  
  // Form data for editing
  editForm: any = {
    name: '',
    email: '',
    phoneNumber: '',
    address: ''
  };

  ngOnInit(): void {
    this.fetchUserInfo();
  }
  
  isAdmin(): boolean {
    return this.apiService.isAdmin();
  }

  isSupplier(): boolean {
    return this.apiService.isSupplier();
  }

  isStaff(): boolean {
    return this.apiService.isStockStaff();
  }

  isCustomer(): boolean {
    return this.apiService.isCustomer();
  }

  fetchUserInfo(): void {
    this.apiService.getLoggedInUserInfo().subscribe({
      next: (res) => {
        this.user = res;
        this.originalUser = { ...res }; // Store original data
        this.updateEditForm(); // Update edit form with current data
        const encoder = new TextEncoder();
        const data = encoder.encode(res.email);
        const hash = sha256.create().update(data);
        hash.hex();
        this.hashEmail = hash.hex();
      },
      error: (error) => {
        this.notificationService.showError('Error',
          error?.error?.message ||
          error?.message ||
          'Không thể lấy thông tin hồ sơ: ' + error
        );
      }
    })
  }

  updateEditForm(): void {
    this.editForm = {
      name: this.user.name || '',
      email: this.user.email || '',
      phoneNumber: this.user.phoneNumber || '',
      address: this.user.address || ''
    };
  }

  toggleEditMode(): void {
    this.isEditMode = !this.isEditMode;
    if (this.isEditMode) {
      this.updateEditForm(); // Reset form with current data
    }
  }

  cancelEdit(): void {
    this.isEditMode = false;
    this.updateEditForm(); // Reset form to original data
  }

  saveChanges(): void {
    if (!this.editForm.name.trim() || !this.editForm.email.trim() || !this.editForm.phoneNumber.trim()) {
      this.notificationService.showError('Error', 'Vui lòng điền đủ các trường bắt buộc');
      return;
    }

    // Email validation
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(this.editForm.email)) {
      this.notificationService.showError('Error', 'Vui lòng nhập email hợp lệ');
      return;
    }

    const updateData = {
      name: this.editForm.name,
      email: this.editForm.email,
      phoneNumber: this.editForm.phoneNumber,
      address: this.editForm.address
    };

    this.apiService.updateUser(this.user.id, updateData).subscribe({
      next: (res: any) => {
        if (res.status === 200) {
          this.notificationService.showSuccess('Success', 'Cập nhật hồ sơ thành công');
          this.isEditMode = false;
          this.fetchUserInfo(); // Refresh user data
        }
      },
      error: (error) => {
        this.notificationService.showError('Error',
          error?.error?.message ||
          error?.message ||
          'Không thể cập nhật hồ sơ: ' + error
        );
      }
    });
  }
}
