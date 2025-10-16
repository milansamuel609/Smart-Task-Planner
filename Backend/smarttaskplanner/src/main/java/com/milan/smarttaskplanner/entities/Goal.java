package com.milan.smarttaskplanner.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "goals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(builderClassName = "GoalBuilder")
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "target_date")
    private LocalDateTime targetDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private GoalStatus status;

    @OneToMany(mappedBy = "goal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> tasks;

    @Column(name = "ai_analysis", columnDefinition = "TEXT")
    private String aiAnalysis;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Custom builder with default empty list for tasks
    public static class GoalBuilder {
        private List<Task> tasks = new ArrayList<>();

        public GoalBuilder tasks(List<Task> tasks) {
            this.tasks = (tasks == null) ? new ArrayList<>() : tasks;
            return this;
        }
    }

    // Helper methods
    public void addTask(Task task) {
        if (tasks == null) tasks = new ArrayList<>();
        tasks.add(task);
        task.setGoal(this);
    }

    public void removeTask(Task task) {
        if (tasks != null) {
            tasks.remove(task);
        }
        task.setGoal(null);
    }
}
