import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ApiService } from '../service/api.service';
import { NotificationService } from '../service/notification.service';
import {sha256} from 'js-sha256';
@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css'
})
export class ProfileComponent implements OnInit {
  constructor(
    private apiService: ApiService,
    private notificationService: NotificationService
  ){}
  
  user: any = {
    name: 'Loading...',
    email: 'user@example.com',
    phoneNumber: '+84 123 456 789',
    role: 'User'
  }
  
  hashEmail = '';

  ngOnInit(): void {
    this.fetchUserInfo();
  }

  fetchUserInfo():void{
    this.apiService.getLoggedInUserInfo().subscribe({
      next:(res)=>{
        this.user = res;
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
            'Unable to Get Profile Info: ' + error
        );
      }
    })
  }
}
