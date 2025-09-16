import { Injectable } from '@angular/core';
import { ApiService } from './api.service';
import { ActivatedRouteSnapshot, CanActivate, GuardResult, MaybeAsync, Router, RouterStateSnapshot } from '@angular/router';


@Injectable({
  providedIn: 'root'
})

export class GuardService implements CanActivate {

  constructor(private apiService: ApiService, private router: Router) { }

  canActivate(
    route: ActivatedRouteSnapshot,
     state: RouterStateSnapshot): boolean {

    // Kiểm tra xem user đã đăng nhập chưa
    if (!this.apiService.isAuthenticated()) {
      this.router.navigate(['/login'], {
        queryParams: { returnUrl: state.url }
      });
      return false;
    }

    // Lấy thông tin phân quyền từ route data
    const requiresAdmin = route.data['requiresAdmin'] || false;
    const allowedRoles = route.data['allowedRoles'] || [];
    const deniedRoles = route.data['deniedRoles'] || [];

    const userRole = this.apiService.getUserRole();

    // Kiểm tra nếu chỉ yêu cầu ADMIN (backward compatibility)
    if (requiresAdmin && !allowedRoles.length && !deniedRoles.length) {
      if (this.apiService.isAdmin()) {
        return true;
      } else {
        this.router.navigate(['/login'], {
          queryParams: { returnUrl: state.url }
        });
        return false;
      }
    }

    // Kiểm tra danh sách vai trò bị cấm
    if (deniedRoles.length > 0 && userRole && deniedRoles.includes(userRole)) {
      this.router.navigate(['/dashboard']);
      return false;
    }

    // Kiểm tra danh sách vai trò được phép
    if (allowedRoles.length > 0) {
      if (userRole && allowedRoles.includes(userRole)) {
        return true;
      } else {
        this.router.navigate(['/dashboard']);
        return false;
      }
    }

    // Mặc định cho phép nếu user đã đăng nhập và không có ràng buộc khác
    return true;
  }
}
