package org.example.controller;


import org.example.dto.task.CreateTaskRequest;
import org.example.models.TaskModel;
import org.example.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<TaskModel> createTask(@RequestBody CreateTaskRequest createTaskRequest) {

        TaskModel task = taskService.createTask(createTaskRequest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(task);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<TaskModel> deleteTask(@PathVariable long id) {
        TaskModel deletedTask = taskService.deleteTask(id);
        return ResponseEntity.ok(deletedTask);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskModel> findTask(@PathVariable long id) {
        TaskModel task = taskService.findTask(id);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(task);
    }

    @GetMapping
    public ResponseEntity<List<TaskModel>> listTasks() {
        List<TaskModel> tasks = taskService.listAllTasks();
        return ResponseEntity.ok(tasks);
    }

}
