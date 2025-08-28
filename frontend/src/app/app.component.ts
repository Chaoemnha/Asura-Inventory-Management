import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import {
  Router,
  RouterLink,
  RouterOutlet,
  NavigationEnd,
} from '@angular/router';
import { ApiService } from './service/api.service';
import { filter } from 'rxjs';
import { sha256 } from 'js-sha256';
import { ChatAssistantComponent } from './chat-assistant/chat-assistant.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, CommonModule, ChatAssistantComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent implements OnInit {
  title = 'Inventory Management';
  activeRoute: string = '';
  user: any = {
    name: 'Loading...',
    email: '',
    role: '',
  };
  hashEmail = '';

  constructor(
    private apiService: ApiService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {
    // Cu, tu dong getLogged, moi, kiem tra auth roi moi get
    if (this.apiService.isAuthenticated()) {
      this.loadUserInfo();
    }
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
    // Kiem tra su thay doi cua route de cap nhat menu theo
    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe((event: NavigationEnd) => {
        this.activeRoute = event.url;
      });

    // Kiem tra trang thai dang nhap
    this.apiService.authStatuschanged.subscribe(() => {
      if (this.apiService.isAuthenticated()) {
        this.loadUserInfo();
      } else {
        this.user = {
          name: 'Loading...',
          email: '',
          role: '',
        };
        this.hashEmail = '';
      }
    });
  }

  isAuth(): boolean {
    return this.apiService.isAuthenticated();
  }

  isAdmin(): boolean {
    return this.apiService.isAdmin();
  }

  logout() {
    this.user = {
      name: 'Loading...',
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
      this.activeRoute === route || this.activeRoute.startsWith(route + '/')
    );
  }
}
