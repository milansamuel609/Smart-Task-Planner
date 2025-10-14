package com.milan.smarttaskplanner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private String detailedDescription;  // NEW: Comprehensive description
    private List<String> steps;          // NEW: Step-by-step guide
    private Integer estimatedDurationHours;
    private String priority;
    private String status;
    private Integer orderIndex;
    private List<Long> dependencies;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}