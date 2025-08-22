import { Injectable } from '@angular/core';

declare var PNotify: any;

@Injectable({
  providedIn: 'root'
})
export class NotificationService {

  constructor() { }

  showSuccess(title: string, message: string) {
    new PNotify({
      title: title,
      text: message,
      type: 'success',
      styling: 'bootstrap3'
    });
  }

  showError(title: string, message: string) {
    new PNotify({
      title: title,
      text: message,
      type: 'error',
      styling: 'bootstrap3'
    });
  }

  showInfo(title: string, message: string) {
    new PNotify({
      title: title,
      text: message,
      type: 'info',
      styling: 'bootstrap3'
    });
  }

  showWarning(title: string, message: string) {
    new PNotify({
      title: title,
      text: message,
      type: 'notice',
      styling: 'bootstrap3'
    });
  }
}
