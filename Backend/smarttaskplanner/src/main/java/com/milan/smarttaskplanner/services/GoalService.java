package com.milan.smarttaskplanner.services;

import com.milan.smarttaskplanner.dto.*;
import com.milan.smarttaskplanner.entities.*;
import com.milan.smarttaskplanner.repositories.GoalRepository;
import com.milan.smarttaskplanner.repositories.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoalService {

    private final GoalRepository goalRepository;
    private final TaskRepository taskRepository;
    private final AIService aiService;

    @Transactional
    public TaskPlanResponse createGoalWithTasks(GoalRequest request) {
        log.info("Creating goal: {}", request.getDescription());

        // Generate task plan using AI
        Map<String, Object> aiPlan = aiService.generateTaskPlan(request);

        // Create Goal entity
        Goal goal = Goal.builder()
                .description(request.getDescription())
                .targetDate(request.getTargetDate())
                .status(GoalStatus.PLANNING)
                .aiAnalysis((String) aiPlan.get("analysis"))
                .build();

        goal = goalRepository.save(goal);

        // Create Task entities from AI response
        List<TaskResponse> taskResponses = (List<TaskResponse>) aiPlan.get("tasks");
        if (taskResponses == null) {
            taskResponses = List.of();
        }

        for (TaskResponse taskResponse : taskResponses) {
            Task task = Task.builder()
                    .title(taskResponse.getTitle())
                    .description(taskResponse.getDescription())
                    .detailedDescription(taskResponse.getDetailedDescription())  // NEW
                    .steps(taskResponse.getSteps())  // NEW
                    .goal(goal)
                    .estimatedDurationHours(taskResponse.getEstimatedDurationHours())
                    .startDate(taskResponse.getStartDate())
                    .endDate(taskResponse.getEndDate())
                    .priority(TaskPriority.valueOf(taskResponse.getPriority()))
                    .status(TaskStatus.PENDING)
                    .orderIndex(taskResponse.getOrderIndex())
                    .dependencies(taskResponse.getDependencies())
                    .build();

            goal.addTask(task);
        }

        goal = goalRepository.save(goal);

        Object startDateObj = aiPlan.get("suggestedStartDate");
        LocalDateTime suggestedStartDate = null;
        if (startDateObj instanceof String) {
            suggestedStartDate = LocalDateTime.parse((String) startDateObj);
        } else if (startDateObj instanceof LocalDateTime) {
            suggestedStartDate = (LocalDateTime) startDateObj;
        }

        Object endDateObj = aiPlan.get("suggestedEndDate");
        LocalDateTime suggestedEndDate = null;
        if (endDateObj instanceof String) {
            suggestedEndDate = LocalDateTime.parse((String) endDateObj);
        } else if (endDateObj instanceof LocalDateTime) {
            suggestedEndDate = (LocalDateTime) endDateObj;
        }

        return TaskPlanResponse.builder()
                .goalAnalysis((String) aiPlan.get("analysis"))
                .totalTasks((Integer) aiPlan.get("totalTasks"))
                .estimatedTotalHours((Integer) aiPlan.get("estimatedTotalHours"))
                .suggestedStartDate(suggestedStartDate)
                .suggestedEndDate(suggestedEndDate)
                .tasks(mapToTaskResponses(goal.getTasks()))
                .recommendations((List<String>) aiPlan.get("recommendations"))
                .risks((List<String>) aiPlan.get("risks"))
                .build();
    }

    public GoalResponse getGoal(Long id) {
        Goal goal = goalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Goal not found with id: " + id));

        return mapToGoalResponse(goal);
    }

    public List<GoalResponse> getAllGoals() {
        return goalRepository.findAll().stream()
                .map(this::mapToGoalResponse)
                .collect(Collectors.toList());
    }

    public List<GoalResponse> getRecentGoals() {
        return goalRepository.findTop10ByOrderByCreatedAtDesc().stream()
                .map(this::mapToGoalResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskResponse updateTaskStatus(Long goalId, UpdateTaskStatusRequest request) {
        Task task = taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (!task.getGoal().getId().equals(goalId)) {
            throw new RuntimeException("Task does not belong to this goal");
        }

        task.setStatus(TaskStatus.valueOf(request.getStatus()));
        task = taskRepository.save(task);

        // Update goal status if all tasks are completed
        updateGoalStatus(goalId);

        return mapToTaskResponse(task);
    }

    @Transactional
    public void deleteGoal(Long id) {
        if (!goalRepository.existsById(id)) {
            throw new RuntimeException("Goal not found with id: " + id);
        }
        goalRepository.deleteById(id);
    }

    @Transactional
    public GoalResponse updateGoalStatus(Long id, String status) {
        Goal goal = goalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Goal not found with id: " + id));
        goal.setStatus(GoalStatus.valueOf(status));
        goal = goalRepository.save(goal);
        return mapToGoalResponse(goal);
    }

    private void updateGoalStatus(Long goalId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found"));

        List<Task> tasks = goal.getTasks();

        if (tasks.isEmpty()) {
            return;
        }

        long completedTasks = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.COMPLETED)
                .count();

        if (completedTasks == tasks.size()) {
            goal.setStatus(GoalStatus.COMPLETED);
        } else if (completedTasks > 0) {
            goal.setStatus(GoalStatus.IN_PROGRESS);
        }

        goalRepository.save(goal);
    }

    // Mapping methods
    private GoalResponse mapToGoalResponse(Goal goal) {
        return GoalResponse.builder()
                .id(goal.getId())
                .description(goal.getDescription())
                .targetDate(goal.getTargetDate())
                .status(goal.getStatus().name())
                .tasks(mapToTaskResponses(goal.getTasks()))
                .aiAnalysis(goal.getAiAnalysis())
                .createdAt(goal.getCreatedAt())
                .updatedAt(goal.getUpdatedAt())
                .build();
    }

    private List<TaskResponse> mapToTaskResponses(List<Task> tasks) {
        if (tasks == null) return List.of();
        return tasks.stream()
                .map(this::mapToTaskResponse)
                .collect(Collectors.toList());
    }

    private TaskResponse mapToTaskResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .detailedDescription(task.getDetailedDescription())  // NEW
                .steps(task.getSteps())  // NEW
                .estimatedDurationHours(task.getEstimatedDurationHours())
                .startDate(task.getStartDate())
                .endDate(task.getEndDate())
                .priority(task.getPriority().name())
                .status(task.getStatus().name())
                .orderIndex(task.getOrderIndex())
                .dependencies(task.getDependencies())
                .createdAt(task.getCreatedAt())
                .build();
    }
}