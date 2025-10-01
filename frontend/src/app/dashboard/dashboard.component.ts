// Import necessary Angular modules and services
import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { NgxChartsModule } from '@swimlane/ngx-charts';  // Module for charts
import { ApiService } from '../service/api.service'; // Service to interact with API
import { FormsModule } from '@angular/forms'; // Forms module for two-way binding

// Define the component metadata
@Component({
  selector: 'app-dashboard', // The component selector
  standalone: true, // Marks this component as standalone (no need for NgModule)
  imports: [CommonModule, NgxChartsModule, FormsModule], // Import other modules required for this component
  templateUrl: './dashboard.component.html', // HTML template
  styleUrl: './dashboard.component.css', // CSS styles for the component
})

export class DashboardComponent {
  // Define the properties for storing transaction data and chart data
  transactions: any[] = []; // Array to hold all transactions
  transactionTypeData: any[] = []; // Data for the chart showing count of transactions by type
  transactionAmountData: any[] = []; // Data for the chart showing total amount by transaction type
  monthlyTransactionData: any[] = []; // Data for the chart showing daily totals for the selected month
  
  // New properties for the missing dashboard features
  topInventoryProducts: any[] = []; // Data for top inventory products chart
  bestSellingProducts: any[] = []; // Data for best selling products chart

  // List of months, used for selecting a month
  months = [
    { name: 'January', value: '02' },
    { name: 'February', value: '03' },
    { name: 'March', value: '04' },
    { name: 'April', value: '05' },
    { name: 'May', value: '06' },
    { name: 'June', value: '07' },
    { name: 'July', value: '08' },
    { name: 'August', value: '09' },
    { name: 'September', value: '10' },
    { name: 'October', value: '11' },
    { name: 'November', value: '12' },
    { name: 'December', value: '13' },
  ];

  // Array to store the years (last 10 years from current year)
  years = Array.from({ length: 10 }, (_, i) => new Date().getFullYear() - i); 

  // Selected month and year for filtering monthly data
  selectedMonth = '';
  selectedYear = '';
  selectedLimit = '10'; // Default limit for charts

  // Chart view dimensions, legend, and animations settings
  view: [number, number] = [700, 400];  // Chart size: width x height
  showLegend = true;  // Display chart legend
  showLabels = true;  // Display labels on chart
  animations = true;  // Enable chart animations

  // Constructor to inject ApiService for API calls
  constructor(private apiService: ApiService) {}

  // ngOnInit lifecycle hook, called when the component initializes
  ngOnInit(): void {
    this.loadTransactions(); // Load transactions when the component initializes
    const limit = parseInt(this.selectedLimit);
    this.loadTopInventoryProducts(limit); // Load top inventory products
    this.loadBestSellingProducts(limit); // Load best selling products
  }

  // Method to fetch all transactions from the API
  loadTransactions(): void {
    this.apiService.getAllTransactions(-1).subscribe((data) => {
      this.transactions = data.transactions; // Store transactions data
      this.processChartData(); // Process data to generate charts
    });
  }
  formatDateToFullString(date: Date): string {
  const pad = (num: number, size: number = 2) => num.toString().padStart(size, '0');

  const year = date.getFullYear();
  const month = pad(date.getMonth()-1); // tháng 0-based
  const day = pad(date.getDate());

  return `${year}-${month}-${day}T00:00:00`;
  }
  // Method to process transaction data for type-based and amount-based charts
  processChartData(): void {
    // Object to count the number of transactions by type
    const typeCounts: { [key: string]: number } = {};

    // Object to sum the actual transaction amounts by type (as absolute values for visualization)
    const amountByType: { [key: string]: number } = {};

    // Loop through each transaction to calculate totals by type
    this.transactions.forEach((transaction) => {
      const type = transaction.transactionType; // Get the transaction type
      typeCounts[type] = (typeCounts[type] || 0) + 1; // Count transactions by type
      
      // For amount visualization, we show absolute values to see transaction volumes
      // (Revenue impact calculation is handled separately in monthly data)
      amountByType[type] = (amountByType[type] || 0) + Math.abs(transaction.totalPrice);
    });

    // Prepare data for chart displaying number of transactions by type
    this.transactionTypeData = Object.keys(typeCounts).map((type) => ({
      name: type,
      value: typeCounts[type],
    }));

    // Prepare data for chart displaying total transaction amount by type
    this.transactionAmountData = Object.keys(amountByType).map((type) => ({
      name: type,
      value: amountByType[type],
    }));
  }

  // Method to load transaction data for a specific month and year
  loadMonthlyData(): void {
    // If no month or year is selected, load all transactions and refresh charts
    if (!this.selectedMonth || !this.selectedYear) {
      this.loadTransactions();
      this.refreshDashboard();
      return;
    }

    // Call API to get transactions for the selected month and year
    this.apiService
      .getTransactionsByMonthAndYear(
        Number.parseInt(this.selectedMonth)-1, // Convert month string to number
        Number.parseInt(this.selectedYear) // Convert year string to number
      )
      .subscribe((data) => {
        this.transactions = data.transactions; // Store transactions for the selected month
        this.processChartData(); // Process the overall data for charts
        this.processMonthlyData(data.transactions); // Process the data for the daily chart
        
        // Refresh charts with selected limit and date range
        const limit = parseInt(this.selectedLimit);
        const fromDate = this.formatDateToFullString(new Date(Number.parseInt(this.selectedYear), Number.parseInt(this.selectedMonth),1));
        const toDate = this.formatDateToFullString(new Date(Number.parseInt(this.selectedYear), Number.parseInt(this.selectedMonth)+1, 0));
        
        this.loadTopInventoryProducts(limit);
        this.loadBestSellingProducts(limit, fromDate, toDate);
      });
  }

