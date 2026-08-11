package org.example.menu.login;

import org.example.menu.task.MenuTask;
import org.example.menu.user.MenuUsers;
import org.example.models.UserModel;
import org.example.repository.UserRepository;
import org.example.service.UserService;

import java.util.Scanner;

public class MenuLogin {


    public void login(Scanner vs, UserService userService) {
        UserModel userLoggedIn = null;

        UserRepository userRepository = new UserRepository();
        MenuTask task = new MenuTask();
        MenuUsers users = new MenuUsers();

        System.out.println("Bem-vindo ao sistema!");
        System.out.println("---------------------");

        while (userLoggedIn == null) {
            System.out.println("Digite o seu email: ");
            String email = vs.nextLine();

            System.out.println("Digite a sua senha: ");
            String password = vs.nextLine();

            userLoggedIn = userRepository.verifyLoginUsers(email, password);

            if (userLoggedIn == null) {
                System.out.println("E-mail ou senha incorreta, favor tentar novamente!");
            } else {
                System.out.println("Login realizado com sucesso!");
                System.out.println("Bem-vindo, " + userLoggedIn.getName() + "!");
                System.out.println("---------------------");
                menuService(task, users, vs, userService);
            }


        }


    }


    public void menuService (MenuTask task, MenuUsers users, Scanner vs, UserService userService) {

        System.out.println("Selecione um dos menus abaixo:");
        System.out.println("1 - Menu de usuários:");
        System.out.println("2 - Menu de tarefas: ");
        int resp = vs.nextInt();
        vs.nextLine();

        switch (resp) {
            case 1 -> users.menuUsers(vs, userService);
            case 2 -> task.menuTask(vs);
        }

    }








}






