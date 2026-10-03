package com.aicodereview.repository;

import com.aicodereview.entity.GithubConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface GithubConnectionRepository extends JpaRepository<GithubConnection, Long> {
    Optional<GithubConnection> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}
