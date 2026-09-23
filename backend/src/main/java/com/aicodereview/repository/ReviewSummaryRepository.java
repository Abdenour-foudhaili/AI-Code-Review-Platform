package com.aicodereview.repository;
import com.aicodereview.entity.ReviewSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewSummaryRepository extends JpaRepository<ReviewSummary, Long> {
}
