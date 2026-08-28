package com.ner.smartlogix.entity;

import com.ner.smartlogix.enums.RoleName;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One of the five system roles. Kept as a table (rather than a plain column on the
 * user) so that an administrator can grant several roles to the same person - which
 * happens in practice, e.g. an authority official who is also a logistics manager.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "role")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Stored as text (ADMIN, DRIVER, ...) so the table stays readable in psql. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 30)
    private RoleName name;

    @Column(length = 200)
    private String description;

    public Role(RoleName name, String description) {
        this.name = name;
        this.description = description;
    }
}
