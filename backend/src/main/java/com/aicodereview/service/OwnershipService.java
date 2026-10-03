package com.aicodereview.service;

import com.aicodereview.entity.CodeReview;
import com.aicodereview.entity.Project;
import com.aicodereview.entity.User;
import com.aicodereview.exception.ResourceNotFoundException;
import com.aicodereview.repository.CodeReviewRepository;
import com.aicodereview.repository.ProjectRepository;
import com.aicodereview.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Resource isolation: every project / review belongs to a user and is only visible to that user.
 * Missing and foreign resources both answer 404 (no information leak). Legacy rows without owner are visible to nobody.
 */
@Service
public class OwnershipService {
    private final ProjectRepository projects;
    private final CodeReviewRepository reviews;
    private final UserRepository users;

    public OwnershipService(ProjectRepository projects, CodeReviewRepository reviews, UserRepository users) {
        this.projects = projects;
        this.reviews = reviews;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public Project requireProject(Long projectId, Long userId) {
        return projects.findByIdAndOwnerId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));
    }

    @Transactional(readOnly = true)
    public void requireProjectAccess(Long projectId, Long userId) { requireProject(projectId, userId); }

    @Transactional(readOnly = true)
    public CodeReview requireReview(Long reviewId, Long userId) {
        return reviews.findByIdAndOwnerId(reviewId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));
    }

    @Transactional
    public void assignProjectOwner(Long projectId, Long userId) {
        Project p = projects.findById(projectId).orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));
        User u = users.getReferenceById(userId);
        p.setOwner(u);
        if (p.getSourceType() == null) p.setSourceType("ZIP");
        projects.save(p);
    }

    @Transactional
    public void assignReviewOwner(Long reviewId, Long userId) {
        CodeReview r = reviews.findById(reviewId).orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));
        r.setOwner(users.getReferenceById(userId));
        reviews.save(r);
    }

    @Transactional(readOnly = true)
    public List<Project> listProjects(Long userId) { return projects.findByOwnerIdOrderByCreatedAtDesc(userId); }

    @Transactional(readOnly = true)
    public List<CodeReview> listReviews(Long userId) { return reviews.findByOwnerIdOrderByCreatedAtDesc(userId); }
}
