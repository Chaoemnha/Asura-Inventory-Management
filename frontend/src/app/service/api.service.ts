import { EventEmitter, Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, withJsonpSupport } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import CryptoJS from 'crypto-js';

@Injectable({
  providedIn: 'root',
})
export class ApiService {
  getAllProductsByCategoryName(categoryName: string, searchText: string = '', sortBy: string = 'stockQuantity', sortDirection: string = 'ASC'): Observable<any> {
    let params: any = {};
    if (searchText) params.searchText = searchText;
    if (sortBy) params.sortBy = sortBy;
    if (sortDirection) params.sortDirection = sortDirection;

    return this.http.get(`${ApiService.BASE_URL}/products/all/${categoryName}`, {
      params: params,
    });
  }
  authStatuschanged = new EventEmitter<void>();
  private static BASE_URL = environment.API_BASE_URL;
  private static ENCRYPTION_KEY = 'luannguyen';

  constructor(private http: HttpClient) {}

  // Encrypt data and save to sessionStorage (per-tab)
  encryptAndSaveToStorage(key: string, value: string): void {
    const encryptedValue = CryptoJS.AES.encrypt(
      value,
      ApiService.ENCRYPTION_KEY
    ).toString();
    sessionStorage.setItem(key, encryptedValue); // ← Đổi từ localStorage thành sessionStorage
  }

  // Retreive from sessionStorage and Decrypt
  private getFromStorageAndDecrypt(key: string): any {
    try {
      const encryptedValue = sessionStorage.getItem(key); // ← Đổi từ localStorage thành sessionStorage
      if (!encryptedValue) return null;
      return CryptoJS.AES.decrypt(
        encryptedValue,
        ApiService.ENCRYPTION_KEY
      ).toString(CryptoJS.enc.Utf8);
    } catch (error) {
      return null;
    }
  }

  private clearAuth() {
    sessionStorage.removeItem('token'); // ← Đổi từ localStorage thành sessionStorage
    sessionStorage.removeItem('role');  // ← Đổi từ localStorage thành sessionStorage
  }

  private getHeader(): HttpHeaders {
    const token = this.getFromStorageAndDecrypt('token');
    return new HttpHeaders({
      Authorization: `Bearer ${token}`,
    });
  }
  /***AUTH & USERS API METHODS */
  registerUser(body: any): Observable<any> {
    return this.http.post(`${ApiService.BASE_URL}/auth/register`, body);
  }

  loginUser(body: any): Observable<any> {
    return this.http.post(`${ApiService.BASE_URL}/auth/login`, body);
  }

  forgotPassword(email: string){
    return this.http.post(`${ApiService.BASE_URL}/auth/forgot-password?email=${email}`,null);
  }

  resetPassword(token: string, password: string){
    return this.http.post(`${ApiService.BASE_URL}/auth/reset-password?token=${token}&password=${password}`, null);
  }

  validateResetToken(token: string){
    return this.http.get(`${ApiService.BASE_URL}/auth/reset-password`, {
      params: {
        token: token,
      },
    });
  }
  
  getLoggedInUserInfo(): Observable<any> {
    const url = new URL(`${ApiService.BASE_URL}/users/current`);
    return this.http.get(url.toString(), {
      headers: this.getHeader(),
    });
  }

  getAllUsers(): Observable<any> {
    return this.http.get(`${ApiService.BASE_URL}/users/all`, {
      headers: this.getHeader(),
    });
  }

  getUserById(id: string): Observable<any> {
    return this.http.get(`${ApiService.BASE_URL}/users/one/${id}`, {
      headers: this.getHeader(),
    });
  }

  getUserBySupplierId(id: string): Observable<any> {
    return this.http.get(`${ApiService.BASE_URL}/users/supplier/${id}`, {
      headers: this.getHeader(),
    });
  }
  
  updateUser(id: string, body: any): Observable<any> {
    return this.http.put(`${ApiService.BASE_URL}/users/update/${id}`, body, {
      headers: this.getHeader(),
    });
  }

  deleteUser(id: string): Observable<any> {
    return this.http.delete(`${ApiService.BASE_URL}/users/delete/${id}`, {
      headers: this.getHeader(),
    });
  }

  getStaffActivityReport(staffId: string, fromDate?: string, toDate?: string): Observable<any> {
    const params: any = {};
    params.staffId = staffId;
    if (fromDate) params.fromDate = fromDate+"T00:00:00";
    if (toDate) params.toDate = toDate+"T00:00:00";
    
    return this.http.get(`${ApiService.BASE_URL}/transactions/activity-report`, {
      headers: this.getHeader(),
      params: params
    });
  }

  /**CATEGOTY ENDPOINTS */
  createCategory(body: any): Observable<any> {
    return this.http.post(`${ApiService.BASE_URL}/categories/add`, body, {
      headers: this.getHeader(),
    });
  }

  getAllCategory(): Observable<any> {
    return this.http.get(`${ApiService.BASE_URL}/categories/all`, {
    });
  }

