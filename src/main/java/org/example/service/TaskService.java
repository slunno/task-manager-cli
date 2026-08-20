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

    /*public List findTask () {
        System.out.println("Selecione uma opção abaixo. ");
        System.out.println("1- Listar todas as tarefas:");
        System.out.println("2- Localizar tarefa por ID:");

        switch ((int) id) {
            case 1 -> {
                return taskRepository.listAllTasks();
            }
            case 2 -> {
                System.out.println("Digite o ID da tarefa que deseja localizar: ");

                return java.util.Collections.singletonList(taskRepository.findTask(id));
            }
            default -> {
                System.out.println("Opção inválida.");
                return java.util.Collections.emptyList();
            }
        }*/

    }


