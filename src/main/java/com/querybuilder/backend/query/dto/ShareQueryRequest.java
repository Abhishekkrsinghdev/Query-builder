package com.querybuilder.backend.query.dto;

import com.querybuilder.backend.query.model.SharePermission;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShareQueryRequest {

    @Email(message = "Invalid email format")
    private String userEmail;  // NULL for public sharing

    @NotNull(message = "Permission is required")
    private SharePermission permission;
}