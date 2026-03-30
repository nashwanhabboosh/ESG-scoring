import { Component, OnInit, ChangeDetectorRef, ViewChild, ElementRef, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EsgService, Company, ESGScorecard } from '../../services/esg';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

interface PortfolioHolding {
  company: Company;
  weight: number;
  scorecard: ESGScorecard | null;
  history: ESGScorecard[];
}

interface RiskMetrics {
  annualizedVolatility: number;
  threeMonthMomentum: number;
  sixMonthMomentum: number;
  sharpeProxy: number;
}

@Component({
  selector: 'app-portfolio-analytics',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './portfolio-analytics.html',
  styleUrl: './portfolio-analytics.css'
})
export class PortfolioAnalyticsComponent implements OnInit, OnDestroy {

  @ViewChild('historyChart') historyChartRef!: ElementRef;
  @ViewChild('contributionChart') contributionChartRef!: ElementRef;

  allCompanies: Company[] = [];
  holdings: PortfolioHolding[] = [];
  searchQuery = '';
  loading = false;
  showIndividualLines = false;
  historyChart: Chart | null = null;
  contributionChart: Chart | null = null;

  readonly MAX_HOLDINGS = 5;
  readonly COLORS = ['#1a1a2e', '#e65100', '#1565c0', '#2e7d32', '#6a1b9a'];

  constructor(private esgService: EsgService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.esgService.getCompanies().subscribe(companies => {
      this.allCompanies = companies;
      this.cdr.detectChanges();
    });
  }

  ngOnDestroy(): void {
    if (this.historyChart) this.historyChart.destroy();
    if (this.contributionChart) this.contributionChart.destroy();
  }

  get filteredCompanies(): Company[] {
    const q = this.searchQuery.toLowerCase();
    return this.allCompanies.filter(c =>
      !this.holdings.find(h => h.company.id === c.id) &&
      (c.name.toLowerCase().includes(q) || c.ticker.toLowerCase().includes(q))
    );
  }

  get totalWeight(): number {
    return Math.round(this.holdings.reduce((sum, h) => sum + h.weight, 0));
  }

  get isReady(): boolean {
    return this.holdings.length >= 2 && this.totalWeight === 100 &&
      this.holdings.every(h => h.scorecard !== null && h.history.length > 0);
  }

  get portfolioE(): number {
    return this.round(this.holdings.reduce((sum, h) =>
      sum + (h.scorecard?.environmentalScore ?? 0) * (h.weight / 100), 0));
  }

  get portfolioS(): number {
    return this.round(this.holdings.reduce((sum, h) =>
      sum + (h.scorecard?.socialScore ?? 0) * (h.weight / 100), 0));
  }

  get portfolioG(): number {
    return this.round(this.holdings.reduce((sum, h) =>
      sum + (h.scorecard?.governanceScore ?? 0) * (h.weight / 100), 0));
  }

  get portfolioComposite(): number {
    return this.round(this.holdings.reduce((sum, h) =>
      sum + (h.scorecard?.compositeScore ?? 0) * (h.weight / 100), 0));
  }

  get portfolioTimeSeries(): number[] {
    if (this.holdings.length === 0) return [];
    const minLength = Math.min(...this.holdings.map(h => h.history.length));
    const series: number[] = [];
    for (let i = 0; i < minLength; i++) {
      const score = this.holdings.reduce((sum, h) => {
        const sc = h.history[i];
        return sum + (sc?.compositeScore ?? 0) * (h.weight / 100);
      }, 0);
      series.push(this.round(score));
    }
    return series;
  }

  get portfolioLabels(): string[] {
    if (this.holdings.length === 0 || this.holdings[0].history.length === 0) return [];
    const minLength = Math.min(...this.holdings.map(h => h.history.length));
    return this.holdings[0].history.slice(0, minLength).map(s =>
      new Date(s.timestamp).toLocaleDateString('en-US', { month: 'short', year: '2-digit' })
    );
  }

