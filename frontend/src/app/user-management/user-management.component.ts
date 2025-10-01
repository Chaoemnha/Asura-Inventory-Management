import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService } from '../service/api.service';
import { NotificationService } from '../service/notification.service';

interface User {
  id: string;
  name: string;
  email: string;
  phoneNumber: string;
  address: string;
  role: string;
  createdAt: string;
}

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './user-management.component.html',
  styleUrl: './user-management.component.css',
})
export class UserManagementComponent implements OnInit {
  users: User[] = [];
  filteredUsers: User[] = [];
  searchText: string = '';
  selectedRole: string = '';
  
  // Available roles for filtering
  roles = [
    { value: '', label: 'Tất cả quyền' },
    { value: 'ADMIN', label: 'Quản trị' },
    { value: 'STOCKSTAFF', label: 'Nhân viên kho' },
    { value: 'CUSTOMER', label: 'Khách hàng' },
    { value: 'SUPPLIER', label: 'Nhà cung cấp' }
  ];

  constructor(
    private apiService: ApiService,
    private router: Router,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.fetchUsers();
  }

  // Check if current user is admin
  isAdmin(): boolean {
    return this.apiService.isAdmin();
  }

  // Fetch all users from API
  fetchUsers(): void {
    this.apiService.getAllUsers().subscribe({
      next: (res: any) => {
        if (res.status === 200) {
          this.users = res.users || [];
          this.applyFilters();
        } else {
          this.notificationService.showError('Error', res.message || 'Lỗi lấy danh sách người dùng');
        }
      },
      error: (error) => {
        this.notificationService.showError('Error', 
          error?.error?.message ||
          error?.message ||
          'Không thể lấy danh sách người dùng'
        );
      },
    });
  }

  // Apply search and role filters
  applyFilters(): void {
    this.filteredUsers = this.users.filter(user => {
      const matchesSearch = !this.searchText || 
        user.name.toLowerCase().includes(this.searchText.toLowerCase()) ||
        user.email.toLowerCase().includes(this.searchText.toLowerCase()) ||
        user.phoneNumber.includes(this.searchText) ||
        user.role.toLowerCase().includes(this.searchText.toLowerCase());
      
      const matchesRole = !this.selectedRole || user.role === this.selectedRole;
      
      return matchesSearch && matchesRole;
    });
  }

  // Handle search input change
  onSearchChange(): void {
    this.applyFilters();
  }

  // Handle role filter change
  onRoleFilterChange(): void {
    this.applyFilters();
  }

  // Clear all filters
  clearFilters(): void {
    this.searchText = '';
    this.selectedRole = '';
    this.applyFilters();
  }

  // Navigate to add user page
  navigateToAddUser(): void {
    this.router.navigate(['/add-user']);
  }

  // Navigate to edit user page
  navigateToEditUser(userId: string): void {
    this.router.navigate([`/edit-user/${userId}`]);
  }

  // Delete user with confirmation
  handleDeleteUser(userId: string, userName: string): void {
    if (window.confirm(`Bạn có chắc muốn xóa người dùng "${userName}"? Thao tác này không thể hoàn tác.`)) {
      this.apiService.deleteUser(userId).subscribe({
        next: (res: any) => {
          if (res.status === 200) {
            this.notificationService.showSuccess('Success', 'Xóa người dùng thành công');
            this.fetchUsers(); // Reload users list
          }
        },
        error: (error) => {
          this.notificationService.showError('Error', 
            error?.error?.message || 
            error?.message || 
            'Không thể xóa người dùng'
          );
        }
      });
    }
  }

  // Format date for display
  formatDate(dateString: string): string {
    if (!dateString) return 'Không có';
    const date = new Date(dateString);
    return date.toLocaleDateString('vi-VN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }

  // Get role badge class for styling
  getRoleBadgeClass(role: string): string {
    const roleClasses: { [key: string]: string } = {
      'ADMIN': 'badge badge-danger',
      'STOCKSTAFF': 'badge badge-info',
      'CUSTOMER': 'badge badge-success',
      'SUPPLIER': 'badge badge-secondary'
    };
    return roleClasses[role] || 'badge badge-light';
  }
}