  getCategoryById(id: string): Observable<any> {
    return this.http.get(`${ApiService.BASE_URL}/categories/${id}`, {
      headers: this.getHeader(),
    });
  }

  updateCategory(id: string, body: any): Observable<any> {
    return this.http.put(
      `${ApiService.BASE_URL}/categories/update/${id}`,
      body,
      {
        headers: this.getHeader(),
      }
    );
  }

  deleteCategory(id: string): Observable<any> {
    return this.http.delete(`${ApiService.BASE_URL}/categories/delete/${id}`, {
      headers: this.getHeader(),
    });
  }
  /** SUPPLIER API */
  addSupplier(body: any): Observable<any> {
    return this.http.post(`${ApiService.BASE_URL}/suppliers/add`, body, {
      headers: this.getHeader(),
    });
  }

  getAllSuppliers(): Observable<any> {
    return this.http.get(`${ApiService.BASE_URL}/suppliers/all`, {
      headers: this.getHeader(),
    });
  }

  createSupplier(body: any): Observable<any> {
    return this.http.post(`${ApiService.BASE_URL}/suppliers/add`, body, {
      headers: this.getHeader(),
    });
  }

  getSupplierById(id: string): Observable<any> {
    return this.http.get(`${ApiService.BASE_URL}/suppliers/${id}`, {
      headers: this.getHeader(),
    });
  }

  updateSupplier(id: string, body: any): Observable<any> {
    return this.http.put(
      `${ApiService.BASE_URL}/suppliers/update/${id}`,
      body,
      {
        headers: this.getHeader(),
      }
    );
  }

  deleteSupplier(id: string): Observable<any> {
    return this.http.delete(`${ApiService.BASE_URL}/suppliers/delete/${id}`, {
      headers: this.getHeader(),
    });
  }
  /**PRODUICTS ENDPOINTS */
  addProduct(formData: any): Observable<any> {
    return this.http.post(`${ApiService.BASE_URL}/products/add`, formData, {
      headers: this.getHeader(),
    });
  }

  updateProduct(formData: any): Observable<any> {
    return this.http.put(`${ApiService.BASE_URL}/products/update`, formData, {
      headers: this.getHeader(),
    });
  }

  getAllProducts(searchText: string = '', sortBy: string = 'stockQuantity', sortDirection: string = 'ASC'): Observable<any> {
    let params: any = {};
    if (searchText) params.searchText = searchText;
    if (sortBy) params.sortBy = sortBy;
    if (sortDirection) params.sortDirection = sortDirection;

    return this.http.get(`${ApiService.BASE_URL}/products/all`, {
      params: params,
    });
  }

  getProductById(id: string): Observable<any> {
    return this.http.get(`${ApiService.BASE_URL}/products/${id}`, {
      headers: this.getHeader(),
    });
  }

  deleteProduct(id: string): Observable<any> {
    return this.http.delete(`${ApiService.BASE_URL}/products/delete/${id}`, {
      headers: this.getHeader(),
    });
  }
  /**Transactions Endpoints */
  purchaseProduct(body: any): Observable<any> {
    return this.http.post(
      `${ApiService.BASE_URL}/transactions/purchase`,
      body,
      {
        headers: this.getHeader(),
      }
    );
  }

  sellProduct(body: any): Observable<any> {
    return this.http.post(`${ApiService.BASE_URL}/transactions/sell`, body, {
      headers: this.getHeader(),
    });
  }

  returnProduct(body: any): Observable<any> {
    return this.http.post(`${ApiService.BASE_URL}/transactions/return`, body, {
      headers: this.getHeader(),
    });
  }

  getAllTransactions(userId: number, 
                     searchType?: string, searchStatus?: string, searchProductName?: string, 
                     searchFromDate?: string, searchToDate?: string, page?: number, size?: number): Observable<any> {
    const params: any = {
      userId: userId, 
    };
    
    if (searchType) params.searchType = searchType;
    if (page) params.page = page;
    if (size) params.size = size;
    if (searchStatus) params.searchStatus = searchStatus;
    if (searchProductName) params.searchProductName = searchProductName;
    if (searchFromDate) params.searchFromDate = searchFromDate+"T00:00:00";
    if (searchToDate) params.searchToDate = searchToDate+"T00:00:00";
    
    return this.http.get(`${ApiService.BASE_URL}/transactions/all`, {
      params: params,
      headers: this.getHeader(),
    });
  }

  getTransactionById(id: string): Observable<any> {
    return this.http.get(`${ApiService.BASE_URL}/transactions/${id}`, {
      headers: this.getHeader(),
    });
  }

  updateTransactionStatus(id: string, status: string): Observable<any> {
    return this.http.put(
      `${ApiService.BASE_URL}/transactions/update/${id}`,
      JSON.stringify(status),
      {
        headers: this.getHeader().set('Content-Type', 'application/json'),
      }
    );
  }

