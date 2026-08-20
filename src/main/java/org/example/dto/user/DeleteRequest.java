package org.example.dto.user;

public class DeleteRequest {

    private long id;

    public DeleteRequest(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
