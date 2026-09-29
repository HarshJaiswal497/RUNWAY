import {
  Component, ElementRef, forwardRef,
  HostListener, Input, signal
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

export interface RwOption {
  value: any;
  label: string;
  disabled?: boolean;
}


@Component({
  selector: 'rw-select',
  standalone: true,
  imports: [],
  templateUrl: './rw-select.component.html',
  styleUrl: './rw-select.component.css',
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => RwSelectComponent),
    multi: true
  }]
})
export class RwSelectComponent implements ControlValueAccessor {
  @Input() options: RwOption[] = [];
  @Input() placeholder = 'Select';

  isOpen     = signal(false);
  isDisabled = signal(false);

  private _value: any = null;
  private onChange:  (v: any) => void = () => {};
  private onTouched: ()       => void = () => {};

  constructor(private el: ElementRef) {}

  // ── ControlValueAccessor ───────────────────────────────────────────────────
  writeValue(val: any): void          { this._value = val; }
  registerOnChange(fn: any): void     { this.onChange = fn; }
  registerOnTouched(fn: any): void    { this.onTouched = fn; }
  setDisabledState(d: boolean): void  { this.isDisabled.set(d); }

  // ── Derived ────────────────────────────────────────────────────────────────
  get selectedLabel(): string {
    return this.options.find(o => o.value === this._value)?.label ?? this.placeholder;
  }

  get hasSelection(): boolean {
    return this.options.some(o => o.value === this._value);
  }

  isSelected(opt: RwOption): boolean {
    return opt.value === this._value;
  }

  // ── Interaction ────────────────────────────────────────────────────────────
  toggle(): void {
    if (this.isDisabled()) return;
    this.isOpen.update(v => !v);
    this.onTouched();
  }

  select(opt: RwOption): void {
    if (opt.disabled) return;
    this._value = opt.value;
    this.onChange(opt.value);
    this.isOpen.set(false);
  }

  @HostListener('document:click', ['$event'])
  onDocClick(e: MouseEvent): void {
    if (!this.el.nativeElement.contains(e.target)) {
      this.isOpen.set(false);
    }
  }

  @HostListener('keydown', ['$event'])
  onKeyDown(e: KeyboardEvent): void {
    if (e.key === 'Escape') this.isOpen.set(false);
  }
}