  // Method to process daily transaction data for the selected month
  processMonthlyData(transactions: any[]): void {
    // Object to store daily revenue (key = day, value = net revenue)
    const dailyRevenue: { [key: string]: number } = {};

    // Loop through each transaction and calculate net revenue for each day
    transactions.forEach((transaction) => {
      const date = new Date(transaction.createdAt).getDate().toString(); // Get the day from transaction date
      const amount = transaction.totalPrice;
      
      // Calculate net revenue based on transaction type
      let revenueImpact = 0;
      
      // Use structured approach instead of nested switches for better readability
      if (transaction.transactionStatus === 'CANCELED') {
        revenueImpact = 0; // Canceled transactions don't affect revenue
      } else {
        switch (transaction.transactionType) {
          case 'SALE':
            revenueImpact = +amount; // Sales increase revenue
            break;
            
          case 'PURCHASE':
            revenueImpact = -amount; // Purchases decrease revenue
            break;
            
          case 'RETURN_TO_SUPPLIER':
            if (transaction.supplier && transaction.supplier.id === 7) {
              // Return to stock (internal) - increases available inventory value
              revenueImpact = +amount;
            } else {
              // Return to external supplier - decreases revenue
              revenueImpact = -amount;
            }
            break;
            
          default:
            revenueImpact = 0; // Unknown transaction type
            break;
        }
      }
      
      dailyRevenue[date] = (dailyRevenue[date] || 0) + revenueImpact;
    });

    // Prepare data for chart displaying daily net revenue for the selected month
    this.monthlyTransactionData = Object.keys(dailyRevenue).map((day) => ({
      name: `Day ${day}`,
      value: dailyRevenue[day],
    }));
  }
  
  isAdmin(): boolean {
    return this.apiService.isAdmin();
  }
  
  isStockstaff(): boolean {
    return this.apiService.isStockStaff();
  }

  // Method to load top inventory products
  loadTopInventoryProducts(limit: number = 10): void {
    if (this.isAdmin() || this.isStockstaff()) {
      this.apiService.getTopInventoryProducts(limit).subscribe({
        next: (data) => {
          if (data.status === 200 && data.products) {
            // Transform data for ngx-charts format
            this.topInventoryProducts = data.products.map((product: any) => ({
              name: product.name,
              value: product.stockQuantity
            }));
          }
        },
        error: (error) => {
          console.error('Error loading top inventory products:', error);
        }
      });
    }
  }

  // Method to load best selling products
  loadBestSellingProducts(limit: number = 10, fromDate?: string, toDate?: string): void {
    if (this.isAdmin() || this.isStockstaff()) {
      this.apiService.getBestSellingProducts(limit, fromDate, toDate).subscribe({
        next: (data) => {
          if (data.status === 200 && data.bestSellingProducts) {
            // Transform data for ngx-charts format
            this.bestSellingProducts = data.bestSellingProducts.map((product: any) => ({
              name: product.name,
              value: product.totalSold
            }));
          }
        },
        error: (error) => {
          console.error('Error loading best selling products:', error);
        }
      });
    }
  }

  // Method to clear all filters and reload initial data
  clearFilters(): void {
    this.selectedMonth = '';
    this.selectedYear = '';
    this.selectedLimit = '10';
    this.monthlyTransactionData = [];
    this.loadTransactions();
    this.refreshDashboard();
  }

  // Method to refresh all dashboard data
  refreshDashboard(): void {
    const limit = parseInt(this.selectedLimit);
    this.loadTransactions();
    this.loadTopInventoryProducts(limit);
    this.loadBestSellingProducts(limit);
  }

  // Helper method to get month name
  getSelectedMonthName(): string {
    if (!this.selectedMonth) return 'Tất cả';
    const month = this.months.find(m => m.value === this.selectedMonth);
    return month ? month.name : 'Tất cả';
  }

  // Calculate summary statistics for current data
  calculateSummaryStats(): any {
    if (!this.transactions || this.transactions.length === 0) {
      return { totalRevenue: 0, totalSales: 0, totalPurchases: 0, totalReturns: 0, netRevenue: 0 };
    }

    let totalSales = 0;
    let totalPurchases = 0;
    let totalReturns = 0;

    this.transactions.forEach(transaction => {
      const amount = Math.abs(transaction.totalPrice);
      switch (transaction.transactionType) {
        case 'SALE':
          totalSales += amount;
          break;
        case 'PURCHASE':
          totalPurchases += amount;
          break;
        case 'RETURN':
          totalReturns += amount;
          break;
      }
    });

    const netRevenue = totalSales - totalPurchases - totalReturns;

    return {
      totalSales,
      totalPurchases,
      totalReturns,
      netRevenue,
      totalTransactions: this.transactions.length
    };
  }

  // Get color for net revenue display
  getNetRevenueColor(): string {
    const stats = this.calculateSummaryStats();
    return stats.netRevenue >= 0 ? 'green' : 'red';
  }
}
