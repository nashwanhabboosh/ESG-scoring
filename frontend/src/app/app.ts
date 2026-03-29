import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LeaderboardComponent } from './components/leaderboard/leaderboard';
import { CompanyDetailComponent } from './components/company-detail/company-detail';
import { MethodologySwitcherComponent } from './components/methodology-switcher/methodology-switcher';

type View = 'leaderboard' | 'company' | 'methodology';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, LeaderboardComponent, CompanyDetailComponent, MethodologySwitcherComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  currentView: View = 'leaderboard';
  selectedCompanyId: string | null = null;

  navigateTo(view: View): void {
    this.currentView = view;
    this.selectedCompanyId = null;
  }

  onCompanySelected(companyId: string): void {
    this.selectedCompanyId = companyId;
    this.currentView = 'company';
  }

  onBack(): void {
    this.currentView = 'leaderboard';
    this.selectedCompanyId = null;
  }

  onMethodologyChanged(): void {
    if (this.currentView === 'leaderboard') {
      this.currentView = 'leaderboard';
    }
  }
}