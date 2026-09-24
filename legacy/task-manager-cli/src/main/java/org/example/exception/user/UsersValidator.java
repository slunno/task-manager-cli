package org.example.exception.user;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class UsersValidator {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");


        public void userValidator (String name, String email, String password) {

            if (name.isBlank()) {
                throw new UserException("O nome do usuário não pode ser vazio.");
            }
            if (email.isBlank()) {
                throw new UserException("O e-mail do usuário não pode ser vazio.");
            }

            if (name.length() > 50) {
                throw new UserException("O nome do usuário não pode ter mais de 50 caracteres.");
            }
            if (email.length() > 100) {
                throw new UserException("O e-mail do usuário não pode ter mais de 100 caracteres.");
            }

            if (!EMAIL_PATTERN.matcher(email).matches()) {
                throw new UserException("O e-mail informado não é válido.");
            }

            if (password == null || password.isBlank()) {
                throw new UserException("A senha não pode ser vazia.");
            }

            if (password.length() < 8) {
                throw new UserException("A senha deve ter no mínimo 8 caracteres.");
            }

        }
}
