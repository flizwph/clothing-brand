package com.brand.backend.domain.user.model;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}
