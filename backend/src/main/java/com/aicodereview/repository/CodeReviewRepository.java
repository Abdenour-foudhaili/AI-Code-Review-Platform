package com.aicodereview.repository;
import com.aicodereview.entity.CodeReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CodeReviewRepository extends JpaRepository<CodeReview, Long> {
}
