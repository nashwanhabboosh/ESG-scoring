import { Component, OnInit, ChangeDetectorRef, ViewChild, ElementRef, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EsgService, Company, ESGScorecard } from '../../services/esg';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-company-comparison',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './company-comparison.html',
  styleUrl: './company-comparison.css'
})
export class CompanyComparisonComponent implements OnInit, OnDestroy {

  @ViewChild('comparisonChart') chartRef!: ElementRef;

  allCompanies: Company[] = [];
  selectedCompanies: Company[] = [];
  scorecards: ESGScorecard[] = [];
  searchQuery = '';
  loading = false;
  chart: Chart | null = null;

  readonly MAX_COMPANIES = 3;

  readonly COLORS = [
    { border: '#1a1a2e', bg: 'rgba(26,26,46,0.7)' },
    { border: '#e65100', bg: 'rgba(230,81,0,0.7)' },
    { border: '#1565c0', bg: 'rgba(21,101,192,0.7)' }
  ];

  constructor(private esgService: EsgService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.esgService.getCompanies().subscribe(companies => {
      this.allCompanies = companies;
      this.cdr.detectChanges();
    });
  }

  ngOnDestroy(): void {
    if (this.chart) this.chart.destroy();
  }

  get filteredCompanies(): Company[] {
    const q = this.searchQuery.toLowerCase();
    return this.allCompanies.filter(c =>
      !this.selectedCompanies.find(s => s.id === c.id) &&
      (c.name.toLowerCase().includes(q) || c.ticker.toLowerCase().includes(q))
    );
  }

  selectCompany(company: Company): void {
    if (this.selectedCompanies.length >= this.MAX_COMPANIES) return;
    this.selectedCompanies = [...this.selectedCompanies, company];
    this.searchQuery = '';
    this.cdr.detectChanges();
    if (this.selectedCompanies.length >= 2) {
      this.loadComparison();
    }
  }

  removeCompany(company: Company): void {
    this.selectedCompanies = this.selectedCompanies.filter(c => c.id !== company.id);
    this.scorecards = [];
    if (this.chart) { this.chart.destroy(); this.chart = null; }
    if (this.selectedCompanies.length >= 2) {
      this.loadComparison();
    }
    this.cdr.detectChanges();
  }

  loadComparison(): void {
    this.loading = true;
    const ids = this.selectedCompanies.map(c => c.id);
    this.esgService.compareCompanies(ids).subscribe(scorecards => {
      this.scorecards = scorecards;
      this.loading = false;
      this.cdr.detectChanges();
      setTimeout(() => this.renderChart(), 50);
    });
  }

  getScorecardForCompany(companyId: string): ESGScorecard | null {
    return this.scorecards.find(s => s.companyId === companyId) || null;
  }

  renderChart(): void {
    if (!this.chartRef || this.scorecards.length < 2) return;
    if (this.chart) this.chart.destroy();

    const labels = ['Environmental', 'Social', 'Governance', 'Composite'];

    const datasets = this.selectedCompanies.map((company, i) => {
      const sc = this.getScorecardForCompany(company.id);
      return {
        label: company.ticker,
        data: sc ? [
          sc.environmentalScore,
          sc.socialScore,
          sc.governanceScore,
          sc.compositeScore
        ] : [0, 0, 0, 0],
        backgroundColor: this.COLORS[i].bg,
        borderColor: this.COLORS[i].border,
        borderWidth: 2,
        borderRadius: 4
      };
    });

    this.chart = new Chart(this.chartRef.nativeElement, {
      type: 'bar',
      data: { labels, datasets },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'top',
            labels: { font: { size: 13 }, usePointStyle: true }
          },
          tooltip: { mode: 'index', intersect: false }
        },
        scales: {
          y: {
            min: 0,
            max: 100,
            ticks: { stepSize: 20 },
            grid: { color: 'rgba(0,0,0,0.05)' }
          },
          x: { grid: { display: false } }
        }
      }
    });
  }

  scoreClass(score: number): string {
    if (score >= 75) return 'score-high';
    if (score >= 50) return 'score-mid';
    return 'score-low';
  }

  colorForIndex(i: number): string {
    return this.COLORS[i].border;
  }
}