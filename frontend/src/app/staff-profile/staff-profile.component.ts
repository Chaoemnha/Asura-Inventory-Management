import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../service/api.service';
import { NotificationService } from '../service/notification.service';
import { ActivatedRoute, Router } from '@angular/router';
import { sha256 } from 'js-sha256';
import { MorrisChartDirective } from '../directives/morris-chart/morris-chart';

@Component({
  selector: 'app-staff-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, MorrisChartDirective],
  templateUrl: './staff-profile.component.html',
  styleUrl: './staff-profile.component.css',
})
export class StaffProfileComponent implements OnInit {
  constructor(
    private apiService: ApiService,
    private route: ActivatedRoute,
    private router: Router,
    private notificationService: NotificationService
  ) {}
  hashEmail = '';
  staffId: string | null = '';
  staff: any = null;
  activeTab: string = 'transactions';
  recentTransactions: any[] = [];

  // Date filter for activity report
  fromDate: string = '';
  toDate: string = '';

  // Activity report data
  activityReport: any = null;
  datas: any;
  options: any;

  ngOnInit(): void {
    this.route.params.subscribe((params) => {
      this.staffId = params['staffId'];
      this.initializeDateRange();
      this.getStaffProfile();
      this.getStaffActivityReport();
      this.getRecentTransactions();
    });
    this.initializeChartOptions();
  }

  initializeDateRange(): void {
    // Set default date range to last 30 days
    const today = new Date();
    const thirtyDaysAgo = new Date();
    thirtyDaysAgo.setDate(today.getDate() - 30);

    this.toDate = today.toISOString().split('T')[0];
    this.fromDate = thirtyDaysAgo.toISOString().split('T')[0];
  }

  initializeChartOptions(): void {
    this.options = {
      xkey: 'category',
      ykeys: ['value', 'val'],
      labels: ['CREATED', 'COMPLETED'],
      barRatio: 0.4,
      barColors: ['#4B5F71', '#26B99A', '#ACADAC', '#3498DB'],
      xLabelAngle: 0,
      hideHover: 'auto',
      resize: !0,
    };
  }

  updateChartData(): void {
    if (this.activityReport) {
      this.datas = [
        { category: 'Sales', value: this.activityReport.salesCreated, val: this.activityReport.salesCompleted },
        {
          category: 'Purchases',
          value: this.activityReport.purchasesCreated, val: this.activityReport.purchasesCompleted
        },
        {
          category: 'Products(Sale)',
          value: this.activityReport.totalProductsSaleCreated, val: this.activityReport.totalProductsSaleCompleted
        },
        {
          category: 'Products(Purchase)',
          value: this.activityReport.totalProductsPurchaseCreated, val: this.activityReport.totalProductsPurchaseCompleted,
        },
      ];
    }
  }

  getStaffProfile(): void {
    if (this.staffId) {
      this.apiService.getUserById(this.staffId).subscribe({
        next: (res: any) => {
          this.staff = res;
          const encoder = new TextEncoder();
          const data = encoder.encode(res.email);
          const hash = sha256.create().update(data);
          hash.hex();
          this.hashEmail = hash.hex();
        },
        error: (error: any) => {
          this.notificationService.showError(
            'Error',
            error?.error?.message ||
              error?.message ||
              'Không thể lấy thông tin nhân viên: ' + error
          );
        },
      });
    }
  }

  getStaffActivityReport(): void {
    if (this.staffId) {
      this.apiService
        .getStaffActivityReport(this.staffId, this.fromDate, this.toDate)
        .subscribe({
          next: (res: any) => {
            if (res.status === 200) {
              this.activityReport = res.activityReport;
              this.updateChartData();
            }
          },
          error: (error: any) => {
            this.notificationService.showError(
              'Error',
              error?.error?.message ||
                error?.message ||
                'Không thể lấy báo cáo hoạt động: ' + error
            );
          },
        });
    }
  }

  onDateRangeChange(): void {
    if (this.fromDate && this.toDate && this.staffId) {
      this.getStaffActivityReport();
    }
  }

  getRecentTransactions(): void {
    if (this.staffId) {
      this.apiService
        .getAllTransactions(
          Number.parseInt(this.staffId),
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          0,
          5
        )
        .subscribe({
          next: (res: any) => {
            this.recentTransactions = res.transactions;
          },
          error: (error: any) => {
            this.notificationService.showError(
              'Lỗi',
              error?.error?.message ||
                error?.message ||
                'Không thể lấy giao dịch gần đây: ' + error
            );
          },
        });
    }
  }

  setActiveTab(tab: string): void {
    this.activeTab = tab;
  }

  goBack(): void {
    window.history.back();
  }

  navigateToTransaction(transactionId: string): void {
    this.router.navigate(['/transaction/details', transactionId]);
  }
}
