package org.example.exception.task;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TaskValidatorTest {

    private final TaskValidator validator = new TaskValidator();

    @Test
    void deveLancarExcecaoQuandoTituloEstaVazio() {
        TaskException ex = assertThrows(TaskException.class,
                () -> validator.titleValidator("", "descrição válida"));
        assertEquals("Title cannot be blank", ex.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoDescricaoEstaVazia() {
        TaskException ex = assertThrows(TaskException.class,
                () -> validator.titleValidator("Título válido", ""));
        assertEquals("Description cannot be blank", ex.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoTituloExcede40Caracteres() {
        String tituloLongo = "a".repeat(41);
        TaskException ex = assertThrows(TaskException.class,
                () -> validator.titleValidator(tituloLongo, "descrição válida"));
        assertEquals("Title cannot be longer than 40 characters", ex.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoDescricaoExcede200Caracteres() {
        String descricaoLonga = "a".repeat(201);
        TaskException ex = assertThrows(TaskException.class,
                () -> validator.titleValidator("Título válido", descricaoLonga));
        assertEquals("Description cannot be longer than 200 characters", ex.getMessage());
    }

    @Test
    void naoDeveLancarExcecaoQuandoDadosSaoValidos() {
        assertDoesNotThrow(() -> validator.titleValidator("Título válido", "Descrição válida"));
    }
}