package org.example.service;

import org.example.dto.user.CreateUserRequest;
import org.example.dto.user.LoginRequest;
import org.example.exception.user.UsersValidator;
import org.example.exception.user.UserException;
import org.example.models.UserModel;
import org.example.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UsersValidator usersValidator;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.usersValidator = new UsersValidator();
    }

    public UserModel createUser (CreateUserRequest request) throws UserException {

        try {
            usersValidator.userValidator(request.getName(), request.getEmail());
            String passwordHash = BCrypt.hashpw(request.getPassword(), BCrypt.gensalt());
            UserModel user = new UserModel(request.getName(), request.getEmail(), passwordHash);
            userRepository.createUser(user);
            return user;
        }catch (UserException e){
            throw new UserException(e.getMessage());
        }

    }

    public void deleteUser (long id) throws UserException {

        userRepository.deleteUser(id);

    }

    public UserModel findUser(long id) {
        return userRepository.findUser(id);
    }

    public java.util.List<UserModel> listUsers() {
        return userRepository.listAllUsers();
    }

    public void loginUser(LoginRequest loginRequest) throws UserException {
        UserModel user = userRepository.findUserByEmail(loginRequest.getUsername());
        if (user == null || !BCrypt.checkpw(loginRequest.getPassword(), user.getPassword())) {
            throw new UserException("");
        }
        // Aqui você pode implementar a lógica de sessão ou token para o usuário logado.
    }











}
