package com.julenka.api.primitiveBank.domain;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Data
@Entity
@Table(name = "userinfo")
public class UserInfo {

    @Id
    @GeneratedValue
    private Long id;

    @OneToOne(mappedBy = "userInfo")
    private User user;

    @NotNull
    @Column(name = "firstname")
    private String firstName;

    @NotNull
    @Column(name = "lastName")
    private String lastName;

}
