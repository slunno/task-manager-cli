package org.example.controller;

import org.example.dto.user.CreateUserRequest;
import org.example.dto.user.DeleteRequest;
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
            @RequestBody DeleteRequest delete
            ) {

        userService.deleteUser(delete);

        return ResponseEntity.noContent().build();
    }

    // Recebe o ID do usuário pelo protocolo HTTP e retorna as informações do usuário

    @GetMapping("/{id}")
    public ResponseEntity<Void> findUser(
            @PathVariable long id
    ) {

        userService.findUser(id);

        return ResponseEntity.ok().build();
    }


    // Retorna uma lista de usuários.

    @GetMapping
    public ResponseEntity<Void> listUsers() {

        userService.listUsers();

        return ResponseEntity.ok().build();
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