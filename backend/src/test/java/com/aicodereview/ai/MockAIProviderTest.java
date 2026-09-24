package com.aicodereview.ai;

import com.aicodereview.entity.*;
import com.aicodereview.repository.CodeReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MockAIProviderTest {

    @Mock
    private CodeReviewRepository repository;

    @InjectMocks
    private MockAIProvider aiProvider;

    @Test
    void performReview_fullReview_success() {
        CodeReview review = new CodeReview();
        review.setId(1L);
        review.setSourceCode("public class Test { public void doSomething() { Object o = null; o.toString(); } }");
        review.setReviewType(ReviewType.FULL_REVIEW);
        review.setFindings(new ArrayList<>());
        
        when(repository.findById(1L)).thenReturn(Optional.of(review));
        
        aiProvider.performReview(review);

        assertEquals(ReviewStatus.COMPLETED, review.getStatus());
        assertNotNull(review.getSummary());
        assertTrue(review.getFindings().size() > 0);
        assertNotNull(review.getCompletedAt());
        verify(repository, times(1)).save(review);
    }
    
    @Test
    void performReview_securityReview_success() {
        CodeReview review = new CodeReview();
        review.setId(2L);
        review.setSourceCode("SELECT * FROM users WHERE name = test");
        review.setReviewType(ReviewType.SECURITY);
        review.setFindings(new ArrayList<>());
        
        when(repository.findById(2L)).thenReturn(Optional.of(review));
        
        aiProvider.performReview(review);

        assertEquals(ReviewStatus.COMPLETED, review.getStatus());
        assertTrue(review.getFindings().stream().allMatch(f -> f.getCategory() == FindingCategory.SECURITY));
    }
    
    @Test
    void performReview_bugDetection_success() {
        CodeReview review = new CodeReview();
        review.setId(3L);
        review.setSourceCode("public void test() { Object user = null; user.toString(); }");
        review.setReviewType(ReviewType.BUG_DETECTION);
        review.setFindings(new ArrayList<>());
        
        when(repository.findById(3L)).thenReturn(Optional.of(review));
        
        aiProvider.performReview(review);

        assertEquals(ReviewStatus.COMPLETED, review.getStatus());
        assertTrue(review.getFindings().stream().allMatch(f -> f.getCategory() == FindingCategory.BUG));
    }
    
    @Test
    void performReview_emptySourceCode_success() {
        CodeReview review = new CodeReview();
        review.setId(4L);
        review.setSourceCode("");
        review.setReviewType(ReviewType.CODE_QUALITY);
        review.setFindings(new ArrayList<>());
        
        when(repository.findById(4L)).thenReturn(Optional.of(review));
        
        aiProvider.performReview(review);

        assertEquals(ReviewStatus.COMPLETED, review.getStatus());
        assertFalse(review.getFindings().isEmpty());
    }
    
    @Test
    void performReview_notFound_doesNotThrow() {
        CodeReview review = new CodeReview();
        review.setId(5L);
        
        when(repository.findById(5L)).thenReturn(Optional.empty());
        
        assertDoesNotThrow(() -> aiProvider.performReview(review));
        verify(repository, never()).save(any());
    }
}
