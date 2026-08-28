package com.ner.smartlogix.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An application user: administrator, authority official, logistics manager,
 * field officer or driver.
 *
 * <p>The table is called {@code users} because {@code user} is a reserved word in
 * PostgreSQL and every query would otherwise need quoting.
 *
 * <p>Note what is NOT here: no plain password, ever. Only {@code passwordHash},
 * produced by BCrypt.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id", callSuper = false)
@Entity
@Table(name = "users")
public class User extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(length = 15)
    private String phone;

    /**
     * Home district. LAZY so that loading a user does not silently drag in a district
     * boundary polygon (which can be hundreds of kilobytes).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id")
    private District district;

    @Column(nullable = false)
    private boolean enabled = true;

    /**
     * EAGER on purpose: Spring Security needs the roles in the same breath as the user
     * during authentication, and there are at most five of them.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    public void addRole(Role role) {
        this.roles.add(role);
    }
}
