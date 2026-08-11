package org.example.service;

import org.example.exception.user.UsersValidator;
import org.example.exception.user.UserException;
import org.example.models.UserModel;
import org.example.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Scanner;

public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Método para criar o usuário.
    public UserModel createUser (Scanner vs) {
        UsersValidator userExceptions = new UsersValidator();

        System.out.println("Digite o nome do usuário: ");
        String name = vs.nextLine();
        System.out.println("Digite o e-mail do usuário: ");
        String email = vs.nextLine();
        String passwordHash = BCrypt.hashpw(password(vs), BCrypt.gensalt());



        try {
            userExceptions.userValidator(name, email);
            UserModel user = new UserModel(name, email, passwordHash); // 0 como ID temporário
            userRepository.createUser(user);
            return user;
        } catch (UserException e) {
            System.out.println(e.getMessage());
        }


        return null;
    }

    // Método para deletar um usuário
    public void deleteUser (Scanner vs) {
        System.out.println("Digite o id do usuário: ");
        long id = vs.nextInt();
        vs.nextLine();

        UserRepository userRepository = new UserRepository();
        userRepository.deleteUser(id);
    }

    // Método para localizar usuários.
    public void listUsers (Scanner vs) {
        System.out.println("Escolha uma das opções abaixo: ");
        System.out.println("1 - Localizar usuário: ");
        System.out.println("2 - Listar todos os usuários: ");
        int resp = vs.nextInt();
        vs.nextLine();

        switch (resp) {
            case 1:
                System.out.println("Digite o id do usuário: ");
                long id = vs.nextLong();
                vs.nextLine();
                userRepository.findUser(id);
                break;
            case 2:
                userRepository.listAllUsers();
                break;
            default:
                System.out.println("Opção inválida!");
        }


    }

    // Método para validar e criar a senha do usuário.
    public String password (Scanner vs) {
        boolean repeat = true;
        String password = "";

         while (repeat) {
            System.out.println("Digite a senha do usuário: ");
            password = vs.nextLine();

            System.out.println("Confirme a senha criada: ");
            String confirmPassword = vs.nextLine();

            if (password.equals(confirmPassword)) {
                System.out.println("Senha criada com sucesso!");
                repeat = false;
                return password;
            } else {
                System.out.println("As senhas não coincidem. Tente novamente.");
            }
        }
         return password;
    }











}
