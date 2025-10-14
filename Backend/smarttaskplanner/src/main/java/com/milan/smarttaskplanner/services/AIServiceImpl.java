package com.milan.smarttaskplanner.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.milan.smarttaskplanner.config.OpenAIProperties;
import com.milan.smarttaskplanner.dto.GoalRequest;
import com.milan.smarttaskplanner.dto.TaskResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class AIServiceImpl implements AIService {

    private final OpenAIProperties openAIProperties;

    @Autowired
    public AIServiceImpl(OpenAIProperties openAIProperties) {
        this.openAIProperties = openAIProperties;
        log.info("=== AIServiceImpl Initialized ===");
        log.info("API Key present: {}", openAIProperties.getKey() != null && !openAIProperties.getKey().isEmpty());
        log.info("API Key length: {}", openAIProperties.getKey() != null ? openAIProperties.getKey().length() : 0);
        log.info("Model: {}", openAIProperties.getModel());
        log.info("Max Tokens: {}", openAIProperties.getMaxTokens());
        log.info("Temperature: {}", openAIProperties.getTemperature());
    }

    @Override
    public Map<String, Object> generateTaskPlan(GoalRequest request) {
        log.info("========================================");
        log.info("Starting task plan generation");
        log.info("Goal: {}", request.getDescription());
        log.info("Target Date: {}", request.getTargetDate());
        log.info("========================================");

        try {
            // Validate API key
            if (openAIProperties.getKey() == null || openAIProperties.getKey().trim().isEmpty()) {
                log.error("CRITICAL: Gemini API Key is not configured!");
                return generateFallbackPlan(request);
            }

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-goog-api-key", openAIProperties.getKey());
            headers.setContentType(MediaType.APPLICATION_JSON);

            log.info("Headers configured with API key");

            String prompt = buildPrompt(request);
            log.info("Prompt built. Length: {} characters", prompt.length());
            log.debug("Prompt content:\n{}", prompt);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(
                    Map.of("parts", List.of(
                            Map.of("text", prompt)
                    ))
            ));

            // Gemini generation config - increase tokens for detailed descriptions
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", openAIProperties.getTemperature());
            generationConfig.put("maxOutputTokens", Math.max(openAIProperties.getMaxTokens(), 8000));
            requestBody.put("generationConfig", generationConfig);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String geminiUrl = String.format(
                    "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent",
                    openAIProperties.getModel()
            );

            log.info("Calling Gemini API");
            log.info("URL: {}", geminiUrl);
            log.info("Model: {}", openAIProperties.getModel());

            ResponseEntity<String> response = restTemplate.postForEntity(
                    geminiUrl,
                    entity,
                    String.class
            );

            log.info("✅ Gemini API call successful!");
            log.info("Status Code: {}", response.getStatusCode());
            log.info("Response Body Length: {}", response.getBody() != null ? response.getBody().length() : 0);
            log.debug("Full Response: {}", response.getBody());

            Map<String, Object> result = parseGeminiResponse(response.getBody(), request);
            log.info("✅ Successfully generated plan with {} tasks", result.get("totalTasks"));

            return result;

        } catch (HttpClientErrorException e) {
            log.error("❌ HTTP Client Error (4xx)");
            log.error("Status Code: {}", e.getStatusCode());
            log.error("Response Body: {}", e.getResponseBodyAsString());
            log.error("Error Message: {}", e.getMessage());
            return generateFallbackPlan(request);

        } catch (HttpServerErrorException e) {
            log.error("❌ HTTP Server Error (5xx)");
            log.error("Status Code: {}", e.getStatusCode());
            log.error("Response Body: {}", e.getResponseBodyAsString());
            log.error("Error Message: {}", e.getMessage());
            return generateFallbackPlan(request);

        } catch (Exception e) {
            log.error("❌ Unexpected Error");
            log.error("Error Type: {}", e.getClass().getName());
            log.error("Error Message: {}", e.getMessage());
            log.error("Stack Trace:", e);
            return generateFallbackPlan(request);
        }
    }

    private String buildPrompt(GoalRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an expert project manager and task planner. ");
        prompt.append("Break down the following goal into detailed, actionable tasks with comprehensive descriptions.\n\n");
        prompt.append("Goal: ").append(request.getDescription()).append("\n");

        LocalDateTime targetDate = request.getTargetDate();
        LocalDateTime currentDate = LocalDateTime.now();

        if (targetDate != null) {
            prompt.append("Target Completion Date: ")
                    .append(targetDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .append("\n");

            long daysAvailable = java.time.Duration.between(currentDate, targetDate).toDays();
            prompt.append("Days Available: ").append(daysAvailable).append(" days\n");
            prompt.append("Please ensure the total estimated hours fit realistically within this timeframe.\n");
        }

        if (request.getConstraints() != null && !request.getConstraints().isEmpty()) {
            prompt.append("Constraints: ").append(String.join(", ", request.getConstraints())).append("\n");
        }

        prompt.append("\nProvide a structured task breakdown in JSON format with this EXACT structure:\n");
        prompt.append("{\n");
        prompt.append("  \"analysis\": \"Brief analysis of the goal and approach (2-3 sentences)\",\n");
        prompt.append("  \"totalTasks\": <number_of_tasks>,\n");
        prompt.append("  \"estimatedTotalHours\": <sum_of_all_task_hours>,\n");
        prompt.append("  \"suggestedStartDate\": \"").append(currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\",\n");

        if (targetDate != null) {
            prompt.append("  \"suggestedEndDate\": \"").append(targetDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\",\n");
        } else {
            prompt.append("  \"suggestedEndDate\": \"<calculate based on total hours>\",\n");
        }

        prompt.append("  \"tasks\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"title\": \"Task name\",\n");
        prompt.append("      \"description\": \"Brief 1-2 sentence summary of the task\",\n");
        prompt.append("      \"detailedDescription\": \"Comprehensive 3-5 paragraph explanation covering: what needs to be done, why it's important, key considerations, potential challenges, and expected outcomes. Be specific and actionable.\",\n");
        prompt.append("      \"steps\": [\n");
        prompt.append("        \"Step 1: Specific action to take\",\n");
        prompt.append("        \"Step 2: Next specific action\",\n");
        prompt.append("        \"Step 3: Continue with detailed steps\"\n");
        prompt.append("      ],\n");
        prompt.append("      \"estimatedDurationHours\": 5,\n");
        prompt.append("      \"priority\": \"HIGH\",\n");
        prompt.append("      \"status\": \"PENDING\",\n");
        prompt.append("      \"orderIndex\": 1,\n");
        prompt.append("      \"dependencies\": []\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"recommendations\": [\"recommendation 1\", \"recommendation 2\"],\n");
        prompt.append("  \"risks\": [\"risk 1\", \"risk 2\"]\n");
        prompt.append("}\n\n");
        prompt.append("CRITICAL REQUIREMENTS:\n\n");
        prompt.append("TASK QUANTITY:\n");
        prompt.append("- Analyze the goal complexity and create appropriate number of tasks (3-15 tasks based on complexity)\n");
        prompt.append("- For simple goals: 3-5 tasks\n");
        prompt.append("- For moderate goals: 5-8 tasks\n");
        prompt.append("- For complex goals: 8-15 tasks\n\n");

        prompt.append("TASK DESCRIPTIONS:\n");
        prompt.append("- 'description': Short summary (1-2 sentences) - what the task is about\n");
        prompt.append("- 'detailedDescription': Comprehensive explanation (3-5 paragraphs, 200-400 words) that includes:\n");
        prompt.append("  * What needs to be accomplished and why it matters\n");
        prompt.append("  * Key activities and deliverables\n");
        prompt.append("  * Important considerations and best practices\n");
        prompt.append("  * Potential challenges and how to address them\n");
        prompt.append("  * Expected outcomes and success criteria\n");
        prompt.append("- 'steps': Array of 3-8 specific, actionable steps to complete the task\n");
        prompt.append("  * Each step should be clear and concrete\n");
        prompt.append("  * Steps should be in logical order\n");
        prompt.append("  * Include specific tools, resources, or methods when relevant\n\n");

        prompt.append("OTHER REQUIREMENTS:\n");
        prompt.append("- Each task must have: title, description, detailedDescription, steps, estimatedDurationHours, priority, status, orderIndex, dependencies\n");
        prompt.append("- Vary the task durations realistically: simple tasks (1-4 hours), moderate (4-8 hours), complex (8-20 hours)\n");
        prompt.append("- Priority must be one of: LOW, MEDIUM, HIGH, CRITICAL\n");
        prompt.append("- Distribute priorities realistically (not all tasks should be HIGH or CRITICAL)\n");
        prompt.append("- Status must always be: PENDING\n");
        prompt.append("- estimatedDurationHours must be a realistic number based on task complexity\n");
        prompt.append("- orderIndex should be sequential starting from 1\n");
        prompt.append("- dependencies should be an empty array [] or array of orderIndex values for prerequisite tasks\n");
        prompt.append("- totalTasks should equal the number of tasks in the array\n");
        prompt.append("- estimatedTotalHours should be the sum of all task hours\n");
        prompt.append("- Be realistic and specific: consider the goal's actual requirements when creating descriptions\n");
        prompt.append("- Return ONLY the JSON object, no markdown code blocks, no explanations\n");

        return prompt.toString();
    }

    private Map<String, Object> parseGeminiResponse(String aiRawResponse, GoalRequest request) {
        log.info("Starting to parse Gemini response");
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> plan = new HashMap<>();

        try {
            JsonNode root = mapper.readTree(aiRawResponse);
            log.info("Parsed JSON root successfully");

            // Extract content from Gemini response structure
            String content = root.at("/candidates/0/content/parts/0/text").asText();

            if (content == null || content.isEmpty()) {
                log.error("❌ No content in Gemini response");
                log.error("Response structure: {}", root.toPrettyString());
                throw new RuntimeException("Empty response from Gemini");
            }

            log.info("✅ Extracted content from Gemini response");
            log.info("Content length: {} characters", content.length());
            log.debug("Raw content: {}", content);

            // Clean up markdown code blocks
            content = content.trim();
            if (content.startsWith("```json")) {
                content = content.substring(7).trim();
                log.info("Removed ```json marker");
            } else if (content.startsWith("```")) {
                content = content.substring(3).trim();
                log.info("Removed ``` marker");
            }
            if (content.endsWith("```")) {
                content = content.substring(0, content.length() - 3).trim();
                log.info("Removed closing ``` marker");
            }

            log.debug("Cleaned content: {}", content);
            JsonNode planJson = mapper.readTree(content);
            log.info("✅ Parsed plan JSON successfully");

            // Parse suggested dates from AI response
            LocalDateTime suggestedStartDate = parseDateTime(planJson.path("suggestedStartDate").asText());
            LocalDateTime suggestedEndDate = parseDateTime(planJson.path("suggestedEndDate").asText());

            if (suggestedStartDate == null) {
                suggestedStartDate = LocalDateTime.now();
            }

            // Parse tasks with AI-generated durations
            List<TaskResponse> tasks = new ArrayList<>();
            JsonNode tasksNode = planJson.path("tasks");
            log.info("Tasks node is array: {}", tasksNode.isArray());
            log.info("Tasks count: {}", tasksNode.size());

            LocalDateTime currentStartDate = suggestedStartDate;
            int totalHours = 0;

            if (tasksNode.isArray()) {
                for (int i = 0; i < tasksNode.size(); i++) {
                    JsonNode taskNode = tasksNode.get(i);

                    // Use AI-generated duration, not a default value
                    int estimatedHours = taskNode.path("estimatedDurationHours").asInt(4);
                    totalHours += estimatedHours;

                    // Calculate dates based on sequential scheduling (tasks run one after another)
                    LocalDateTime taskStart = currentStartDate;
                    LocalDateTime taskEnd = taskStart.plusHours(estimatedHours);

                    // Parse steps array
                    List<String> steps = new ArrayList<>();
                    JsonNode stepsNode = taskNode.path("steps");
                    if (stepsNode.isArray()) {
                        stepsNode.forEach(step -> steps.add(step.asText()));
                    }

                    TaskResponse task = TaskResponse.builder()
                            .title(taskNode.path("title").asText("Untitled Task"))
                            .description(taskNode.path("description").asText("No description"))
                            .detailedDescription(taskNode.path("detailedDescription").asText(
                                    taskNode.path("description").asText("No detailed description available")))
                            .steps(steps)
                            .estimatedDurationHours(estimatedHours)
                            .priority(taskNode.path("priority").asText("MEDIUM"))
                            .status(taskNode.path("status").asText("PENDING"))
                            .orderIndex(taskNode.path("orderIndex").asInt(i + 1))
                            .dependencies(parseDependencies(taskNode.path("dependencies")))
                            .startDate(taskStart)
                            .endDate(taskEnd)
                            .build();

                    tasks.add(task);
                    currentStartDate = taskEnd; // Next task starts when this one ends

                    log.info("Parsed task {}: {} ({} hours, priority: {}, {} steps)",
                            i + 1, task.getTitle(), estimatedHours, task.getPriority(), steps.size());
                }
            }

            // Use AI's calculated end date if available, otherwise calculate from tasks
            if (suggestedEndDate == null || suggestedEndDate.isBefore(currentStartDate)) {
                suggestedEndDate = currentStartDate;
            }

            plan.put("analysis", planJson.path("analysis").asText("No analysis provided"));
            plan.put("tasks", tasks);
            plan.put("totalTasks", tasks.size());
            plan.put("estimatedTotalHours", totalHours);
            plan.put("suggestedStartDate", suggestedStartDate);
            plan.put("suggestedEndDate", suggestedEndDate);
            plan.put("recommendations", parseList(planJson.path("recommendations")));
            plan.put("risks", parseList(planJson.path("risks")));

            log.info("✅ Successfully parsed {} tasks from Gemini response", tasks.size());
            log.info("Total estimated hours: {}", totalHours);
            log.info("Suggested timeline: {} to {}", suggestedStartDate, suggestedEndDate);

        } catch (Exception e) {
            log.error("❌ Error parsing Gemini response: {}", e.getMessage(), e);
            return generateFallbackPlan(request);
        }

        return plan;
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }

        try {
            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            log.warn("Could not parse datetime: {}", dateTimeStr);
            return null;
        }
    }

    private List<String> parseList(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node.isArray()) {
            node.forEach(item -> result.add(item.asText()));
        }
        return result;
    }

    private List<Long> parseDependencies(JsonNode node) {
        List<Long> dependencies = new ArrayList<>();
        if (node.isArray()) {
            node.forEach(item -> dependencies.add(item.asLong()));
        }
        return dependencies;
    }

    private Map<String, Object> generateFallbackPlan(GoalRequest request) {
        log.warn("⚠️ Generating fallback plan");

        Map<String, Object> result = new HashMap<>();
        result.put("analysis", "⚠️ This is a sample task plan. The AI service is not configured or encountered an error. " +
                "Please configure your Gemini API key to get AI-generated plans.");

        List<TaskResponse> tasks = new ArrayList<>();
        LocalDateTime currentDate = LocalDateTime.now();
        LocalDateTime currentStartDate = currentDate;

        String goalDesc = request != null ? request.getDescription() : "Sample Goal";

        String[] taskTitles = {
                "Research and Planning for: " + goalDesc,
                "Setup and Preparation",
                "Core Implementation",
                "Testing and Quality Assurance",
                "Final Review and Deployment"
        };

        String[] taskDescriptions = {
                "Conduct thorough research and create a detailed project plan",
                "Set up necessary tools, environments, and resources",
                "Execute the main tasks and deliverables",
                "Test all components and ensure quality standards",
                "Perform final checks and deploy/deliver the results"
        };

        String[] detailedDescriptions = {
                "This initial phase focuses on comprehensive research and strategic planning. Begin by gathering all relevant information about the project requirements, constraints, and success criteria. Analyze similar projects or case studies to understand best practices and potential pitfalls. Create a detailed project plan that outlines milestones, deliverables, and timelines. Document your findings and share them with stakeholders for feedback. This foundation will guide all subsequent work.",
                "In this phase, you'll prepare your working environment and gather necessary resources. Install and configure all required tools, software, and frameworks. Set up version control, development environments, and any collaboration platforms. Create initial project structure and documentation templates. Verify that all team members have access to necessary resources. This preparation ensures smooth execution of the main implementation phase.",
                "This is the main execution phase where you'll implement the core functionality. Break down the work into manageable chunks and tackle them systematically. Follow coding best practices and maintain clean, documented code. Regular commits and progress reviews help maintain momentum. Stay focused on the primary objectives while remaining flexible to adjust as needed. This phase typically consumes the most time and effort.",
                "Quality assurance is critical for project success. Develop comprehensive test cases covering all functionality. Perform unit tests, integration tests, and end-to-end testing. Document any bugs or issues discovered and track their resolution. Involve stakeholders in user acceptance testing when appropriate. This thorough testing ensures the final product meets all requirements and quality standards.",
                "The final phase involves careful review and deployment preparation. Conduct a comprehensive review of all deliverables against initial requirements. Address any remaining issues or improvements. Prepare deployment documentation and rollback procedures. Execute the deployment following established protocols. Monitor the initial deployment closely and be prepared to address any issues. Celebrate the successful completion of the project."
        };

        String[][] taskSteps = {
                {
                        "Gather and analyze project requirements and constraints",
                        "Research similar projects and industry best practices",
                        "Identify potential risks and mitigation strategies",
                        "Create detailed project timeline with milestones",
                        "Document findings and get stakeholder approval"
                },
                {
                        "Install required development tools and frameworks",
                        "Configure development and testing environments",
                        "Set up version control and collaboration platforms",
                        "Create initial project structure and templates",
                        "Verify team access to all necessary resources"
                },
                {
                        "Break down work into manageable tasks",
                        "Implement core features following best practices",
                        "Write clean, documented code with regular commits",
                        "Conduct code reviews and address feedback",
                        "Track progress and adjust timeline as needed"
                },
                {
                        "Develop comprehensive test cases and scenarios",
                        "Execute unit, integration, and end-to-end tests",
                        "Document and prioritize any issues found",
                        "Fix bugs and retest affected functionality",
                        "Conduct user acceptance testing with stakeholders"
                },
                {
                        "Review all deliverables against requirements",
                        "Address final improvements and polish",
                        "Prepare deployment documentation and procedures",
                        "Execute deployment following protocols",
                        "Monitor deployment and address any issues"
                }
        };

        String[] priorities = {"HIGH", "MEDIUM", "HIGH", "MEDIUM", "CRITICAL"};
        int[] hours = {8, 6, 16, 8, 4};
        int totalHours = 0;

        for (int i = 0; i < 5; i++) {
            LocalDateTime taskStart = currentStartDate;
            LocalDateTime taskEnd = taskStart.plusHours(hours[i]);

            TaskResponse task = TaskResponse.builder()
                    .title(taskTitles[i])
                    .description(taskDescriptions[i])
                    .detailedDescription(detailedDescriptions[i])
                    .steps(Arrays.asList(taskSteps[i]))
                    .estimatedDurationHours(hours[i])
                    .priority(priorities[i])
                    .status("PENDING")
                    .orderIndex(i + 1)
                    .dependencies(i > 0 ? List.of((long) i) : List.of())
                    .startDate(taskStart)
                    .endDate(taskEnd)
                    .build();

            tasks.add(task);
            totalHours += hours[i];
            currentStartDate = taskEnd;
        }

        result.put("tasks", tasks);
        result.put("totalTasks", 5);
        result.put("estimatedTotalHours", totalHours);
        result.put("suggestedStartDate", currentDate);
        result.put("suggestedEndDate", currentStartDate);
        result.put("recommendations", List.of(
                "✅ Configure your Gemini API key in application.yml",
                "✅ Set GEMINI_API_KEY environment variable",
                "✅ Get API key from: https://aistudio.google.com/app/apikey",
                "Break down large tasks into smaller chunks",
                "Set clear milestones and deadlines",
                "Regular progress reviews help maintain momentum"
        ));
        result.put("risks", List.of(
                "❌ Gemini API not configured - using sample data",
                "Scope creep without proper planning",
                "Resource constraints may impact timeline",
                "Inadequate testing may lead to quality issues"
        ));

        return result;
    }
}