  get riskMetrics(): RiskMetrics {
    const series = this.portfolioTimeSeries;
    if (series.length < 6) return { annualizedVolatility: 0, threeMonthMomentum: 0, sixMonthMomentum: 0, sharpeProxy: 0 };

    const changes = series.slice(1).map((v, i) => v - series[i]);
    const mean = changes.reduce((a, b) => a + b, 0) / changes.length;
    const variance = changes.reduce((sum, c) => sum + Math.pow(c - mean, 2), 0) / changes.length;
    const monthlyStdDev = Math.sqrt(variance);
    const annualizedVolatility = this.round(monthlyStdDev * Math.sqrt(12));

    const last = series[series.length - 1];
    const threeMonthMomentum = this.round(last - series[series.length - 4]);
    const sixMonthMomentum = this.round(last - series[series.length - 7]);

    const totalReturn = last - series[0];
    const sharpeProxy = annualizedVolatility > 0 ? this.round(totalReturn / annualizedVolatility) : 0;

    return { annualizedVolatility, threeMonthMomentum, sixMonthMomentum, sharpeProxy };
  }

  get holdingVolatilities(): { ticker: string; volatility: number; color: string }[] {
    return this.holdings.map((h, i) => {
      const series = h.history.map(s => s.compositeScore);
      if (series.length < 2) return { ticker: h.company.ticker, volatility: 0, color: this.COLORS[i] };
      const changes = series.slice(1).map((v, j) => v - series[j]);
      const mean = changes.reduce((a, b) => a + b, 0) / changes.length;
      const variance = changes.reduce((sum, c) => sum + Math.pow(c - mean, 2), 0) / changes.length;
      return {
        ticker: h.company.ticker,
        volatility: this.round(Math.sqrt(variance) * Math.sqrt(12)),
        color: this.COLORS[i]
      };
    }).sort((a, b) => b.volatility - a.volatility);
  }

  selectCompany(company: Company): void {
    if (this.holdings.length >= this.MAX_HOLDINGS) return;
    const defaultWeight = Math.floor(100 / (this.holdings.length + 1));
    this.holdings = [...this.holdings, {
      company,
      weight: defaultWeight,
      scorecard: null,
      history: []
    }];
    this.searchQuery = '';
    this.autoNormalize();
    this.loadHoldingData(this.holdings[this.holdings.length - 1]);
    this.cdr.detectChanges();
  }

  removeHolding(holding: PortfolioHolding): void {
    this.holdings = this.holdings.filter(h => h.company.id !== holding.company.id);
    this.autoNormalize();
    this.destroyCharts();
    if (this.isReady) setTimeout(() => this.renderCharts(), 50);
    this.cdr.detectChanges();
  }

  loadHoldingData(holding: PortfolioHolding): void {
    this.loading = true;
    this.esgService.compareCompanies([holding.company.id]).subscribe(scorecards => {
      holding.scorecard = scorecards[0] || null;
      this.esgService.getScoreHistory(holding.company.id).subscribe(history => {
        holding.history = [...history].reverse();
        this.loading = false;
        this.cdr.detectChanges();
        if (this.isReady) setTimeout(() => this.renderCharts(), 50);
      });
    });
  }

  onWeightInput(holding: PortfolioHolding, value: string): void {
    const parsed = parseFloat(value);
    if (!isNaN(parsed)) {
      holding.weight = Math.max(0, Math.min(100, parsed));
      this.destroyCharts();
      if (this.isReady) setTimeout(() => this.renderCharts(), 50);
      this.cdr.detectChanges();
    }
  }

  onSliderChange(holding: PortfolioHolding): void {
    this.destroyCharts();
    if (this.isReady) setTimeout(() => this.renderCharts(), 50);
    this.cdr.detectChanges();
  }

