package com.aicodereview.repository;
import com.aicodereview.entity.ReviewFinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewFindingRepository extends JpaRepository<ReviewFinding, Long> {
    List<ReviewFinding> findByProjectIdOrderBySeverityAscFileAscLineNumberAsc(Long projectId);
}
