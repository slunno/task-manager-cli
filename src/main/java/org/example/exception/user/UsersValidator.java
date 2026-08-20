package org.example.exception.user;

import org.springframework.stereotype.Component;

@Component
public class UsersValidator {


        public void userValidator (String name, String email) {

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

            if (!email.contains("@") || !email.contains(".")) {
                throw new UserException("O e-mail do usuário deve conter '@' e '.'");
            }

        }

        public void userDeleteValidator (long id) {

        }
















}
