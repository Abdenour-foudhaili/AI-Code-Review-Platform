package com.aicodereview.repository;
import com.aicodereview.entity.ReviewFinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewFindingRepository extends JpaRepository<ReviewFinding, Long> {
}
