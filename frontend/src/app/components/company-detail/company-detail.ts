import { Component, OnInit, Input, Output, EventEmitter, ChangeDetectorRef, AfterViewInit, ViewChild, ElementRef, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EsgService, Company, ESGScorecard, RawMetric } from '../../services/esg';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-company-detail',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './company-detail.html',
  styleUrl: './company-detail.css'
})
export class CompanyDetailComponent implements OnInit, AfterViewInit, OnDestroy {

  @Input() companyId!: string;
  @Output() back = new EventEmitter<void>();
  @ViewChild('historyChart') historyChartRef!: ElementRef;

  company: Company | null = null;
  scorecard: ESGScorecard | null = null;
  metrics: RawMetric[] = [];
  history: ESGScorecard[] = [];
  loading = true;
  chart: Chart | null = null;
  viewReady = false;

  constructor(private esgService: EsgService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.load();
  }

  ngAfterViewInit(): void {
    this.viewReady = true;
    if (this.history.length > 0) {
      this.renderChart();
    }
  }

  ngOnDestroy(): void {
    if (this.chart) {
      this.chart.destroy();
    }
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

          this.esgService.getScoreHistory(this.companyId).subscribe(history => {
            this.history = [...history].reverse();
            this.loading = false;
            this.cdr.detectChanges();

            if (this.viewReady) {
              this.renderChart();
            }
          });
        });
      });
    });
  }

  renderChart(): void {
    if (!this.historyChartRef || this.history.length === 0) return;

    if (this.chart) {
      this.chart.destroy();
    }

    const labels = this.history.map(s =>
      new Date(s.timestamp).toLocaleDateString('en-US', { month: 'short', year: '2-digit' })
    );

    this.chart = new Chart(this.historyChartRef.nativeElement, {
      type: 'line',
      data: {
        labels,
        datasets: [
          {
            label: 'Composite',
            data: this.history.map(s => s.compositeScore),
            borderColor: '#f57c00',
            backgroundColor: 'rgba(245, 124, 0, 0.08)',
            borderWidth: 2.5,
            pointRadius: 3,
            tension: 0.3,
            fill: true
          },
          {
            label: 'Environmental',
            data: this.history.map(s => s.environmentalScore),
            borderColor: '#2e7d32',
            backgroundColor: 'transparent',
            borderWidth: 1.5,
            pointRadius: 2,
            tension: 0.3
          },
          {
            label: 'Social',
            data: this.history.map(s => s.socialScore),
            borderColor: '#1565c0',
            backgroundColor: 'transparent',
            borderWidth: 1.5,
            pointRadius: 2,
            tension: 0.3
          },
          {
            label: 'Governance',
            data: this.history.map(s => s.governanceScore),
            borderColor: '#6a1b9a',
            backgroundColor: 'transparent',
            borderWidth: 1.5,
            pointRadius: 2,
            tension: 0.3
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'top',
            labels: { font: { size: 12 }, usePointStyle: true }
          },
          tooltip: {
            mode: 'index',
            intersect: false
          }
        },
        scales: {
          y: {
            min: Math.max(0, Math.floor(Math.min(...this.history.map(s => Math.min(s.environmentalScore, s.socialScore, s.governanceScore, s.compositeScore))) / 10) * 10 - 10),
            max: Math.min(100, Math.ceil(Math.max(...this.history.map(s => Math.max(s.environmentalScore, s.socialScore, s.governanceScore, s.compositeScore))) / 10) * 10 + 10),
            grid: { color: 'rgba(0,0,0,0.05)' }
          },
          x: {
            grid: { display: false }
          }
        }
      }
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