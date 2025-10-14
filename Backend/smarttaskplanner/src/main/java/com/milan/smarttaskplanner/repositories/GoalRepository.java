package com.milan.smarttaskplanner.repositories;

import com.milan.smarttaskplanner.entities.Goal;
import com.milan.smarttaskplanner.entities.GoalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findByStatus(GoalStatus status);
    List<Goal> findByStatusOrderByCreatedAtDesc(GoalStatus status);
    List<Goal> findTop10ByOrderByCreatedAtDesc();
}
