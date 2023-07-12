package lk.mbpt.ims.app.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class User implements Serializable {
    private String fullName;
    private String username;
    private String password;
    private Role role;

    public enum Role {
        ADMIN, USER
    }
}


