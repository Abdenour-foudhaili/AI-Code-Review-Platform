package com.aicodereview.ai;
import com.aicodereview.entity.CodeReview;
import com.aicodereview.entity.ReviewStatus;
import com.aicodereview.repository.CodeReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MockAIProviderTest {

    @Mock
    private CodeReviewRepository repository;

    @InjectMocks
    private MockAIProvider aiProvider;

    @Test
    void performReview_success() {
        CodeReview review = new CodeReview();
        review.setId(1L);
        review.setFindings(new ArrayList<>());
        
        when(repository.findById(1L)).thenReturn(java.util.Optional.of(review));
        
        aiProvider.performReview(review);

        assertEquals(ReviewStatus.COMPLETED, review.getStatus());
        assertNotNull(review.getSummary());
        assertFalse(review.getFindings().isEmpty());
        assertNotNull(review.getCompletedAt());
        
        verify(repository, times(1)).save(review);
    }
}
