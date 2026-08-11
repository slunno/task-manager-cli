package org.example.repository;

import org.example.config.DatabaseConfig;
import org.example.models.UserModel;
import org.mindrot.jbcrypt.BCrypt;

import javax.xml.crypto.Data;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UserRepository {

    public void createUser (UserModel userModel) {

        String sql = """
            INSERT INTO users (name, email, password)
            VALUES (?, ?, ?)
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql)) {

            statement.setString(1, userModel.getName());
            statement.setString(2, userModel.getEmail());
            statement.setString(3, userModel.getPassword());

            statement.executeUpdate();

            System.out.println("Usuário criado com sucesso!");

        } catch (Exception e) {
            System.out.println("Erro ao criar usuário:");
            System.out.println(e.getMessage());
        }




    }

    public void deleteUser (long id) {

        String sql = """
                DELETE FROM users WHERE id_user = ?
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id); // Substitua "id" pelo valor real do ID do usuário que deseja excluir

            statement.executeUpdate();

            int rowsAffected = statement.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Usuário excluído com sucesso!");
            } else {
                System.out.println("Usuário não encontrado!");
            }

        } catch (Exception e) {
            System.out.println("Erro ao excluir usuário:");
            System.out.println(e.getMessage());
        }



    }

    public void findUser (long idUser) {

        String sql = """
                 SELECT * FROM users WHERE idUser = ?
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql)) {

            statement.setLong(1, idUser);

            var resultSet = statement.executeQuery();

            if (resultSet.next()) {
                System.out.println("Usuário encontrado:");
                System.out.println("Nome: " + resultSet.getString("name"));
                System.out.println("Email: " + resultSet.getString("email"));
            } else {
                System.out.println("Usuário não encontrado.");
            }

        } catch (Exception e) {
            System.out.println("Erro ao buscar usuário:");
            System.out.println(e.getMessage());
        }

    }

    public void listAllUsers () {

        String sql = """
                SELECT * FROM users
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql)) {

            var resultSet = statement.executeQuery();

            System.out.println("Lista de usuários:");
            while (resultSet.next()) {
                System.out.println("ID: " + resultSet.getString("id_user"));
                System.out.println("Nome: " + resultSet.getString("name"));
                System.out.println("Email: " + resultSet.getString("email"));
                System.out.println("--------------------");
            }

        } catch (Exception e) {
            System.out.println("Erro ao listar usuários:");
            System.out.println(e.getMessage());
        }

    }

    public UserModel verifyLoginUsers(String email, String password) {

        String sql = """
            SELECT  name, email, password
            FROM users
            WHERE email = ?
            """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, email);

            var resultSet = statement.executeQuery();

            if (resultSet.next()) {

                String passwordHash = resultSet.getString("password");

                if (BCrypt.checkpw(password, passwordHash)) {

                    return new UserModel(
                            resultSet.getString("name"),
                            resultSet.getString("email"),
                            resultSet.getString("password")
                    );
                }
            }

            return null;

        } catch (SQLException e) {

            System.out.println("Erro ao verificar login:");
            System.out.println(e.getMessage());

            return null;
        }
    }


}
