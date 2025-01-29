package com.brand.backend.models;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}
