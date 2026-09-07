package org.example.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "Users_Admins")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UA_ID")
    private Integer uaId;

    @Column(name = "UA_Username", nullable = false)
    private String username;

    @Column(name = "UA_Password", nullable = false)
    private String password;

    @Column(name = "UA_Role", nullable = false)
    private String role;

    @Column(name = "UA_Email", unique = true, nullable = false)
    private String email;

    @Column(name = "UA_CreateDate", insertable = false, updatable = false)
    private LocalDateTime createDate;
}