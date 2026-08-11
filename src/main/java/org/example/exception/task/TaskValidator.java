package org.example.exception.task;

public class TaskValidator {

    public void titleValidator (String title, String description, int priority) {

        // Validando se o titulo, descrição e a prioridade não estão vazias.
        if (title.isBlank())
            throw new TaskException("Title cannot be blank");
        if (description.isBlank())
            throw new TaskException("Description cannot be blank");
        if (priority == 0)
            throw new TaskException("Priority cannot be blank");

        // Validando se o titulo e a descrição estão dentro do limite de caracteres.
        if (title.length() > 40)
            throw new TaskException("Title cannot be longer than 40 characters");
        if (description.length() > 200)
            throw new TaskException("Description cannot be longer than 200 characters");

        // Validando se o usuário escolheu entre 1 ou 3 na prioridade.
        if (priority != 1 && priority != 2 && priority != 3)
            throw new TaskException("Priority must be 1, 2 or 3");


    }












}
