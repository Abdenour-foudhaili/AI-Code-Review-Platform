package com.aicodereview.controller;
import com.aicodereview.dto.CreateReviewRequest;
import com.aicodereview.dto.ReviewDetailsResponse;
import com.aicodereview.dto.ReviewResponse;
import com.aicodereview.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse createReview(@Valid @RequestBody CreateReviewRequest request) {
        return reviewService.createReview(request);
    }

    @GetMapping
    public List<ReviewResponse> getAllReviews() {
        return reviewService.getAllReviews();
    }

    @GetMapping("/{id}")
    public ReviewDetailsResponse getReview(@PathVariable Long id) {
        return reviewService.getReviewDetails(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
    }

    @PostMapping("/{id}/analyze")
    public ReviewResponse analyzeReview(@PathVariable Long id) {
        return reviewService.analyzeReview(id);
    }
}
