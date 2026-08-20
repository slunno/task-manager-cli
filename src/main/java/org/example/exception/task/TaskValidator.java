package org.example.exception.task;

import org.springframework.stereotype.Component;

@Component
public class TaskValidator {

    public void titleValidator (String title, String description) {

        // Validando se o titulo, descrição e a prioridade não estão vazias.
        if (title.isBlank())
            throw new TaskException("Title cannot be blank");
        if (description.isBlank())
            throw new TaskException("Description cannot be blank");


        // Validando se o titulo e a descrição estão dentro do limite de caracteres.
        if (title.length() > 40)
            throw new TaskException("Title cannot be longer than 40 characters");
        if (description.length() > 200)
            throw new TaskException("Description cannot be longer than 200 characters");

    }












}
