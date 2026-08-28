package com.ner.smartlogix.enums;

/**
 * The five user roles of the platform.
 * Spring Security expects authorities to be prefixed with "ROLE_",
 * so we store the bare name here and add the prefix in the security layer.
 */
public enum RoleName {
    ADMIN,
    AUTHORITY_OFFICIAL,
    FIELD_OFFICER,
    LOGISTICS_MANAGER,
    DRIVER
}
