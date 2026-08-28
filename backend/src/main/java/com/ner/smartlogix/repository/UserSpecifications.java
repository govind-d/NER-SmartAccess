package com.ner.smartlogix.repository;

import com.ner.smartlogix.entity.Role;
import com.ner.smartlogix.entity.User;
import com.ner.smartlogix.enums.RoleName;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Reusable query fragments for the admin user list.
 *
 * <p>Why not one JPQL query with {@code (:role IS NULL OR ...)} repeated three times:
 * that pattern is hard to read, and passing a null enum parameter to PostgreSQL is a
 * classic source of "could not determine data type" errors. A Specification is built in
 * Java, so a filter that was not supplied simply is not added to the SQL at all.
 */
public final class UserSpecifications {

    private UserSpecifications() {
    }

    /** Neutral starting point - matches every row, so filters can be chained onto it. */
    public static Specification<User> all() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static Specification<User> hasRole(RoleName roleName) {
        return (root, query, cb) -> {
            // A user can hold several roles, so the join could duplicate rows.
            query.distinct(true);
            Join<User, Role> roles = root.join("roles");
            return cb.equal(roles.get("name"), roleName);
        };
    }

    public static Specification<User> inDistrict(Long districtId) {
        return (root, query, cb) -> cb.equal(root.get("district").get("id"), districtId);
    }

    /** Case-insensitive contains-search over username, full name and email. */
    public static Specification<User> matchesText(String text) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(text)) {
                return cb.conjunction();
            }
            String pattern = "%" + text.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("username")), pattern),
                    cb.like(cb.lower(root.get("fullName")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern));
        };
    }
}
