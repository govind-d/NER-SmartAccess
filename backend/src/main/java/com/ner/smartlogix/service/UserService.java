package com.ner.smartlogix.service;

import com.ner.smartlogix.dto.request.UpdateRolesRequest;
import com.ner.smartlogix.dto.request.UpdateUserRequest;
import com.ner.smartlogix.dto.response.RoleResponse;
import com.ner.smartlogix.dto.response.UserResponse;
import com.ner.smartlogix.enums.RoleName;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Administration of user accounts and their roles. */
public interface UserService {

    /** Paged list; every filter is optional and simply omitted from the SQL when null. */
    Page<UserResponse> search(RoleName role, Long districtId, String text, Pageable pageable);

    UserResponse getById(Long id);

    UserResponse update(Long id, UpdateUserRequest request);

    /** Disabling is preferred over deleting: audit trails must keep pointing somewhere. */
    UserResponse setEnabled(Long id, boolean enabled);

    UserResponse updateRoles(Long id, UpdateRolesRequest request);

    void delete(Long id);

    List<RoleResponse> listRoles();
}
