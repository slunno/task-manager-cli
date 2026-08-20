package org.example.controller;

import org.example.dto.user.CreateUserRequest;
import org.example.dto.user.LoginRequest;
import org.example.models.UserModel;
import org.example.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Recebe as informações pelo protocolo HTTP e retorna um novo usuário.
    @PostMapping
    public ResponseEntity<UserModel> createUser(
            @RequestBody CreateUserRequest request
    ) {

        UserModel user = userService.createUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(user);
    }



    // Recebe as informações pelo protocolo HTTP e deleta um usuário

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable long id
            ) {

        userService.deleteUser(id);

        return ResponseEntity.noContent().build();
    }

    // Recebe o ID do usuário pelo protocolo HTTP e retorna as informações do usuário

    @GetMapping("/{id}")
    public ResponseEntity<UserModel> findUser(
            @PathVariable long id
    ) {

        UserModel user = userService.findUser(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(user);
    }


    // Retorna uma lista de usuários.

    @GetMapping
    public ResponseEntity<java.util.List<UserModel>> listUsers() {

        java.util.List<UserModel> users = userService.listUsers();

        return ResponseEntity.ok(users);
    }

    // Faz o login do usuário.

    @PostMapping("/login")
    public ResponseEntity<Void> loginUser(
            @RequestBody LoginRequest request
    ) {

        userService.loginUser(request);

        return ResponseEntity.ok().build();
    }
}