  getTransactionsByMonthAndYear(month: number, year: number): Observable<any> {
    return this.http.get(`${ApiService.BASE_URL}/transactions/by-month-year`, {
      headers: this.getHeader(),
      params: {
        month: month,
        year: year,
      },
    });
  }

  downloadFile(url: string, type: string){
    const token = this.getFromStorageAndDecrypt('token');
    fetch(url, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
    .then(response => {
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      return response.blob();
    })
    .then(blob => {
      const fileName = 'transaction-report';
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.style.display = 'none';
      link.href = url;
      link.download = `${fileName}_${new Date().toISOString().split('T')[0]}.xlsx`;
      
      document.body.appendChild(link);
      link.click();
      link.parentNode?.removeChild(link);
      window.URL.revokeObjectURL(url);
      
    })
    .catch(error => {
      console.error('Export error:', error);
    });
  }
  /**AUTHENTICATION CHECKER */
  logout(): void {
    this.clearAuth();
    this.authStatuschanged.emit();
  }

  isAuthenticated(): boolean {
    const token = this.getFromStorageAndDecrypt('token');
    return !!token;
  }

  isAdmin(): boolean {
    const role = this.getFromStorageAndDecrypt('role');
    return role === 'ADMIN';
  }

  getUserRole(): string | null {
    return this.getFromStorageAndDecrypt('role');
  }

  isStockStaff(): boolean {
    const role = this.getFromStorageAndDecrypt('role');
    return role === 'STOCKSTAFF';
  }

  isSupplier(): boolean {
    const role = this.getFromStorageAndDecrypt('role');
    return role === 'SUPPLIER';
  }

  isCustomer(): boolean {
    const role = this.getFromStorageAndDecrypt('role');
    return role === 'CUSTOMER';
  }

  hasRole(requiredRoles: string[]): boolean {
    const userRole = this.getUserRole();
    return userRole ? requiredRoles.includes(userRole) : false;
  }

  logged(){
    this.authStatuschanged.emit();
  }

  exportInvoiceWithQR(id: any, status: any) {
    const token = this.getFromStorageAndDecrypt('token');
    const url = new URL(`${ApiService.BASE_URL}/transactions/export-invoice-qr`);
    url.searchParams.append('transactionId', id);
    url.searchParams.append('transactionStatus', status);
    fetch(url, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`
      },
    })
    .then(response => {
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      return response.blob();
    }).then(blob => {
      const fileName = 'invoiceQR';
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.style.display = 'none';
      link.href = url;
      link.download = `${fileName}.pdf`;
      
      document.body.appendChild(link);
      link.click();
      link.parentNode?.removeChild(link);
      window.URL.revokeObjectURL(url);
      
    })
    .catch(error => {
      console.error('Export error:', error);
    });
  }
  
  goToQRLink(input: any): Observable<any> {
    return this.http.get(input, {
      headers: this.getHeader(),
    });
  }

  /**REPORTS ENDPOINTS */
  getRevenueReport(startDate: string, endDate: string): Observable<any> {
    return this.http.get(`${ApiService.BASE_URL}/reports/revenue`, {
      headers: this.getHeader(),
      params: {
        startDate: startDate,
        endDate: endDate
      }
    });
  }

  getProductProfitReport(startDate: string, endDate: string): Observable<any> {
    return this.http.get(`${ApiService.BASE_URL}/reports/profit/products`, {
      headers: this.getHeader(),
      params: {
        startDate: startDate,
        endDate: endDate
      }
    });
  }

  getMonthlyPerformanceReport(): Observable<any> {
    return this.http.get(`${ApiService.BASE_URL}/reports/performance/monthly`, {
      headers: this.getHeader(),
    });
  }

  getCurrentInventoryValue(): Observable<any> {
    return this.http.get(`${ApiService.BASE_URL}/reports/inventory/value`, {
      headers: this.getHeader(),
    });
  }

  getTopInventoryProducts(limit?: number): Observable<any> {
    const params: any = {};
    if (limit) params.limit = limit;
    
    return this.http.get(`${ApiService.BASE_URL}/products/reports/top-inventory`, {
      headers: this.getHeader(),
      params: params
    });
  }

  getBestSellingProducts(limit?: number, fromDate?: string, toDate?: string): Observable<any> {
    const params: any = {};
    if (limit) params.limit = limit;
    if (fromDate) params.fromDate = fromDate;
    if (toDate) params.toDate = toDate;
    
    return this.http.get(`${ApiService.BASE_URL}/transactions/reports/best-selling`, {
      headers: this.getHeader(),
      params: params
    });
  }

  updateTransactionByAdmin(transactionId: string, updateData: any): Observable<any> {
    return this.http.put(`${ApiService.BASE_URL}/transactions/admin-update/${transactionId}`, updateData, {
      headers: this.getHeader(),
    });
  }

  // AI Chat with RAG
  chatWithAI(question: string): Observable<any> {
    return this.http.post(`${ApiService.BASE_URL}/rag/chat`, { question }, {
      headers: this.getHeader(),
    });
  }

}
