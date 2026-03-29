import { Component, OnInit, Output, EventEmitter, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EsgService, ScoringMethodology } from '../../services/esg';

@Component({
  selector: 'app-methodology-switcher',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './methodology-switcher.html',
  styleUrl: './methodology-switcher.css'
})
export class MethodologySwitcherComponent implements OnInit {

  @Output() methodologyChanged = new EventEmitter<void>();

  methodologies: ScoringMethodology[] = [];
  loading = true;

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
}