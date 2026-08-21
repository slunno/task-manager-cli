package org.example.service;

import org.example.dto.task.CreateTaskRequest;
import org.example.exception.task.TaskValidator;
import org.example.models.TaskModel;
import org.example.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    private TaskValidator taskValidator;
    private TaskService taskService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        taskValidator = new TaskValidator(); // é uma classe simples, não precisa mockar
        taskService = new TaskService(taskRepository, taskValidator);
    }

    @Test
    void deveCriarTarefaComSucesso() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Estudar Java");
        request.setDescription("Praticar testes unitários");
        request.setPriority("Alta");

        TaskModel tarefaSalva = new TaskModel(1L, "Estudar Java", "Praticar testes unitários", "Alta");

        // Quando o repositório for chamado com qualquer TaskModel, devolve a tarefa "salva"
        when(taskRepository.createTask(any(TaskModel.class))).thenReturn(tarefaSalva);

        TaskModel resultado = taskService.createTask(request);

        assertNotNull(resultado);
        assertEquals("Estudar Java", resultado.getTitle());
        verify(taskRepository, times(1)).createTask(any(TaskModel.class));
    }

    @Test
    void naoDeveChamarRepositorioQuandoValidacaoFalha() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle(""); // título inválido
        request.setDescription("Descrição válida");
        request.setPriority("Alta");

        assertThrows(RuntimeException.class, () -> taskService.createTask(request));

        // Garante que o repositório NUNCA foi chamado, já que a validação falhou antes
        verify(taskRepository, never()).createTask(any(TaskModel.class));
    }

    @Test
    void deveDeletarTarefa() {
        taskService.deleteTask(5L);

        verify(taskRepository, times(1)).deleteTask(5L);
    }
}