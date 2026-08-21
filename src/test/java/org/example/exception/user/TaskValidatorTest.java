package org.example.exception.user;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UsersValidatorTest {

    private final UsersValidator validator = new UsersValidator();

    @Test
    void deveLancarExcecaoQuandoNomeEstaVazio() {
        UserException ex = assertThrows(UserException.class,
                () -> validator.userValidator("", "email@teste.com", "senha12345"));
        assertEquals("O nome do usuário não pode ser vazio.", ex.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoEmailNaoContemArroba() {
        UserException ex = assertThrows(UserException.class,
                () -> validator.userValidator("Nome Válido", "emailinvalido.com", "senha12345"));
        assertEquals("O e-mail informado não é válido.", ex.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoEmailEhApenasPontoEArroba() {
        UserException ex = assertThrows(UserException.class,
                () -> validator.userValidator("Nome Válido", "@.", "senha12345"));
        assertEquals("O e-mail informado não é válido.", ex.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoEmailTemDuplosPontos() {
        UserException ex = assertThrows(UserException.class,
                () -> validator.userValidator("Nome Válido", "a@b..", "senha12345"));
        assertEquals("O e-mail informado não é válido.", ex.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoEmailSemDominio() {
        UserException ex = assertThrows(UserException.class,
                () -> validator.userValidator("Nome Válido", "user@", "senha12345"));
        assertEquals("O e-mail informado não é válido.", ex.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoSenhaEstaVazia() {
        UserException ex = assertThrows(UserException.class,
                () -> validator.userValidator("Nome Válido", "email@teste.com", ""));
        assertEquals("A senha não pode ser vazia.", ex.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoSenhaEhNula() {
        UserException ex = assertThrows(UserException.class,
                () -> validator.userValidator("Nome Válido", "email@teste.com", null));
        assertEquals("A senha não pode ser vazia.", ex.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoSenhaTemMenosDe8Caracteres() {
        UserException ex = assertThrows(UserException.class,
                () -> validator.userValidator("Nome Válido", "email@teste.com", "1234567"));
        assertEquals("A senha deve ter no mínimo 8 caracteres.", ex.getMessage());
    }

    @Test
    void naoDeveLancarExcecaoQuandoDadosSaoValidos() {
        assertDoesNotThrow(() -> validator.userValidator("Nome Válido", "email@teste.com", "senha12345"));
    }
}