package com.aicodereview.repository;

import com.aicodereview.entity.ReviewRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRuleRepository extends JpaRepository<ReviewRule, Long> {
    List<ReviewRule> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    List<ReviewRule> findByOwnerIdAndEnabledTrue(Long ownerId);
    Optional<ReviewRule> findByIdAndOwnerId(Long id, Long ownerId);
}
