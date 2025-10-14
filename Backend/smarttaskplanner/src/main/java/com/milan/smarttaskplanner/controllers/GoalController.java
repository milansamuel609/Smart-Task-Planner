package com.milan.smarttaskplanner.controllers;

import com.milan.smarttaskplanner.dto.GoalRequest;
import com.milan.smarttaskplanner.dto.GoalResponse;
import com.milan.smarttaskplanner.dto.TaskPlanResponse;
import com.milan.smarttaskplanner.dto.TaskResponse;
import com.milan.smarttaskplanner.dto.UpdateTaskStatusRequest;
import com.milan.smarttaskplanner.services.GoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Goal Management", description = "APIs for managing goals and task plans")
@CrossOrigin(origins = "*")
public class GoalController {

    private final GoalService goalService;

    @PostMapping
    @Operation(summary = "Create a new goal and generate task plan",
            description = "Accepts a goal description and uses AI to break it down into actionable tasks")
    public ResponseEntity<TaskPlanResponse> createGoal(@Valid @RequestBody GoalRequest request) {
        log.info("Received request to create goal: {}", request.getDescription());

        try {
            TaskPlanResponse response = goalService.createGoalWithTasks(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating goal", e);
            throw new RuntimeException("Failed to create goal: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get goal by ID", description = "Retrieves a specific goal with all its tasks")
    public ResponseEntity<GoalResponse> getGoal(@PathVariable Long id) {
        GoalResponse response = goalService.getGoal(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all goals", description = "Retrieves all goals")
    public ResponseEntity<List<GoalResponse>> getAllGoals() {
        List<GoalResponse> goals = goalService.getAllGoals();
        return ResponseEntity.ok(goals);
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent goals", description = "Retrieves the 10 most recent goals")
    public ResponseEntity<List<GoalResponse>> getRecentGoals() {
        List<GoalResponse> goals = goalService.getRecentGoals();
        return ResponseEntity.ok(goals);
    }

    @PutMapping("/{goalId}/tasks/status")
    @Operation(summary = "Update task status", description = "Updates the status of a specific task")
    public ResponseEntity<TaskResponse> updateTaskStatus(
            @PathVariable Long goalId,
            @Valid @RequestBody UpdateTaskStatusRequest request) {

        TaskResponse response = goalService.updateTaskStatus(goalId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update goal status", description = "Updates the status of a goal (e.g., PLANNING, IN_PROGRESS, COMPLETED)")
    public ResponseEntity<GoalResponse> updateGoalStatus(
            @PathVariable Long id,
            @RequestParam("status") String status) {
        GoalResponse response = goalService.updateGoalStatus(id, status);
        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/{id}")
    @Operation(summary = "Delete goal", description = "Deletes a goal and all its tasks")
    public ResponseEntity<Void> deleteGoal(@PathVariable Long id) {
        goalService.deleteGoal(id);
        return ResponseEntity.noContent().build();
    }
}
