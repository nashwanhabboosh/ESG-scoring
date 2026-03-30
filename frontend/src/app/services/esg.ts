import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Company {
  id: string;
  name: string;
  ticker: string;
  sector: string;
  country: string;
}

export interface ESGScorecard {
  id: string;
  companyId: string;
  methodologyId: string;
  environmentalScore: number;
  socialScore: number;
  governanceScore: number;
  compositeScore: number;
  timestamp: string;
}

export interface CompanyScore {
  companyId: string;
  name: string;
  ticker: string;
  environmentalScore: number;
  socialScore: number;
  governanceScore: number;
  compositeScore: number;
}

export interface SectorSummary {
  sector: string;
  avgEnvironmentalScore: number;
  avgSocialScore: number;
  avgGovernanceScore: number;
  avgCompositeScore: number;
  companyCount: number;
  companies: CompanyScore[];
}

export interface ScoringMethodology {
  id: string;
  name: string;
  description: string;
  active: boolean;
  environmentalWeight: number;
  socialWeight: number;
  governanceWeight: number;
}

export interface RawMetric {
  id: string;
  companyId: string;
  metricName: string;
  value: number;
  year: number;
}

@Injectable({
  providedIn: 'root'
})
export class EsgService {

  private baseUrl = environment.apiUrl;
  private authHeaders = new HttpHeaders({
    Authorization: 'Basic ' + btoa('admin:esg-admin-2024')
  });

  constructor(private http: HttpClient) {}

  // Companies
  getCompanies(): Observable<Company[]> {
    return this.http.get<Company[]>(`${this.baseUrl}/companies`);
  }

  getCompany(id: string): Observable<Company> {
    return this.http.get<Company>(`${this.baseUrl}/companies/${id}`);
  }

  // Sectors
  getSectors(): Observable<SectorSummary[]> {
    return this.http.get<SectorSummary[]>(`${this.baseUrl}/sectors`);
  }

  getSectorDetail(sector: string): Observable<SectorSummary> {
    return this.http.get<SectorSummary>(`${this.baseUrl}/sectors/${encodeURIComponent(sector)}`);
  }

  // Scores
  getLeaderboard(): Observable<ESGScorecard[]> {
    return this.http.get<ESGScorecard[]>(`${this.baseUrl}/scores/leaderboard`);
  }

  getLatestScore(companyId: string): Observable<ESGScorecard> {
    return this.http.get<ESGScorecard>(`${this.baseUrl}/scores/company/${companyId}`);
  }

  getScoreHistory(companyId: string): Observable<ESGScorecard[]> {
    return this.http.get<ESGScorecard[]>(`${this.baseUrl}/scores/company/${companyId}/history`);
  }

  scoreAllCompanies(): Observable<ESGScorecard[]> {
    return this.http.post<ESGScorecard[]>(`${this.baseUrl}/scores/score-all`, {}, { headers: this.authHeaders });
  }

  scoreCompany(companyId: string): Observable<ESGScorecard> {
    return this.http.post<ESGScorecard>(`${this.baseUrl}/scores/company/${companyId}`, {}, { headers: this.authHeaders });
  }

  // Methodologies
  getMethodologies(): Observable<ScoringMethodology[]> {
    return this.http.get<ScoringMethodology[]>(`${this.baseUrl}/methodologies`);
  }

  activateMethodology(id: string): Observable<ScoringMethodology> {
    return this.http.put<ScoringMethodology>(`${this.baseUrl}/methodologies/${id}/activate`, {}, { headers: this.authHeaders });
  }

  createMethodology(methodology: ScoringMethodology): Observable<ScoringMethodology> {
  return this.http.post<ScoringMethodology>(`${this.baseUrl}/methodologies`, methodology, { headers: this.authHeaders });
}

  // Metrics
  getMetrics(companyId: string): Observable<RawMetric[]> {
    return this.http.get<RawMetric[]>(`${this.baseUrl}/metrics/company/${companyId}`);
  }
}