package com.milan.smarttaskplanner.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "tasks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String detailedDescription;

    @ElementCollection
    @CollectionTable(name = "task_steps", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "step", columnDefinition = "TEXT")
    private List<String> steps;

    private Integer estimatedDurationHours;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "task_priority")
    private TaskPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "task_status")
    private TaskStatus status;

    private Integer orderIndex;

    @ElementCollection
    @CollectionTable(name = "task_dependencies", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "dependency_id")
    private List<Long> dependencies;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_id", nullable = false)
    private Goal goal;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
