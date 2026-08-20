package org.example.models;

public class TaskModel {

    private long taskId;
    private String title;
    private String description;
    private String priority;

    public TaskModel(long taskId, String title, String description, String priority) {
        this.taskId = taskId;
        this.title = title;
        this.description = description;
        this.priority = priority;
    }

    public TaskModel(String title, String description, String priority) {
        this.title = title;
        this.description = description;
        this.priority = priority;
    }

    public TaskModel(long taskId) {
        this.taskId = taskId;
    }

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }
}
