import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ReviewResponse, ReviewDetailsResponse, CreateReviewRequest } from '../models/review.model';

@Injectable({ providedIn: 'root' })
export class ReviewService {
  private apiUrl = `${environment.apiUrl}/reviews`;

  constructor(private http: HttpClient) {}

  getReviews(): Observable<ReviewResponse[]> {
    return this.http.get<ReviewResponse[]>(this.apiUrl);
  }

  getReviewById(id: number): Observable<ReviewDetailsResponse> {
    return this.http.get<ReviewDetailsResponse>(`${this.apiUrl}/${id}`);
  }

  createReview(request: CreateReviewRequest): Observable<ReviewResponse> {
    return this.http.post<ReviewResponse>(this.apiUrl, request);
  }

  deleteReview(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  analyzeReview(id: number): Observable<ReviewResponse> {
    return this.http.post<ReviewResponse>(`${this.apiUrl}/${id}/analyze`, {});
  }
}
