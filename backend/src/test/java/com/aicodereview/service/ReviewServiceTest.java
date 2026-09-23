package com.aicodereview.service;
import com.aicodereview.ai.AIReviewService;
import com.aicodereview.dto.CreateReviewRequest;
import com.aicodereview.dto.ReviewDetailsResponse;
import com.aicodereview.dto.ReviewResponse;
import com.aicodereview.entity.CodeReview;
import com.aicodereview.entity.ReviewStatus;
import com.aicodereview.entity.ReviewType;
import com.aicodereview.exception.ResourceNotFoundException;
import com.aicodereview.mapper.ReviewMapper;
import com.aicodereview.repository.CodeReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReviewServiceTest {

    @Mock
    private CodeReviewRepository repository;
    @Mock
    private ReviewMapper mapper;
    @Mock
    private AIReviewService aiService;

    @InjectMocks
    private ReviewService service;

    private CodeReview mockReview;

    @BeforeEach
    void setUp() {
        mockReview = CodeReview.builder()
                .id(1L)
                .projectName("Test Project")
                .language("Java")
                .sourceCode("public class Test {}")
                .reviewType(ReviewType.CODE_QUALITY)
                .status(ReviewStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createReview_success() {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setProjectName("Test Project");
        request.setLanguage("Java");
        request.setSourceCode("public class Test {}");
        request.setReviewType(ReviewType.CODE_QUALITY);

        ReviewResponse response = new ReviewResponse();
        response.setId(1L);

        when(repository.save(any(CodeReview.class))).thenReturn(mockReview);
        when(mapper.toResponse(any(CodeReview.class))).thenReturn(response);

        ReviewResponse result = service.createReview(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(repository, times(1)).save(any(CodeReview.class));
    }

    @Test
    void getAllReviews_success() {
        ReviewResponse response = new ReviewResponse();
        response.setId(1L);

        when(repository.findAll()).thenReturn(List.of(mockReview));
        when(mapper.toResponse(any(CodeReview.class))).thenReturn(response);

        List<ReviewResponse> results = service.getAllReviews();

        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
    }

    @Test
    void getReviewDetails_success() {
        ReviewDetailsResponse response = new ReviewDetailsResponse();
        response.setId(1L);

        when(repository.findById(1L)).thenReturn(Optional.of(mockReview));
        when(mapper.toDetailsResponse(any(CodeReview.class))).thenReturn(response);

        ReviewDetailsResponse result = service.getReviewDetails(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getReviewDetails_notFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getReviewDetails(1L));
    }

    @Test
    void deleteReview_success() {
        when(repository.existsById(1L)).thenReturn(true);
        service.deleteReview(1L);
        verify(repository, times(1)).deleteById(1L);
    }

    @Test
    void deleteReview_notFound() {
        when(repository.existsById(1L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> service.deleteReview(1L));
    }

    @Test
    void analyzeReview_success() {
        ReviewResponse response = new ReviewResponse();
        response.setId(1L);

        when(repository.findById(1L)).thenReturn(Optional.of(mockReview));
        when(mapper.toResponse(any(CodeReview.class))).thenReturn(response);
        when(repository.save(any(CodeReview.class))).thenReturn(mockReview);

        ReviewResponse result = service.analyzeReview(1L);

        assertNotNull(result);
        assertEquals(ReviewStatus.ANALYZING, mockReview.getStatus());
        verify(aiService, timeout(1000).times(1)).performReview(mockReview);
    }

    @Test
    void analyzeReview_alreadyInProgress() {
        mockReview.setStatus(ReviewStatus.ANALYZING);
        when(repository.findById(1L)).thenReturn(Optional.of(mockReview));
        assertThrows(IllegalArgumentException.class, () -> service.analyzeReview(1L));
    }
}
