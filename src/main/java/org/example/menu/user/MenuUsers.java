package org.example.menu.user;

import org.example.service.UserService;

import java.util.Scanner;

public class MenuUsers {

    public void menuUsers (Scanner vs, UserService userService) {



        System.out.println("Bem-vindo ao sistema de usuários, escolha uma opção abaixo:");
        System.out.println("1 - Criar usuário: ");
        System.out.println("2 - Deletar usuário: ");
        System.out.println("3 - Menu de usuários: ");
        int resp = vs.nextInt();
        vs.nextLine(); // Consumir a quebra de linha após o nextInt()


        switch (resp) {
            case 1:
                userService.createUser(vs);
                break;
            case 2:
                userService.deleleteUser(vs);
                break;
            case 3:
                userService.listUsers(vs);
                break;
            default:
                System.out.println("Opção inválida!");
        }

    }


}
