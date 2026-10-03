package com.aicodereview.repository;
import com.aicodereview.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    Optional<Project> findByIdAndOwnerId(Long id, Long ownerId);
    List<Project> findByOwnerIdAndSourceTypeOrderByCreatedAtDesc(Long ownerId, String sourceType);
    long countByOwnerId(Long ownerId);

    @Query("select count(f) from ReviewFinding f where f.project.owner.id = :ownerId and f.severity = com.aicodereview.entity.FindingSeverity.CRITICAL")
    long countCriticalFindings(Long ownerId);

    @Query("select count(f) from ReviewFinding f where f.project.owner.id = :ownerId and f.category = com.aicodereview.entity.FindingCategory.SECURITY")
    long countSecurityFindings(Long ownerId);

    @Query("select count(f) from ReviewFinding f where f.project.id = :projectId")
    long countFindings(Long projectId);
}
