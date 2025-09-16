import { Component, EventEmitter, OnInit, OnDestroy, Output, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ZXingScannerModule } from '@zxing/ngx-scanner';
import { ApiService } from '../service/api.service';
import { NotificationService } from '../service/notification.service';
import { BarcodeFormat } from '@zxing/library';

@Component({
  selector: 'app-qr-scanner',
  imports: [CommonModule, FormsModule, ZXingScannerModule],
  templateUrl: './qr-scanner.component.html',
  styleUrl: './qr-scanner.component.css'
})
export class QrScannerComponent implements OnInit, OnDestroy {
  allowedFormats = [BarcodeFormat.QR_CODE];
  availableDevices: MediaDeviceInfo[] = [];
  currentDevice: MediaDeviceInfo | undefined;
  hasDevices: boolean = false;
  hasPermission: boolean = false;
  qrResultString: string = '';
  torchEnabled = false;
  tryHarder = false;
  videoStream: MediaStream | null = null;
  isCapturing: boolean = false;
  selectedDevice: MediaDeviceInfo | undefined;
  scannedResult: string | null = null;
  
  constructor(
    private apiService: ApiService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.requestCameraPermission();
  }

  onCodeResult(result: string) {
    this.scannedResult = result;
    this.apiService.goToQRLink(result).subscribe({
        next: (response) => {
            console.log('QR Link response:', response);
            if (response.status === 200) {
                this.notificationService.showSuccess('Success', 'Transaction status updated successfully!');
                // Reload trang hoặc đóng scanner
                window.location.reload();
            }
        },
        error: (error) => {
            console.error('QR Link error:', error);
            this.notificationService.showError('Error', 
                'Failed to process QR code: ' + (error?.error?.message || error.message));
        }
    });
  }

  onDeviceSelectChange(selectedValue: string): void {
    const device = this.availableDevices.find(x => x.deviceId === selectedValue);
    this.selectedDevice = device || this.selectedDevice;
  }
  
  requestCameraPermission(): void {
    navigator.mediaDevices.getUserMedia({ video: true })
      .then((stream) => {
        this.hasPermission = true;
        // Stop the stream immediately after getting permission
        this.videoStream = stream;
      })
      .catch((error) => {
        this.hasPermission = false;
        console.error('Camera permission denied:', error);
      });
  }

  onCamerasFound(devices: MediaDeviceInfo[]): void {
    this.availableDevices = devices;
    this.hasDevices = Boolean(devices && devices.length);
    
    // Select the first back camera or the first camera if no back camera
    const backCamera = devices.find(device => 
      device.label.toLowerCase().includes('back') || 
      device.label.toLowerCase().includes('rear')
    );
    this.currentDevice = backCamera || devices[0];
  }

  onError(error: any): void {
    console.error('QR Scanner error:', error);
  }

  onHasDevices(hasDevices: boolean) {
    this.hasDevices = hasDevices;
  }

  onDevicesFound(devices: MediaDeviceInfo[]) {
    this.availableDevices = devices;
    if (devices.length > 0) {
      this.selectedDevice = devices[devices.length-1]; // Fix: Ensure a device is selected initially
    }
  }

  close(): void {
    // Stop video stream
    if (this.videoStream) {
      this.videoStream.getTracks().forEach(track => track.stop());
      this.videoStream = null;
    }
  }

  ngOnDestroy(): void {
    this.close();
  }
}
