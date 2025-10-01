import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import {
  Router,
  RouterLink,
  RouterOutlet,
  NavigationEnd,
  ActivatedRoute,
} from '@angular/router';
import { ApiService } from './service/api.service';
import { filter, Subscription } from 'rxjs';
import { sha256 } from 'js-sha256';
import { ChatAssistantComponent } from './chat-assistant/chat-assistant.component';
import { WebSocketService } from './service/websocket.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, CommonModule, ChatAssistantComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent implements OnInit, OnDestroy {
  title = 'Inventory Management';
  activeRoute: string = '';
  user: any = {
    name: 'Guest',
    email: '',
    role: '',
  };
  hashEmail = '';
  categories: any[] = [];
  showProductSubmenu: boolean = false;
  isCategory:boolean=false;
  showUserDropdown: boolean = false;
  private wsSubscription!: Subscription;

  constructor(
    private apiService: ApiService,
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef,
    private webSocketService:WebSocketService
  ) {
    // Cu, tu dong getLogged, moi, kiem tra auth roi moi get
    if (this.apiService.isAuthenticated()) {
      this.loadUserInfo();
    }
      this.loadCategories();
  }

  private loadUserInfo(): void {
    this.apiService.getLoggedInUserInfo().subscribe({
      next: (res: any) => {
        this.user = res;
        const encoder = new TextEncoder();
        const data = encoder.encode(res.email);
        const hash = sha256.create().update(data);
        hash.hex();
        this.hashEmail = hash.hex();
      },
      error: (error: any) => {
        console.log('Failed to load user info:', error);
        // If failed to get user info, logout
        this.logout();
      },
    });
  }

  ngOnInit() {
    //Ket noi websocket tổng
    this.webSocketService.connect();
    // Kiem tra su thay doi cua route de cap nhat menu theo
    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe((event: NavigationEnd) => {
        this.activeRoute = event.url;
        // Auto open product submenu if on product page
        if (this.activeRoute.startsWith('/product')) {
          this.showProductSubmenu = true;
        }
      });

    // Kiem tra trang thai dang nhap
    this.apiService.authStatuschanged.subscribe(() => {
      if (this.apiService.isAuthenticated()) {
        this.loadUserInfo();
        this.loadCategories();
      } else {
        this.user = {
          name: 'Guest',
          email: '',
          role: '',
        };
        this.hashEmail = '';
        this.categories = [];
      }
    });
  }

  ngOnDestroy(){
    if(this.wsSubscription){
      this.wsSubscription.unsubscribe();
    }
    this.webSocketService.disconnect();
  }
  
  private loadCategories(): void {
      this.apiService.getAllCategory().subscribe({
        next: (res: any) => {
          this.categories = res.categories || [];
        },
        error: (error: any) => {
          console.log('Failed to load categories:', error);
        }
      });
  }

  toggleProductSubmenu(event: Event): void {
    event.preventDefault();
    this.showProductSubmenu = !this.showProductSubmenu;
  }

  toggleUserDropdown(event: Event): void {
    event.preventDefault();
    this.showUserDropdown = !this.showUserDropdown;
  }

  // Close dropdown when clicking outside
  closeDropdowns(): void {
    this.showUserDropdown = false;
  }

  isAuth(): boolean {
    return this.apiService.isAuthenticated();
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

  logout() {
    this.user = {
      name: 'Khách',
      email: '',
      role: '',
    };
    this.hashEmail = '';
    
    this.apiService.logout();
    this.router.navigate(['/login']);
    this.cdr.detectChanges();
  }

  // Check if route is active
  isActiveRoute(route: string): boolean {
    return (
      this.activeRoute === route || this.activeRoute.startsWith(route + '/')||this.activeRoute.includes(route)
    );
  }

  // Check if current route has query parameters
  hasQueryParams(): boolean {
    return this.activeRoute.includes('?');
  }

  // Check if specific category is active
  isActiveCategoryRoute(categoryName: string): boolean {
    const encodedCategoryName = encodeURIComponent(categoryName);
    return this.activeRoute === `/product?category=${encodedCategoryName}` || 
           this.activeRoute.includes(`category=${encodedCategoryName}`);
  }
}
