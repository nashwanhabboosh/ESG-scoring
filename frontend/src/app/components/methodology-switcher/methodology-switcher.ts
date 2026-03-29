import { Component, OnInit, Output, EventEmitter, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EsgService, ScoringMethodology } from '../../services/esg';

@Component({
  selector: 'app-methodology-switcher',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './methodology-switcher.html',
  styleUrl: './methodology-switcher.css'
})
export class MethodologySwitcherComponent implements OnInit {

  @Output() methodologyChanged = new EventEmitter<void>();

  methodologies: ScoringMethodology[] = [];
  loading = true;

  // Form state
  newName = '';
  newDescription = '';
  eWeight = 34;
  sWeight = 33;
  gWeight = 33;
  createSuccess = false;
  createError = false;

  constructor(private esgService: EsgService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.loadMethodologies();
  }

  loadMethodologies(): void {
    this.loading = true;
    this.esgService.getMethodologies().subscribe(methodologies => {
      this.methodologies = methodologies;
      this.loading = false;
      this.cdr.detectChanges();
    });
  }

  activate(id: string): void {
    this.esgService.activateMethodology(id).subscribe(() => {
      this.esgService.scoreAllCompanies().subscribe(() => {
        this.loadMethodologies();
        this.methodologyChanged.emit();
      });
    });
  }

  get weightTotal(): number {
    return this.eWeight + this.sWeight + this.gWeight;
  }

  // When E changes, adjust G to compensate keeping total at 100
  onEChange(): void {
    const remaining = 100 - this.eWeight;
    if (this.sWeight + this.gWeight > remaining) {
      const ratio = remaining / (this.sWeight + this.gWeight);
      this.sWeight = Math.round(this.sWeight * ratio);
      this.gWeight = remaining - this.sWeight;
    }
    this.cdr.detectChanges();
  }

  // When S changes, adjust G to compensate
  onSChange(): void {
    const remaining = 100 - this.eWeight - this.sWeight;
    this.gWeight = Math.max(0, remaining);
    if (this.eWeight + this.sWeight > 100) {
      this.eWeight = 100 - this.sWeight;
    }
    this.cdr.detectChanges();
  }

  // When G changes, adjust S to compensate
  onGChange(): void {
    const remaining = 100 - this.eWeight - this.gWeight;
    this.sWeight = Math.max(0, remaining);
    if (this.eWeight + this.gWeight > 100) {
      this.eWeight = 100 - this.gWeight;
    }
    this.cdr.detectChanges();
  }

  canCreate(): boolean {
    return this.newName.trim().length > 0 && this.weightTotal === 100;
  }

  create(): void {
    this.createSuccess = false;
    this.createError = false;

    const methodology: Partial<ScoringMethodology> = {
      name: this.newName.trim(),
      description: this.newDescription.trim(),
      environmentalWeight: this.eWeight / 100,
      socialWeight: this.sWeight / 100,
      governanceWeight: this.gWeight / 100,
      active: false
    };

    this.esgService.createMethodology(methodology as ScoringMethodology).subscribe({
      next: () => {
        this.createSuccess = true;
        this.newName = '';
        this.newDescription = '';
        this.eWeight = 34;
        this.sWeight = 33;
        this.gWeight = 33;
        this.loadMethodologies();
        this.cdr.detectChanges();
        setTimeout(() => { this.createSuccess = false; this.cdr.detectChanges(); }, 3000);
      },
      error: () => {
        this.createError = true;
        this.cdr.detectChanges();
        setTimeout(() => { this.createError = false; this.cdr.detectChanges(); }, 3000);
      }
    });
  }
}