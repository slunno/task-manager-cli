package org.example.repository;

import org.example.config.DatabaseConfig;
import org.example.models.TaskModel;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class TaskRepository {

        public TaskModel createTask(TaskModel taskModel) {

            String sql = """
                INSERT INTO tasks (title, description, priority)
                VALUES (?, ?, ?)
                RETURNING id
                """;

            try (Connection connection = DatabaseConfig.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {


                statement.setString(1, taskModel.getTitle());
                statement.setString(2, taskModel.getDescription());
                statement.setString(3, taskModel.getPriority());

                var resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    long taskId = resultSet.getLong("id");
                    taskModel.setTaskId(taskId);
                }

                return taskModel;

            } catch (SQLException e) {
                System.out.println("Erro ao criar tarefa:");
                System.out.println(e.getMessage());
                return null;
            }
        }

        public void deleteTask (long id) {
            String sql = "DELETE FROM tasks WHERE id = ?";

            try (Connection connection = DatabaseConfig.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {


                int rowsAffected = statement.executeUpdate();

                if (rowsAffected > 0) {
                    statement.setLong(1, id);
                    statement.executeUpdate();
                    System.out.println("Tarefa excluída com sucesso!");
                } else {
                    System.out.println("Tarefa não encontrada!");
                }
            } catch (SQLException e) {
                System.out.println("Erro ao excluir tarefa:");
                System.out.println(e.getMessage());
            }
        }

        public TaskModel findTask(long id) {
            // Implementar a lógica para localizar uma tarefa no banco de dados


            String sql = """
                    SELECT id, title, description, priority
                    FROM tasks
                    WHERE id = ?
                    """;

            try (Connection connection = DatabaseConfig.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setLong(1, id);

                var resultSet = statement.executeQuery();

                if (resultSet.next()) {

                    long taskId = resultSet.getLong("id");
                    String title = resultSet.getString("title");
                    String description = resultSet.getString("description");
                    String priority = resultSet.getString("priority");

                    return new TaskModel(
                            taskId,
                            title,
                            description,
                            priority
                    );
                }

            } catch (SQLException e) {
                System.out.println("Erro ao localizar tarefa:");
                System.out.println(e.getMessage());
            }

            return null; // Retornar null se não encontrar a tarefa
        }

        public List<TaskModel> listAllTasks () {
            // Implementar a lógica para listar todas as tarefas no banco de dados.

            List<TaskModel> tasks = new ArrayList<>();

            String sql = """
                    SELECT id, title, description, priority
                    FROM tasks
                    """;


            try (Connection connection = DatabaseConfig.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {

                var resultSet = statement.executeQuery();

                System.out.println("Lista de tarefas:");
                while (resultSet.next()) {
                    long taskId = resultSet.getLong("id");
                    String title = resultSet.getString("title");
                    String description = resultSet.getString("description");
                    String priority = resultSet.getString("priority");

                    TaskModel task = new TaskModel(taskId, title, description, priority);
                    tasks.add(task);
                }

            } catch (SQLException e) {
                System.out.println("Erro ao listar tarefas:" + e.getMessage());
            }


            return tasks; // Retornar null ou uma lista vazia, dependendo da implementação desejada
        }

    }


