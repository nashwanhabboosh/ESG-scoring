import { Component, OnInit, ChangeDetectorRef, ViewChild, ElementRef, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EsgService, SectorSummary } from '../../services/esg';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-sector-analytics',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './sector-analytics.html',
  styleUrl: './sector-analytics.css'
})
export class SectorAnalyticsComponent implements OnInit, OnDestroy {

  @ViewChild('sectorChart') sectorChartRef!: ElementRef;

  sectors: SectorSummary[] = [];
  selectedSector: SectorSummary | null = null;
  loading = true;
  detailLoading = false;
  chart: Chart | null = null;

  constructor(private esgService: EsgService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.esgService.getSectors().subscribe(sectors => {
      this.sectors = sectors;
      this.loading = false;
      this.cdr.detectChanges();
    });
  }

  ngOnDestroy(): void {
    if (this.chart) this.chart.destroy();
  }

  selectSector(sector: SectorSummary): void {
    this.selectedSector = sector;
    this.detailLoading = false;
    this.cdr.detectChanges();
    setTimeout(() => this.renderChart(), 50);
  }

  renderChart(): void {
    if (!this.sectorChartRef || !this.selectedSector) return;
    if (this.chart) this.chart.destroy();

    const s = this.selectedSector;
    this.chart = new Chart(this.sectorChartRef.nativeElement, {
      type: 'doughnut',
      data: {
        labels: ['Environmental', 'Social', 'Governance'],
        datasets: [{
          data: [s.avgEnvironmentalScore, s.avgSocialScore, s.avgGovernanceScore],
          backgroundColor: ['#2e7d32', '#1565c0', '#6a1b9a'],
          borderColor: '#ffffff',
          borderWidth: 3,
          hoverOffset: 8
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
            labels: { font: { size: 13 }, usePointStyle: true, padding: 16 }
          },
          tooltip: {
            callbacks: {
              label: (ctx) => ` ${ctx.label}: ${ctx.raw}`
            }
          }
        },
        cutout: '60%'
      }
    });
  }

  scoreClass(score: number): string {
    if (score >= 75) return 'score-high';
    if (score >= 50) return 'score-mid';
    return 'score-low';
  }
}