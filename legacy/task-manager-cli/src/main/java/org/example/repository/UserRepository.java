package org.example.repository;

import org.example.config.DatabaseConfig;
import org.example.models.UserModel;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Repository
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

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao criar usuário: " + e.getMessage(), e);
        }




    }

    public void deleteUser (long id) {

        String sql = """
                DELETE FROM users WHERE id_user = ?
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            int rowsAffected = statement.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Usuário excluído com sucesso!");
            } else {
                System.out.println("Usuário não encontrado!");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao excluir usuário: " + e.getMessage(), e);
        }

    }

    public UserModel findUser (long idUser) {

        String sql = """
                 SELECT * FROM users WHERE id_user = ?
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql)) {

            statement.setLong(1, idUser);

            var resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return new UserModel(
                        resultSet.getLong("id_user"),
                        resultSet.getString("name"),
                        resultSet.getString("email"),
                        resultSet.getString("password")
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar usuário: " + e.getMessage(), e);
        }
        return null;
    }

    public List<UserModel> listAllUsers () {
        List<UserModel> users = new java.util.ArrayList<>();
        String sql = """
                SELECT * FROM users
                """;
        try (Connection connection = DatabaseConfig.getConnection();
             var statement = connection.prepareStatement(sql)) {

            var resultSet = statement.executeQuery();

            while (resultSet.next()) {
                users.add(new UserModel(
                        resultSet.getLong("id_user"),
                        resultSet.getString("name"),
                        resultSet.getString("email"),
                        resultSet.getString("password")
                ));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar usuários: " + e.getMessage(), e);
        }
        return users;
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


//    public UserModel findUserByEmail(String email) {
//        return null;
//    }
}
