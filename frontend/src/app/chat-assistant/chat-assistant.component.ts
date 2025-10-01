import { Component, OnInit, ElementRef, ViewChild, AfterViewChecked } from '@angular/core';
import { ApiService } from '../service/api.service';
import { Router } from '@angular/router';
import { NotificationService } from '../service/notification.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-chat-assistant',
  imports: [CommonModule, FormsModule],
  templateUrl: './chat-assistant.component.html',
  styleUrl: './chat-assistant.component.css', 
  standalone: true,
})
export class ChatAssistantComponent implements OnInit {
  @ViewChild('chatMessages') private chatMessages!: ElementRef;
  
  messages: any[] = [];
  currentMessage: string = '';
  isLoading: boolean = false;

  constructor(
    private apiService: ApiService,
    private router: Router,
    private notificationService: NotificationService
  ) {}

  ngOnInit() {
    // Initialize with welcome message
    setTimeout(() => {
      this.addWelcomeMessage();
    }, 100);
    // this.setupEventListeners();
  window.addEventListener('ai-navigate', this.handleNavigateEvent.bind(this));
}

handleNavigateEvent(event: any) {
  const url = event.detail?.url;
  if (url) {
    this.router.navigate([url]);
  }
}

  private addWelcomeMessage() {
    const chatMessages = document.getElementById('chatMessages');
    if (chatMessages) {
      // Clear existing welcome message to avoid duplicates
      const existingWelcome = chatMessages.querySelector('.welcome-message');
      if (!existingWelcome) {
        const welcomeHtml = `
          <div class="welcome-message text-center">
            <i class="fa fa-android" style="font-size: 24px; margin-bottom: 10px;"></i><br>
            Xin chào! Tôi là trợ lý ảo RAG, hãy hỏi tôi bất cứ điều gì bạn muốn biết.
          </div>
        `;
        chatMessages.innerHTML = welcomeHtml;
      }
    }
  }
}
