package com.ner.smartlogix.repository;

import com.ner.smartlogix.entity.Role;
import com.ner.smartlogix.enums.RoleName;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);

    boolean existsByName(RoleName name);
}
