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
public class GoalResponse {
    private Long id;
    private String description;
    private LocalDateTime targetDate;
    private String status;

    @Builder.Default
    private List<TaskResponse> tasks = new ArrayList<>();

    private String aiAnalysis;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
