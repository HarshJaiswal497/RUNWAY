import { Component, OnInit, OnDestroy, inject, signal, NgZone } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
    selector: 'app-info',
    standalone: true,
    imports: [],
    templateUrl: './info.component.html',
    styleUrl: './info.component.css'
})
export class InfoComponent implements OnInit, OnDestroy {
    private route  = inject(ActivatedRoute);
    private zone   = inject(NgZone);

    readonly sections = ['sizing','delivery','returns','faq','sustainability','careers','press','privacy','terms'];
    activeSection = signal<string>('sizing');
    private _scrollHandler: (() => void) | null = null;
    /** True while a programmatic smooth-scroll is in flight; pauses scroll tracking. */
    private _scrollLock = false;

    faqs = [
        {
            q: 'How do I know if a product is authentic?',
            a: 'Every piece on RunWay is sourced directly from the atelier houses we partner with. We do not work with third-party resellers. Each product is inspected by our curation team before listing.',
            open: false
        },
        {
            q: 'Can I cancel or modify my order after placing it?',
            a: 'Orders can be cancelled before payment is confirmed. Once payment is received, the order enters processing and cannot be modified. If you need assistance, please contact our support team immediately.',
            open: false
        },
        {
            q: 'Do you offer gift wrapping?',
            a: 'All RunWay orders are presented in our signature packaging by default — a rigid black box with gold ribbon. Gift messaging can be added at checkout. There is no additional charge.',
            open: false
        },
        {
            q: 'What payment methods do you accept?',
            a: 'We accept all major credit and debit cards, UPI, net banking, and select wallet payments. All transactions are secured with 256-bit SSL encryption.',
            open: false
        },
        {
            q: 'How do I track my order?',
            a: 'Once your order is dispatched, a tracking number is sent to your registered email. You can also track your shipment from the Orders section of your account.',
            open: false
        },
        {
            q: 'Are your pieces covered by warranty?',
            a: 'All RunWay pieces are covered by a 6-month manufacturing defect warranty. This does not cover wear and tear, accidental damage, or improper care. Contact support to raise a warranty claim.',
            open: false
        },
        {
            q: 'Do you have a physical store?',
            a: 'RunWay is exclusively online. This allows us to maintain a curated, global selection without the constraints of physical retail. We occasionally host private viewing events — announcements are made via our newsletter.',
            open: false
        },
        {
            q: 'How do I become a seller on RunWay?',
            a: 'We do not accept applications. Our curation team identifies and invites designers whose work meets our standards. If you believe your house is a fit, you may send a portfolio to curation@runway.in.',
            open: false
        },
    ];

    ngOnInit(): void {
        window.scrollTo({ top: 0, behavior: 'instant' });

        // Fragment navigation (tab clicks or direct URL)
        this.route.fragment.subscribe(fragment => {
            if (fragment && this.sections.includes(fragment)) {
                // Immediately mark the tab the user intended
                this.activeSection.set(fragment);
                // Lock scroll tracking briefly so the smooth-scroll animation
                // doesn't flicker the active tab to a passing section
                this._scrollLock = true;
                setTimeout(() => {
                    document.getElementById(fragment)
                        ?.scrollIntoView({ behavior: 'smooth', block: 'start' });
                    // Release lock after the smooth scroll typically completes (~600 ms)
                    setTimeout(() => { this._scrollLock = false; }, 700);
                }, 50);
            }
        });

        // Scroll-event tracking: find the last section whose top edge is above
        // the trigger line (sticky navbar ~68 px + info nav ~50 px + buffer = 140 px)
        setTimeout(() => {
            this._updateActive();
            this._scrollHandler = () => {
                if (this._scrollLock) return;
                this._updateActive();
            };
            // Run outside Angular zone for performance; signal.set re-enters it
            this.zone.runOutsideAngular(() => {
                window.addEventListener('scroll', this._scrollHandler!, { passive: true });
            });
        }, 150);
    }

    private _updateActive(): void {
        const TRIGGER_Y = 140; // px from viewport top — just below both sticky bars
        let current = this.sections[0];
        for (const id of this.sections) {
            const el = document.getElementById(id);
            if (!el) continue;
            if (el.getBoundingClientRect().top <= TRIGGER_Y) {
                current = id;
            }
        }
        this.zone.run(() => this.activeSection.set(current));
    }

    ngOnDestroy(): void {
        if (this._scrollHandler) {
            window.removeEventListener('scroll', this._scrollHandler);
        }
    }
}