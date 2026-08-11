package org.example;


import org.example.menu.login.MenuLogin;
import org.example.repository.UserRepository;
import org.example.service.UserService;

import java.util.Scanner;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        Scanner vs = new Scanner(System.in);
        UserRepository userRepository = new UserRepository();
        UserService userService = new UserService(userRepository);


        MenuLogin login = new MenuLogin();
        login.login(vs, userService);





    }
}