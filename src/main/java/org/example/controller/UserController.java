package org.example.controller;

import org.example.dto.user.CreateUserRequest;
import org.example.dto.user.LoginRequest;
import org.example.dto.user.LoginResponse;
import org.example.dto.user.UserResponse;
import org.example.models.UserModel;
import org.example.security.JwtUtil;
import org.example.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public UserController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    // Recebe as informações pelo protocolo HTTP e retorna um novo usuário.

    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @RequestBody CreateUserRequest request
    ) {

        UserResponse user = userService.createUser(request);

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
    public ResponseEntity<UserResponse> findUser(
            @PathVariable long id
    ) {

        UserResponse user = userService.findUser(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(user);
    }


    // Retorna uma lista de usuários.

    @GetMapping
    public ResponseEntity<List<UserResponse>> listUsers() {

        List<UserResponse> users = userService.listUsers();

        return ResponseEntity.ok(users);
    }

    // Faz o login do usuário e retorna um token JWT.

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginUser(
            @RequestBody LoginRequest request
    ) {

        UserModel user = userService.loginUser(request);
        String token = jwtUtil.generateToken(user.getEmail());
        UserResponse userResponse = UserResponse.fromModel(user);

        return ResponseEntity.ok(new LoginResponse(token, userResponse));
    }
}