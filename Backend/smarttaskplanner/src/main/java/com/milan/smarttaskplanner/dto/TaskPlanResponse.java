package com.milan.smarttaskplanner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskPlanResponse {
    private String goalAnalysis;
    private Integer totalTasks;
    private Integer estimatedTotalHours;
    private LocalDateTime suggestedStartDate;
    private LocalDateTime suggestedEndDate;

    @Builder.Default
    private List<TaskResponse> tasks = new ArrayList<>();

    @Builder.Default
    private List<String> recommendations = new ArrayList<>();

    @Builder.Default
    private List<String> risks = new ArrayList<>();
}
