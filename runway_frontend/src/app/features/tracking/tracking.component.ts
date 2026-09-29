import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ShipmentService } from '../../core/services/shipment.service';
import { AuthService } from '../../core/services/auth.service';
import { ShipmentResponse, ShipmentStatus } from '../../core/models/order.model';

// Exact ShipmentStatus values from backend Shipment.ShipmentStatus enum
// RETURNED is excluded from the progress stepper (it's a terminal exception state)
const PROGRESS_STEPS: ShipmentStatus[] = [
  'PROCESSING',
  'DISPATCHED',
  'IN_TRANSIT',
  'OUT_FOR_DELIVERY',
  'DELIVERED'
];

@Component({
  selector: 'app-tracking',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './tracking.component.html',
  styleUrl: './tracking.component.css'
})
export class TrackingComponent implements OnInit {
  private shipmentSvc = inject(ShipmentService);
  private auth        = inject(AuthService);
  private route       = inject(ActivatedRoute);
  private router      = inject(Router);

  loading  = signal(true);
  error    = signal<string | null>(null);
  shipment = signal<ShipmentResponse | null>(null);
  orderId  = signal<number>(0);

  readonly steps = PROGRESS_STEPS;

  // RETURNED is a special terminal failure state separate from the stepper
  isReturned = computed(() => this.shipment()?.status === 'RETURNED');

  currentStepIndex = computed(() => {
    const s = this.shipment();
    if (!s || s.status === 'RETURNED') return -1;
    return PROGRESS_STEPS.indexOf(s.status);
  });

  ngOnInit(): void {
    const userId  = this.auth.userId();
    const orderId = Number(this.route.snapshot.paramMap.get('id'));

    if (!userId) { this.router.navigate(['/login']); return; }

    this.orderId.set(orderId);

    this.shipmentSvc.getByOrderId(orderId).subscribe({
      next: (s)  => { this.shipment.set(s); this.loading.set(false); },
      error: ()  => {
        this.error.set('Tracking information is not yet available for this order. Check back once your order is in processing.');
        this.loading.set(false);
      }
    });
  }

  stepDone(index: number): boolean   { return index <= this.currentStepIndex(); }
  stepActive(index: number): boolean { return index === this.currentStepIndex(); }
  stepLabel(status: ShipmentStatus): string { return status.replace(/_/g, ' '); }
}