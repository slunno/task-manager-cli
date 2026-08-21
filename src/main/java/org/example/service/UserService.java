package org.example.service;

import org.example.dto.user.CreateUserRequest;
import org.example.dto.user.LoginRequest;
import org.example.dto.user.UserResponse;
import org.example.exception.user.UsersValidator;
import org.example.exception.user.UserException;
import org.example.models.UserModel;
import org.example.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UsersValidator usersValidator;

    public UserService(UserRepository userRepository, UsersValidator usersValidator) {
        this.userRepository = userRepository;
        this.usersValidator = usersValidator;
    }

    public UserResponse createUser(CreateUserRequest request) {
        usersValidator.userValidator(request.getName(), request.getEmail(), request.getPassword());
        String passwordHash = BCrypt.hashpw(request.getPassword(), BCrypt.gensalt());
        UserModel user = new UserModel(request.getName(), request.getEmail(), passwordHash);
        userRepository.createUser(user);
        return UserResponse.fromModel(user);
    }

    public void deleteUser(long id) {
        userRepository.deleteUser(id);
    }

    public UserResponse findUser(long id) {
        UserModel user = userRepository.findUser(id);
        if (user == null) {
            return null;
        }
        return UserResponse.fromModel(user);
    }

    public List<UserResponse> listUsers() {
        return userRepository.listAllUsers().stream()
                .map(UserResponse::fromModel)
                .collect(Collectors.toList());
    }

    public UserModel loginUser(LoginRequest loginRequest) {
        UserModel user = userRepository.verifyLoginUsers(loginRequest.getUsername(), loginRequest.getPassword());
        if (user == null) {
            throw new UserException("E-mail ou senha inválidos.");
        }
        return user;
    }
}
