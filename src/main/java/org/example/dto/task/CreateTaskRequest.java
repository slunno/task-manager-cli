package org.example.dto.task;

public class CreateTaskRequest {

    private long taskId;
    private String title;
    private String description;
    private String priority;

    public CreateTaskRequest() {
    }

    public String getTitle() {return title;}

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


    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }
}
