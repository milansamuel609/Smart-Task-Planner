package com.milan.smarttaskplanner.services;

import com.milan.smarttaskplanner.dto.GoalRequest;
import java.util.Map;

public interface AIService {
    Map<String, Object> generateTaskPlan(GoalRequest request);
}
