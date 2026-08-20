package org.example.service;

import org.example.dto.task.CreateTaskRequest;
import org.example.models.TaskModel;
import org.example.repository.TaskRepository;
import org.example.exception.task.TaskValidator;
import org.example.exception.task.TaskException;
import org.springframework.stereotype.Service;

import java.util.List;




@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskValidator taskValidator;

    public TaskService(TaskRepository taskRepository, TaskValidator taskValidator) {
        this.taskRepository = taskRepository;
        this.taskValidator = taskValidator;
    }

    public TaskModel createTask(CreateTaskRequest request)   {
        try {
            taskValidator.titleValidator(request.getTitle(), request.getDescription());
            TaskModel task = new TaskModel(request.getTitle(), request.getDescription(), request.getPriority());
            return taskRepository.createTask(task);
        } catch (TaskException e) {
            throw new TaskException(e.getMessage());
        }

    }

    public TaskModel deleteTask (CreateTaskRequest request)   {

        TaskModel task = new TaskModel(request.getTaskId());
        taskRepository.deleteTask(task.getTaskId());
        return task;
    }

    public TaskModel findTask(long id) {
        return taskRepository.findTask(id);
    }

    public List<TaskModel> listAllTasks() {
        return taskRepository.listAllTasks();
    }
}


