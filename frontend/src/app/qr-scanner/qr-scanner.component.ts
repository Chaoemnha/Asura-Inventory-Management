import { Component, EventEmitter, OnInit, OnDestroy, Output, Input, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ZXingScannerModule, ZXingScannerComponent } from '@zxing/ngx-scanner';
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
  @Input() expectedTransactionId: string = ''; // Transaction ID để validate
  @Output() closeScanner = new EventEmitter<void>();
  @ViewChild('scanner', { static: false }) scanner!: ZXingScannerComponent;
  
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
    
    // Validate QR code contains the expected transaction ID
    if (!this.validateQRCode(result)) {
      this.notificationService.showError('Invalid QR Code', 
        `QR code does not match the expected transaction ID: ${this.expectedTransactionId}`);
      return; // Don't proceed if validation fails
    }
    
    this.apiService.goToQRLink(result).subscribe({
        next: (response) => {
            console.log('QR Link response:', response);
            if (response.status === 200) {
                this.notificationService.showSuccess('Success', 'Transaction status updated successfully!');
                // Close scanner and reload page
                this.close();
                setTimeout(() => {
                  window.location.reload();
                }, 1000);
            }
        },
        error: (error) => {
            console.error('QR Link error:', error);
            this.notificationService.showError('Error', 
                'Failed to process QR code: ' + (error?.error?.message || error.message));
            // Close scanner on error too
            this.close();
        }
    });
  }

  // Validate QR code contains the expected transaction ID
  private validateQRCode(qrResult: string): boolean {
    if (!this.expectedTransactionId) {
      console.warn('No expected transaction ID provided for validation');
      return true; // Allow if no expected ID is set (backward compatibility)
    }

    // Check if QR result contains the pattern: /api/transactions/update/{transactionId}
    const pattern = `/api/transactions/update/${this.expectedTransactionId}`;
    const isValid = qrResult.includes(pattern);
    
    console.log('QR Validation:', {
      qrResult,
      expectedPattern: pattern,
      isValid
    });
    
    return isValid;
  }

  onDeviceSelectChange(selectedValue: string): void {
    // Stop current video stream before switching
    this.stopCurrentStream();
    
    const device = this.availableDevices.find(x => x.deviceId === selectedValue);
    this.selectedDevice = device || this.selectedDevice;
    
    // Small delay to ensure previous stream is fully stopped
    setTimeout(() => {
      // The ZXing scanner will automatically start the new device
    }, 200);
  }

  private stopCurrentStream(): void {
    if (this.videoStream) {
      this.videoStream.getTracks().forEach(track => {
        track.stop();
        track.enabled = false;
      });
      this.videoStream = null;
    }
  }
  
  requestCameraPermission(): void {
    navigator.mediaDevices.getUserMedia({ video: true })
      .then((stream) => {
        this.hasPermission = true;
        // Stop the stream immediately after getting permission
        // We don't need to keep this stream as ZXing will create its own
        stream.getTracks().forEach(track => {
          track.stop();
          track.enabled = false;
        });
      })
      .catch((error) => {
        this.hasPermission = false;
        console.error('Camera permission denied:', error);
      });
  }

  onCamerasFound(devices: MediaDeviceInfo[]): void {
    this.availableDevices = devices.reverse();
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
    this.availableDevices = devices.reverse();
    if (devices.length > 0 && !this.selectedDevice) {
      // Only set initial device if none is selected
      // Prefer back camera for mobile devices
      const backCamera = devices.find(device => 
        device.label.toLowerCase().includes('back') || 
        device.label.toLowerCase().includes('rear')
      );
      this.selectedDevice = backCamera || devices[0];
    }
  }

  close(): void {
    // Stop ZXing scanner first
    if (this.scanner) {
      try {
        this.scanner.camerasNotFound.complete();
        this.scanner.camerasFound.complete();
        this.scanner.scanComplete.complete();
        this.scanner.scanError.complete();
        this.scanner.scanFailure.complete();
        this.scanner.scanSuccess.complete();
      } catch (e) {
        console.log('Error stopping scanner:', e);
      }
    }

    // Stop current video stream
    this.stopCurrentStream();

    // Stop all active video tracks more aggressively
    navigator.mediaDevices.enumerateDevices()
      .then(devices => {
        const videoDevices = devices.filter(device => device.kind === 'videoinput');
        
        // Try to stop streams from each video device
        const stopPromises = videoDevices.map(device => {
          return navigator.mediaDevices.getUserMedia({ 
            video: { deviceId: { exact: device.deviceId } } 
          })
          .then(stream => {
            stream.getTracks().forEach(track => {
              track.stop();
              track.enabled = false;
            });
          })
          .catch(() => {
            // Device might not be active, ignore
          });
        });

        Promise.allSettled(stopPromises).then(() => {
          console.log('All camera streams stopped');
        });
      })
      .catch(() => {
        // Fallback: try to stop any active video stream
        navigator.mediaDevices.getUserMedia({ video: true })
          .then(stream => {
            stream.getTracks().forEach(track => {
              track.stop();
              track.enabled = false;
            });
          })
          .catch(() => {
            // Ignore errors when stopping tracks
          });
      });

    // Reset scanner state
    this.hasPermission = false;
    this.isCapturing = false;
    this.scannedResult = null;
    this.selectedDevice = undefined;
    this.currentDevice = undefined;
    
    // Emit close event
    this.closeScanner.emit();
  }

  ngOnDestroy(): void {
    this.close();
  }
}
