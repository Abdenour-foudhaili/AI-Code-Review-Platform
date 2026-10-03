package com.aicodereview.dto;

import com.aicodereview.entity.Role;
import lombok.Data;

@Data
public class UpdateUserRequest {
    private Role role;
    private Boolean enabled;
}