  autoNormalize(): void {
    if (this.holdings.length === 0) return;
    const perCompany = Math.floor(100 / this.holdings.length);
    const remainder = 100 - perCompany * this.holdings.length;
    this.holdings.forEach((h, i) => {
      h.weight = i === 0 ? perCompany + remainder : perCompany;
    });
    this.destroyCharts();
    if (this.isReady) setTimeout(() => this.renderCharts(), 50);
    this.cdr.detectChanges();
  }

  toggleIndividualLines(): void {
    this.showIndividualLines = !this.showIndividualLines;
    if (this.historyChart) { this.historyChart.destroy(); this.historyChart = null; }
    setTimeout(() => this.renderHistoryChart(), 50);
  }

  destroyCharts(): void {
    if (this.historyChart) { this.historyChart.destroy(); this.historyChart = null; }
    if (this.contributionChart) { this.contributionChart.destroy(); this.contributionChart = null; }
  }

  renderCharts(): void {
    this.renderHistoryChart();
    this.renderContributionChart();
  }

  renderHistoryChart(): void {
    if (!this.historyChartRef || !this.isReady) return;
    if (this.historyChart) this.historyChart.destroy();

    const datasets: any[] = [{
      label: 'Portfolio',
      data: this.portfolioTimeSeries,
      borderColor: '#f57c00',
      backgroundColor: 'rgba(245,124,0,0.08)',
      borderWidth: 3,
      pointRadius: 2,
      tension: 0.3,
      fill: true
    }];

    if (this.showIndividualLines) {
      this.holdings.forEach((h, i) => {
        const minLength = Math.min(...this.holdings.map(h => h.history.length));
        datasets.push({
          label: h.company.ticker,
          data: h.history.slice(0, minLength).map(s => s.compositeScore),
          borderColor: this.COLORS[i],
          backgroundColor: 'transparent',
          borderWidth: 1.5,
          pointRadius: 1,
          tension: 0.3,
          borderDash: [4, 4]
        });
      });
    }

    this.historyChart = new Chart(this.historyChartRef.nativeElement, {
      type: 'line',
      data: { labels: this.portfolioLabels, datasets },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'top', labels: { usePointStyle: true, font: { size: 12 } } },
          tooltip: { mode: 'index', intersect: false }
        },
        scales: {
          y: (() => {
            const allValues = this.showIndividualLines
                ? this.holdings.flatMap(h => h.history.map(s => s.compositeScore)).concat(this.portfolioTimeSeries)
                : this.portfolioTimeSeries;
            const min = Math.max(0, Math.floor(Math.min(...allValues) / 10) * 10 - 10);
            const max = Math.min(100, Math.ceil(Math.max(...allValues) / 10) * 10 + 10);
            return { min, max, grid: { color: 'rgba(0,0,0,0.05)' } };
            })(),
          x: { grid: { display: false } }
        }
      }
    });
  }

  renderContributionChart(): void {
    if (!this.contributionChartRef || !this.isReady) return;
    if (this.contributionChart) this.contributionChart.destroy();

    this.contributionChart = new Chart(this.contributionChartRef.nativeElement, {
      type: 'doughnut',
      data: {
        labels: this.holdings.map(h => h.company.ticker),
        datasets: [{
          data: this.holdings.map(h =>
            this.round((h.scorecard?.compositeScore ?? 0) * (h.weight / 100))
          ),
          backgroundColor: this.COLORS.slice(0, this.holdings.length),
          borderColor: '#ffffff',
          borderWidth: 3,
          hoverOffset: 8
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'bottom', labels: { usePointStyle: true, font: { size: 12 } } },
          tooltip: {
            callbacks: {
              label: (ctx) => ` ${ctx.label}: ${ctx.raw} pts (weighted)`
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

  momentumClass(val: number): string {
    if (val > 0) return 'score-high';
    if (val < 0) return 'score-low';
    return 'score-mid';
  }

  round(value: number): number {
    return Math.round(value * 100) / 100;
  }
}