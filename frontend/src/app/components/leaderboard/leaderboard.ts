import { Component, OnInit, Output, EventEmitter, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EsgService, Company, ESGScorecard } from '../../services/esg';

interface RankedRow {
  company: Company;
  scorecard: ESGScorecard;
}

@Component({
  selector: 'app-leaderboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './leaderboard.html',
  styleUrl: './leaderboard.css'
})
export class LeaderboardComponent implements OnInit {

  @Output() companySelected = new EventEmitter<string>();

  rankedRows: RankedRow[] = [];
  loading = true;
  sortKey: keyof ESGScorecard = 'compositeScore';

  constructor(private esgService: EsgService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.loadLeaderboard();
  }

  loadLeaderboard(): void {
    this.loading = true;

    this.esgService.getCompanies().subscribe(companies => {
      this.esgService.getLeaderboard().subscribe(scorecards => {
        const companyMap = new Map(companies.map(c => [c.id, c]));

        this.rankedRows = scorecards
          .filter(s => companyMap.has(s.companyId))
          .map(s => ({ company: companyMap.get(s.companyId)!, scorecard: s }))
          .sort((a, b) => (b.scorecard[this.sortKey] as number) - (a.scorecard[this.sortKey] as number));

        this.loading = false;
        this.cdr.detectChanges();
      });
    });
  }

  onSortChange(event: Event): void {
    this.sortKey = (event.target as HTMLSelectElement).value as keyof ESGScorecard;
    this.rankedRows.sort((a, b) =>
      (b.scorecard[this.sortKey] as number) - (a.scorecard[this.sortKey] as number)
    );
    this.cdr.detectChanges();
  }

  onSelectCompany(id: string): void {
    this.companySelected.emit(id);
  }

  scoreClass(score: number): string {
    if (score >= 75) return 'score-high';
    if (score >= 50) return 'score-mid';
    return 'score-low';
  }
}