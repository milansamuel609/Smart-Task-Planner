package com.milan.smarttaskplanner.repositories;

import com.milan.smarttaskplanner.entities.Task;
import com.milan.smarttaskplanner.entities.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByGoalId(Long goalId);
    List<Task> findByGoalIdAndStatus(Long goalId, TaskStatus status);
    List<Task> findByGoalIdOrderByOrderIndexAsc(Long goalId);
}
