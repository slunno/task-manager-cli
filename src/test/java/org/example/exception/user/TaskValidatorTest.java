package org.example.exception.user;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UsersValidatorTest {

    private final UsersValidator validator = new UsersValidator();

    @Test
    void deveLancarExcecaoQuandoNomeEstaVazio() {
        UserException ex = assertThrows(UserException.class,
                () -> validator.userValidator("", "email@teste.com"));
        assertEquals("O nome do usuário não pode ser vazio.", ex.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoEmailNaoContemArroba() {
        UserException ex = assertThrows(UserException.class,
                () -> validator.userValidator("Nome Válido", "emailinvalido.com"));
        assertEquals("O e-mail do usuário deve conter '@' e '.'", ex.getMessage());
    }

    @Test
    void naoDeveLancarExcecaoQuandoDadosSaoValidos() {
        assertDoesNotThrow(() -> validator.userValidator("Nome Válido", "email@teste.com"));
    }
}