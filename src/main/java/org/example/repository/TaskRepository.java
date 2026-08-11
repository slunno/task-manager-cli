package org.example.repository;

import org.example.config.DatabaseConfig;
import org.example.models.TaskModel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

    public class TaskRepository {

        public void createTask(TaskModel taskModel) {

            String sql = """
                INSERT INTO tasks (title, description, priority)
                VALUES (?, ?, ?)
                """;

            try (Connection connection = DatabaseConfig.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, taskModel.getTitle());
                statement.setString(2, taskModel.getDescription());
                statement.setString(3, taskModel.getPriority());

                statement.executeUpdate();

                System.out.println("Tarefa criada com sucesso!");

            } catch (SQLException e) {
                System.out.println("Erro ao criar tarefa:");
                System.out.println(e.getMessage());
            }
        }

        public void deleteTask (int id) {
            String sql = "DELETE FROM tasks WHERE id = ?";

            try (Connection connection = DatabaseConfig.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setInt(1, id);
                statement.executeUpdate();
                System.out.println("Tarefa excluída com sucesso!");

            } catch (SQLException e) {
                System.out.println("Erro ao excluir tarefa:");
                System.out.println(e.getMessage());
            }
        }

        public void localizarTask(int id) {
            // Implementar a lógica para localizar uma tarefa no banco de dados


            String sql = """
                    SELECT * FROM tasks WHERE id = ?
                    """;

            try (Connection connection = DatabaseConfig.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setInt(1, id);

                var resultSet = statement.executeQuery();

                if (resultSet.next()) {
                    System.out.println("Tarefa encontrada:");
                    System.out.println("ID: " + resultSet.getInt("id"));
                    System.out.println("Título: " + resultSet.getString("title"));
                    System.out.println("Descrição: " + resultSet.getString("description"));
                    System.out.println("Prioridade: " + resultSet.getString("priority"));
                } else {
                    System.out.println("Tarefa não encontrada.");
                }

            } catch (SQLException e) {
                System.out.println("Erro ao localizar tarefa:");
                System.out.println(e.getMessage());
            }
        }

        public void listAllTasks () {
            // Implementar a lógica para listar todas as tarefas no banco de dados.

            String sql = """
                    SELECT * FROM tasks
                    """;


            try (Connection connection = DatabaseConfig.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {

                var resultSet = statement.executeQuery();

                System.out.println("Lista de tarefas:");
                while (resultSet.next()) {
                    System.out.println("ID: " + resultSet.getInt("id"));
                    System.out.println("Título: " + resultSet.getString("title"));
                    System.out.println("Descrição: " + resultSet.getString("description"));
                    System.out.println("Prioridade: " + resultSet.getString("priority"));
                    System.out.println("------------------------");
                }

            } catch (SQLException e) {
                System.out.println("Erro ao listar tarefas:" + e.getMessage());
            }


        }

    }


