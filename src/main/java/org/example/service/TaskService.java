package org.example.service;

import org.example.models.TaskModel;
import org.example.repository.TaskRepository;
import org.example.exception.task.TaskValidator;
import org.example.exception.task.TaskException;

import java.util.Scanner;

public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskValidator taskValidator;

    public TaskService(TaskRepository taskRepository, TaskValidator taskValidator) {
        this.taskRepository = taskRepository;
        this.taskValidator = taskValidator;
    }

    public TaskModel createTask(Scanner vs)  {

        System.out.println("Digite o titulo do chamado: ");
        String title = vs.nextLine();

        System.out.println("Digite a descricao do chamado: ");
        String description = vs.nextLine();

        System.out.println("selecione a prioridade do chamado: ");
        System.out.println("1 - Baixa");
        System.out.println("2 - Média");
        System.out.println("3 - Alta");
        int priorityChoice = vs.nextInt();
        vs.nextLine(); // Consumir a quebra de linha após o nextInt()

        String priority = switch (priorityChoice) {
            case 1 -> "Baixa";
            case 2 -> "Média";
            case 3 -> "Alta";
            default -> throw new IllegalArgumentException("Prioridade inválida");
        };

        try {
            taskValidator.titleValidator(title, description, priorityChoice);
            TaskModel task = new TaskModel(title, description, priority);
            taskRepository.createTask(task);
            return task;
        }catch (TaskException e) {
            System.out.println(e.getMessage());
        }
       return null;
    }
    public void deleteTask (Scanner vs) {

        System.out.println("Digite o id da tarefa que deseja excluir: ");
        int id = vs.nextInt();
        vs.nextLine(); // Consumir a quebra de linha após o nextInt()

        taskRepository.localizarTask(id);

        System.out.println("Deseja encerrar essa tarefa? [S/N]");
        String resp = vs.nextLine();
        if (resp.equalsIgnoreCase("N")) {
            System.out.println("Tarefa não excluída.");
        } else if (resp.equalsIgnoreCase("S")) {
            taskRepository.deleteTask(id);
        } else {
            System.out.println("Opção inválida. Tarefa não excluída.");
        }













    }
    public void findTask (Scanner vs) {
        System.out.println("Selecione uma opção abaixo. ");
        System.out.println("1- Listar todas as tarefas:");
        System.out.println("2- Localizar tarefa por ID:");
        String resp = vs.nextLine();

        switch (resp) {
            case "1" -> taskRepository.listAllTasks();
            case "2" -> {
                System.out.println("Digite o ID da tarefa que deseja localizar: ");
                int id = vs.nextInt();
                vs.nextLine(); // Consumir a quebra de linha após o nextInt()
                taskRepository.localizarTask(id);
            }
            default -> System.out.println("Opção inválida.");
        }








    }





}
