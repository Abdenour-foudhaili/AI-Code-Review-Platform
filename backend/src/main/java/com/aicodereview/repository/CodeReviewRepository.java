package com.aicodereview.repository;
import com.aicodereview.entity.CodeReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CodeReviewRepository extends JpaRepository<CodeReview, Long> {
    List<CodeReview> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    Optional<CodeReview> findByIdAndOwnerId(Long id, Long ownerId);
}
