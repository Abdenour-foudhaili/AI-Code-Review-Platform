package com.aicodereview.service;
import com.aicodereview.ai.AIReviewService;
import com.aicodereview.dto.CreateReviewRequest;
import com.aicodereview.dto.ReviewDetailsResponse;
import com.aicodereview.dto.ReviewResponse;
import com.aicodereview.entity.CodeReview;
import com.aicodereview.entity.ReviewStatus;
import com.aicodereview.exception.ResourceNotFoundException;
import com.aicodereview.mapper.ReviewMapper;
import com.aicodereview.repository.CodeReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewService {
    
    private final CodeReviewRepository repository;
    private final ReviewMapper mapper;
    private final AIReviewService aiService;

    public ReviewService(CodeReviewRepository repository, ReviewMapper mapper, AIReviewService aiService) {
        this.repository = repository;
        this.mapper = mapper;
        this.aiService = aiService;
    }

    @Transactional
    public ReviewResponse createReview(CreateReviewRequest request) {
        CodeReview review = CodeReview.builder()
                .projectName(request.getProjectName())
                .language(request.getLanguage())
                .sourceCode(request.getSourceCode())
                .reviewType(request.getReviewType())
                .status(ReviewStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        return mapper.toResponse(repository.save(review));
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getAllReviews() {
        return repository.findAll().stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReviewDetailsResponse getReviewDetails(Long id) {
        CodeReview review = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));
        return mapper.toDetailsResponse(review);
    }

    @Transactional
    public void deleteReview(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Review not found with id: " + id);
        }
        repository.deleteById(id);
    }

    @Transactional
    public ReviewResponse analyzeReview(Long id) {
        CodeReview review = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));
        
        if (review.getStatus() == ReviewStatus.ANALYZING || review.getStatus() == ReviewStatus.COMPLETED) {
            throw new IllegalArgumentException("Review is already analyzed or in progress.");
        }

        review.setStatus(ReviewStatus.ANALYZING);
        repository.save(review);
        
        // In a real app, this should be async (e.g. using @Async or message queue)
        new Thread(() -> aiService.performReview(review)).start();
        
        return mapper.toResponse(review);
    }
}
