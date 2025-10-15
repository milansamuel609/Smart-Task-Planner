import React, { useState, useEffect } from "react";
import axios from "axios";
import "./App.css";

// If you want to deploy then use the below API_URL
// const API_URL = process.env.REACT_APP_API_URL || "http://localhost:8080";

function App() {
  const [goal, setGoal] = useState("");
  const [targetDate, setTargetDate] = useState("");
  const [plan, setPlan] = useState(null);
  const [allGoals, setAllGoals] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [activeTab, setActiveTab] = useState("create");
  const [expandedGoal, setExpandedGoal] = useState(null);
  const [expandedTaskIdx, setExpandedTaskIdx] = useState(null);
  const [expandedGoalTask, setExpandedGoalTask] = useState(null);

  useEffect(() => {
    fetchGoals();
  }, []);

  const fetchGoals = () => {
    axios.get(`http://localhost:8080/api/goals/recent`)
      .then(res => setAllGoals(res.data))
      .catch(err => console.error(err));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!goal.trim()) return;
    setLoading(true);
    setError("");
    setPlan(null);
    try {
      const resp = await axios.post(`http://localhost:8080/api/goals`, {
        description: goal,
        targetDate: targetDate ? `${targetDate}T00:00:00` : null
      });
      setPlan(resp.data);
      setGoal("");
      setTargetDate("");
      fetchGoals();
    } catch (err) {
      setError(err?.response?.data?.message || "Failed to generate plan");
    } finally {
      setLoading(false);
    }
  };

  const updateTaskStatus = async (goalId, taskId, currentStatus) => {
    const newStatus = currentStatus === "COMPLETED" ? "PENDING" : "COMPLETED";
    try {
      await axios.put(`http://localhost:8080/api/goals/${goalId}/tasks/status`, {
        taskId,
        status: newStatus
      });
      fetchGoals();
    } catch (err) {
      setError("Failed to update task status");
    }
  };

  const deleteGoal = async (goalId) => {
    if (!window.confirm("Are you sure you want to delete this goal?")) return;
    try {
      await axios.delete(`http://localhost:8080/api/goals/${goalId}`);
      fetchGoals();
    } catch (err) {
      setError("Failed to delete goal");
    }
  };

  const startGoal = async (goalId) => {
    try {
      await axios.put(`http://localhost:8080/api/goals/${goalId}/status?status=IN_PROGRESS`);
      fetchGoals();
    } catch (err) {
      setError("Failed to start goal");
    }
  };

  const getPriorityColor = (priority) => {
    const colors = {
      CRITICAL: "priority-critical",
      HIGH: "priority-high",
      MEDIUM: "priority-medium",
      LOW: "priority-low"
    };
    return colors[priority] || "priority-medium";
  };

  const getStatusColor = (status) => {
    const colors = {
      COMPLETED: "status-completed",
      IN_PROGRESS: "status-in-progress",
      PLANNING: "status-planning",
      PENDING: "status-pending",
      BLOCKED: "status-blocked"
    };
    return colors[status] || "status-pending";
  };

  const formatDate = (dateString) => {
    if (!dateString) return "Not set";
    return new Date(dateString).toLocaleDateString("en-US", {
      month: "short",
      day: "numeric",
      year: "numeric"
    });
  };

  const formatDateOnly = (dateString) => {
    if (!dateString) return "Not set";
    return new Date(dateString).toLocaleDateString("en-US", {
      month: "short",
      day: "numeric",
      year: "numeric"
    });
  };

  const calculateProgress = (tasks) => {
    if (!tasks || tasks.length === 0) return 0;
    const completed = tasks.filter(t => t.status === "COMPLETED").length;
    return Math.round((completed / tasks.length) * 100);
  };

  const toggleTaskDetails = (taskIdx) => {
    setExpandedTaskIdx(expandedTaskIdx === taskIdx ? null : taskIdx);
  };

  const toggleGoalTaskDetails = (taskId) => {
    setExpandedGoalTask(expandedGoalTask === taskId ? null : taskId);
  };

  return (
    <div className="app-container">
      {/* Header */}
      <header className="app-header">
        <div className="header-content">
          <div className="logo">
            <svg width="40" height="40" viewBox="0 0 40 40" fill="none">
              <circle cx="20" cy="20" r="18" stroke="#6366f1" strokeWidth="2" />
              <path d="M12 20L18 26L28 14" stroke="#6366f1" strokeWidth="2" strokeLinecap="round" />
            </svg>
            <h1>Smart Task Planner</h1>
          </div>
          <p className="subtitle">AI-powered goal breakdown and task management</p>
        </div>
      </header>

      {/* Tabs */}
      <div className="tabs">
        <button
          className={`tab ${activeTab === "create" ? "active" : ""}`}
          onClick={() => setActiveTab("create")}
        >
          <span>📝</span> Create Goal
        </button>
        <button
          className={`tab ${activeTab === "view" ? "active" : ""}`}
          onClick={() => setActiveTab("view")}
        >
          <span>📊</span> View Goals
        </button>
      </div>

      <div className="content-wrapper">
        {/* Error Message */}
        {error && (
          <div className="alert alert-error">
            <span>⚠️</span>
            {error}
            <button onClick={() => setError("")}>✕</button>
          </div>
        )}

        {/* Create Goal Tab */}
        {activeTab === "create" && (
          <div className="card">
            <h2 className="card-title">Create New Goal</h2>
            <div className="form-container">
              <div className="form-group">
                <label htmlFor="goal">Goal Description *</label>
                <textarea
                  id="goal"
                  placeholder="E.g., Launch a product in 2 weeks, Learn Python in 30 days..."
                  value={goal}
                  onChange={e => setGoal(e.target.value)}
                  rows="4"
                  disabled={loading}
                />
              </div>
              <div className="form-group">
                <label htmlFor="targetDate">Target Completion Date (Optional)</label>
                <input
                  id="targetDate"
                  type="date"
                  value={targetDate}
                  onChange={e => setTargetDate(e.target.value)}
                  disabled={loading}
                />
              </div>
              <button
                onClick={handleSubmit}
                disabled={loading || !goal.trim()}
                className="btn btn-primary"
              >
                {loading ? (
                  <>
                    <span className="spinner"></span>
                    Generating Plan...
                  </>
                ) : (
                  <>
                    <span>🚀</span>
                    Generate Task Plan
                  </>
                )}
              </button>
            </div>
            {/* Generated Plan Display */}
            {plan && (
              <div className="plan-result">
                <h3 className="section-title">Generated Plan</h3>
                <div className="ai-analysis">
                  <div className="analysis-header">
                    <span>🤖</span>
                    <strong>AI Analysis</strong>
                  </div>
                  <p>{plan.goalAnalysis}</p>
                </div>
                <div className="stats-grid">
                  <div className="stat-card">
                    <div className="stat-value">{plan.totalTasks}</div>
                    <div className="stat-label">Total Tasks</div>
                  </div>
                  <div className="stat-card">
                    <div className="stat-value">{plan.estimatedTotalHours}h</div>
                    <div className="stat-label">Estimated Time</div>
                  </div>
                  <div className="stat-card">
                    <div className="stat-value">{formatDate(plan.suggestedEndDate)}</div>
                    <div className="stat-label">Target Date</div>
                  </div>
                </div>
                <div className="tasks-section">
                  <h4>Tasks Breakdown</h4>
                  <div className="tasks-list">
                    {plan.tasks?.map((task, idx) => (
                      <div key={idx} className="task-card">
                        <div
                          className="task-header"
                          style={{ cursor: 'pointer' }}
                          onClick={() => toggleTaskDetails(idx)}
                        >
                          <h5 className="task-title">
                            {expandedTaskIdx === idx ? "▼" : "▶"} {task.title}
                          </h5>
                          <span className={`badge ${getPriorityColor(task.priority)}`}>
                            {task.priority}
                          </span>
                        </div>
                        <p className="task-description">{task.description}</p>
                        <div className="task-meta">
                          <span className="meta-item">
                            <span>⏱️</span>
                            {task.estimatedDurationHours}h
                          </span>
                          <span className="meta-item">
                            <span>📅</span>
                            {formatDate(task.startDate)} - {formatDate(task.endDate)}
                          </span>
                          {task.dependencies?.length > 0 && (
                            <span className="meta-item">
                              <span>🔗</span>
                              Depends on: Task {task.dependencies.join(", ")}
                            </span>
                          )}
                        </div>
                        {/* Expanded Task Details */}
                        {expandedTaskIdx === idx && (
                          <div className="task-details-expanded">
                            <div className="detail-section">
                              <h6>📋 Detailed Description</h6>
                              <p style={{ whiteSpace: 'pre-line' }}>{task.detailedDescription || task.description}</p>
                            </div>
                            {task.steps && task.steps.length > 0 && (
                              <div className="detail-section">
                                <h6>📝 Step-by-Step Guide</h6>
                                <ol className="steps-list">
                                  {task.steps.map((step, stepIdx) => (
                                    <li key={stepIdx}>
                                      {step.replace(/^\d+\.\s*/, "")}
                                    </li>
                                  ))}
                                </ol>
                              </div>
                            )}
                            <div className="detail-section">
                              <h6>📊 Task Information</h6>
                              <div className="detail-grid">
                                <div className="detail-item">
                                  <span className="detail-label">Status:</span>
                                  <span className={`badge small ${getStatusColor(task.status)}`}>
                                    {task.status}
                                  </span>
                                </div>
                                <div className="detail-item">
                                  <span className="detail-label">Priority:</span>
                                  <span className={`badge small ${getPriorityColor(task.priority)}`}>
                                    {task.priority}
                                  </span>
                                </div>
                                <div className="detail-item">
                                  <span className="detail-label">Order:</span>
                                  <span>Task #{task.orderIndex}</span>
                                </div>
                                <div className="detail-item">
                                  <span className="detail-label">Duration:</span>
                                  <span>{task.estimatedDurationHours} hours</span>
                                </div>
                              </div>
                            </div>
                            <div className="detail-section">
                              <h6>📅 Timeline</h6>
                              <div className="timeline-info">
                                <div className="timeline-item">
                                  <span className="timeline-label">Start:</span>
                                  <span className="timeline-value">{formatDateOnly(task.startDate)}</span>
                                </div>
                                <div className="timeline-item">
                                  <span className="timeline-label">End:</span>
                                  <span className="timeline-value">{formatDateOnly(task.endDate)}</span>
                                </div>
                              </div>
                            </div>
                            {task.dependencies?.length > 0 && (
                              <div className="detail-section">
                                <h6>🔗 Dependencies</h6>
                                <p>This task depends on completing: Task {task.dependencies.join(", Task ")}</p>
                                <p className="detail-note">⚠️ Complete the dependent tasks before starting this one.</p>
                              </div>
                            )}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
                {plan.recommendations?.length > 0 && (
                  <div className="recommendations">
                    <h4>💡 Recommendations</h4>
                    <ul>
                      {plan.recommendations.map((rec, idx) => (
                        <li key={idx}>{rec}</li>
                      ))}
                    </ul>
                  </div>
                )}
                {plan.risks?.length > 0 && (
                  <div className="risks">
                    <h4>⚠️ Potential Risks</h4>
                    <ul>
                      {plan.risks.map((risk, idx) => (
                        <li key={idx}>{risk}</li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            )}
          </div>
        )}

        {/* View Goals Tab */}
        {activeTab === "view" && (
          <div className="goals-container">
            <h2 className="section-title">Your Goals</h2>
            {allGoals.length === 0 ? (
              <div className="empty-state">
                <div className="empty-icon">🎯</div>
                <h3>No goals yet</h3>
                <p>Create your first goal to get started!</p>
                <button
                  onClick={() => setActiveTab("create")}
                  className="btn btn-primary"
                >
                  Create Goal
                </button>
              </div>
            ) : (
              <div className="goals-list">
                {allGoals.map(g => {
                  const progress = calculateProgress(g.tasks);
                  const isExpanded = expandedGoal === g.id;
                  return (
                    <div key={g.id} className="goal-card">
                      <div className="goal-header" onClick={() => setExpandedGoal(isExpanded ? null : g.id)}>
                        <div className="goal-info">
                          <h3 className="goal-title">{g.description}</h3>
                          <div className="goal-meta">
                            <span className={`badge ${getStatusColor(g.status)}`}>{g.status}</span>
                            <span className="meta-item">
                              <span>📅</span>
                              {formatDate(g.targetDate)}
                            </span>
                            <span className="meta-item">
                              <span>✓</span>
                              {g.tasks?.filter(t => t.status === "COMPLETED").length || 0}/{g.tasks?.length || 0} tasks
                            </span>
                          </div>
                          <div className="progress-bar">
                            <div className="progress-fill" style={{ width: `${progress}%` }}></div>
                          </div>
                          <div className="progress-text">{progress}% complete</div>
                        </div>
                        <button className="expand-btn">{isExpanded ? "▼" : "▶"}</button>
                        <button
                          className="btn btn-danger"
                          onClick={e => { e.stopPropagation(); deleteGoal(g.id); }}
                          style={{ marginLeft: "10px" }}
                        >🗑️</button>
                        {g.status === "PLANNING" && (
                          <button
                            className="btn btn-primary"
                            onClick={e => { e.stopPropagation(); startGoal(g.id); }}
                            style={{ marginLeft: "6px" }}
                          >▶ Start</button>
                        )}
                      </div>
                      {isExpanded && (
                        <div className="goal-details">
                          {g.aiAnalysis && (
                            <div className="ai-analysis small">
                              <p>{g.aiAnalysis}</p>
                            </div>
                          )}
                          <div className="goal-tasks">
                            <h4>Tasks</h4>
                            {g.tasks?.map(task => {
                              const isTaskExpanded = expandedGoalTask === task.id;
                              return (
                                <div key={task.id} className="task-item">
                                  <button
                                    className={`task-checkbox ${task.status === "COMPLETED" ? "checked" : ""}`}
                                    onClick={() => updateTaskStatus(g.id, task.id, task.status)}
                                  >
                                    {task.status === "COMPLETED" ? "✓" : ""}
                                  </button>
                                  <div className="task-item-content">
                                    <div
                                      style={{ cursor: 'pointer', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}
                                      onClick={() => toggleGoalTaskDetails(task.id)}
                                    >
                                      <h5 className={task.status === "COMPLETED" ? "completed" : ""}>
                                        {isTaskExpanded ? "▼" : "▶"} {task.title}
                                      </h5>
                                    </div>
                                    <p className="task-description">{task.description}</p>
                                    <div className="task-meta">
                                      <span className={`badge small ${getPriorityColor(task.priority)}`}>{task.priority}</span>
                                      <span className="meta-item small">
                                        <span>⏱️</span>
                                        {task.estimatedDurationHours}h
                                      </span>
                                      <span className={`badge small ${getStatusColor(task.status)}`}>{task.status}</span>
                                    </div>
                                    {/* Expanded Task Details in View Goals */}
                                    {isTaskExpanded && (
                                      <div className="task-details-expanded" style={{ marginTop: '1rem' }}>
                                        <div className="detail-section">
                                          <h6>📋 Detailed Description</h6>
                                          <p style={{ whiteSpace: 'pre-line' }}>{task.detailedDescription || task.description}</p>
                                        </div>
                                        {task.steps && task.steps.length > 0 && (
                                          <div className="detail-section">
                                            <h6>📝 Step-by-Step Guide</h6>
                                            <ol className="steps-list">
                                              {task.steps.map((step, stepIdx) => (
                                                <li key={stepIdx}>
                                                  {step.replace(/^\d+\.\s*/, "")}
                                                </li>
                                              ))}
                                            </ol>
                                          </div>
                                        )}
                                        <div className="detail-section">
                                          <h6>📊 Task Information</h6>
                                          <div className="detail-grid">
                                            <div className="detail-item">
                                              <span className="detail-label">Status:</span>
                                              <span className={`badge small ${getStatusColor(task.status)}`}>
                                                {task.status}
                                              </span>
                                            </div>
                                            <div className="detail-item">
                                              <span className="detail-label">Priority:</span>
                                              <span className={`badge small ${getPriorityColor(task.priority)}`}>
                                                {task.priority}
                                              </span>
                                            </div>
                                            <div className="detail-item">
                                              <span className="detail-label">Order:</span>
                                              <span>Task #{task.orderIndex}</span>
                                            </div>
                                            <div className="detail-item">
                                              <span className="detail-label">Duration:</span>
                                              <span>{task.estimatedDurationHours} hours</span>
                                            </div>
                                          </div>
                                        </div>
                                        <div className="detail-section">
                                          <h6>📅 Timeline</h6>
                                          <div className="timeline-info">
                                            <div className="timeline-item">
                                              <span className="timeline-label">Start:</span>
                                              <span className="timeline-value">{formatDateOnly(task.startDate)}</span>
                                            </div>
                                            <div className="timeline-item">
                                              <span className="timeline-label">End:</span>
                                              <span className="timeline-value">{formatDateOnly(task.endDate)}</span>
                                            </div>
                                          </div>
                                        </div>
                                        {task.dependencies?.length > 0 && (
                                          <div className="detail-section">
                                            <h6>🔗 Dependencies</h6>
                                            <p>This task depends on completing: Task {task.dependencies.join(", Task ")}</p>
                                            <p className="detail-note">⚠️ Complete the dependent tasks before starting this one.</p>
                                          </div>
                                        )}
                                      </div>
                                    )}
                                  </div>
                                </div>
                              );
                            })}
                          </div>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}
      </div>
      <footer className="app-footer">
        <p>Powered by AI • Built with Spring Boot & React</p>
      </footer>
    </div>
  );
}

export default App;
