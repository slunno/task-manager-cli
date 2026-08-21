package org.example.dto.user;

import org.example.models.UserModel;

public class UserResponse {

    private Long idUser;
    private String name;
    private String email;

    public UserResponse() {
    }

    public UserResponse(Long idUser, String name, String email) {
        this.idUser = idUser;
        this.name = name;
        this.email = email;
    }

    public static UserResponse fromModel(UserModel user) {
        return new UserResponse(user.getIdUser(), user.getName(), user.getEmail());
    }

    public Long getIdUser() {
        return idUser;
    }

    public void setIdUser(Long idUser) {
        this.idUser = idUser;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
