package com.milan.smarttaskplanner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTaskStatusRequest {
    @NotNull(message = "Task ID is required.")
    private Long taskId;

    @NotBlank(message = "Status is required.")
    private String status; // e.g., PENDING, IN_PROGRESS, COMPLETED, BLOCKED
}
