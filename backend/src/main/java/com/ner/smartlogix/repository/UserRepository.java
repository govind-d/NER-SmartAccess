package com.ner.smartlogix.repository;

import com.ner.smartlogix.entity.User;
import com.ner.smartlogix.enums.RoleName;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data generates the implementation of every method below at startup from its
 * NAME, which is why there is no UserRepositoryImpl anywhere in the project.
 */
public interface UserRepository extends JpaRepository<User, Long>,
        JpaSpecificationExecutor<User> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /** Used by the alert fan-out: notify everyone holding a given role. */
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name = :roleName AND u.enabled = true")
    List<User> findEnabledByRole(@Param("roleName") RoleName roleName);

    List<User> findByDistrictId(Long districtId);
}
