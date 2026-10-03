package com.aicodereview.controller;
import com.aicodereview.dto.CreateReviewRequest;
import com.aicodereview.dto.ReviewDetailsResponse;
import com.aicodereview.dto.ReviewResponse;
import com.aicodereview.mapper.ReviewMapper;
import com.aicodereview.security.AuthenticatedUser;
import com.aicodereview.service.OwnershipService;
import com.aicodereview.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** Single-file code reviews (existing feature) - now authenticated and isolated per user. */
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final OwnershipService ownership;
    private final ReviewMapper mapper;

    public ReviewController(ReviewService reviewService, OwnershipService ownership, ReviewMapper mapper) {
        this.reviewService = reviewService;
        this.ownership = ownership;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse createReview(@Valid @RequestBody CreateReviewRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        ReviewResponse created = reviewService.createReview(request);
        ownership.assignReviewOwner(created.getId(), user.id());
        return created;
    }

    @GetMapping
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<ReviewResponse> getAllReviews(@AuthenticationPrincipal AuthenticatedUser user) {
        return ownership.listReviews(user.id()).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ReviewDetailsResponse getReview(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        ownership.requireReview(id, user.id());
        return reviewService.getReviewDetails(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReview(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        ownership.requireReview(id, user.id());
        reviewService.deleteReview(id);
    }

    @PostMapping("/{id}/analyze")
    public ReviewResponse analyzeReview(@PathVariable Long id,
                                        @RequestParam(value = "model", required = false) String modelId,
                                        @AuthenticationPrincipal AuthenticatedUser user) {
        ownership.requireReview(id, user.id());
        return reviewService.analyzeReview(id, modelId);
    }
}
