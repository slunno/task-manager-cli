package org.example.controller;


import org.example.dto.task.CreateTaskRequest;
import org.example.models.TaskModel;
import org.example.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Scanner;


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

        TaskModel deletedTask = taskService.deleteTask(new CreateTaskRequest() {{
            setTaskId(id);
        }});

        return ResponseEntity.ok(deletedTask);
    }


//   @GetMapping
//    public ResponseEntity findTasks(long id, long id2) {
//        List tasks = taskService.findTask(id, id2);
//        return ResponseEntity.ok(tasks);
//    }





}
