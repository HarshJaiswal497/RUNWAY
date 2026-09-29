import { Directive, ElementRef, HostListener, Input, Renderer2 } from '@angular/core';

@Directive({
  selector: '[rwTooltip]',
  standalone: true,
})
export class TooltipDirective {
  @Input('rwTooltip') text = '';

  private tooltipEl: HTMLElement | null = null;

  constructor(private el: ElementRef<HTMLElement>, private renderer: Renderer2) {}

  @HostListener('mouseenter')
  show(): void {
    if (!this.text || this.tooltipEl) return;

    const tip = this.renderer.createElement('div') as HTMLElement;
    this.renderer.addClass(tip, 'rw-tooltip');
    this.renderer.setProperty(tip, 'textContent', this.text);

    // Position before appending so the element is placed correctly on first paint
    const host = this.el.nativeElement.getBoundingClientRect();
    this.renderer.setStyle(tip, 'top',  `${host.bottom + 9}px`);
    this.renderer.setStyle(tip, 'left', `${host.left + host.width / 2}px`);

    this.renderer.appendChild(document.body, tip);
    this.tooltipEl = tip;

    // After layout, clamp horizontally so it never overflows the viewport
    requestAnimationFrame(() => {
      if (!this.tooltipEl) return;
      const tipRect = this.tooltipEl.getBoundingClientRect();
      const margin  = 10;
      const vw      = window.innerWidth;
      if (tipRect.left < margin) {
        this.renderer.setStyle(this.tooltipEl, 'left', `${tipRect.width / 2 + margin}px`);
      } else if (tipRect.right > vw - margin) {
        this.renderer.setStyle(this.tooltipEl, 'left', `${vw - tipRect.width / 2 - margin}px`);
      }
    });
  }

  @HostListener('mouseleave')
  @HostListener('click')
  hide(): void {
    if (this.tooltipEl) {
      this.renderer.removeChild(document.body, this.tooltipEl);
      this.tooltipEl = null;
    }
  }
}
