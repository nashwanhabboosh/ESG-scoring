import { Component, OnInit, Input, Output, EventEmitter, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EsgService, Company, ESGScorecard, RawMetric } from '../../services/esg';

@Component({
  selector: 'app-company-detail',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './company-detail.html',
  styleUrl: './company-detail.css'
})
export class CompanyDetailComponent implements OnInit {

  @Input() companyId!: string;
  @Output() back = new EventEmitter<void>();

  company: Company | null = null;
  scorecard: ESGScorecard | null = null;
  metrics: RawMetric[] = [];
  loading = true;

  constructor(private esgService: EsgService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;

    this.esgService.getCompany(this.companyId).subscribe(company => {
      this.company = company;
      this.cdr.detectChanges();

      this.esgService.getLatestScore(this.companyId).subscribe(scorecard => {
        this.scorecard = scorecard;
        this.cdr.detectChanges();

        this.esgService.getMetrics(this.companyId).subscribe(metrics => {
          this.metrics = metrics.filter(m => m.year === 2024);
          this.loading = false;
          this.cdr.detectChanges();
        });
      });
    });
  }

  rescore(): void {
    this.loading = true;
    this.esgService.scoreCompany(this.companyId).subscribe(scorecard => {
      this.scorecard = scorecard;
      this.loading = false;
      this.cdr.detectChanges();
    });
  }

  onBack(): void {
    this.back.emit();
  }

  scoreClass(score: number): string {
    if (score >= 75) return 'score-high';
    if (score >= 50) return 'score-mid';
    return 'score-low';
  }

  formatMetricName(name: string): string {
    return name.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
  }
}