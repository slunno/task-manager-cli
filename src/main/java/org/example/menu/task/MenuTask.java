package org.example.menu.task;

import org.example.repository.TaskRepository;
import org.example.service.TaskService;
import org.example.exception.task.TaskValidator;

import java.util.Scanner;

public class MenuTask {

    public void menuTask (Scanner vs) {

        TaskService taskService = new TaskService(new TaskRepository(), new TaskValidator());

        System.out.println("Bem-vindo ao sistema de tarefas, escolha uma opção abaixo:");
        System.out.println("1 - Criar uma tarefa: ");
        System.out.println("2 - Excluir uma tarefa: ");
        System.out.println("3 - Menu de tarefas: ");
        int resp = vs.nextInt();
        vs.nextLine(); // Consumir a quebra de linha após o nextInt()


        switch (resp) {
            case 1:
                taskService.createTask(vs);
                break;
            case 2:
                taskService.deleteTask(vs);
                break;
            case 3:
                taskService.findTask(vs);
                break;
            default:
                System.out.println("Opção inválida!");
        }

    }





}
