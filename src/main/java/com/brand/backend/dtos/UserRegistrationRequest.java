package com.brand.backend.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserRegistrationRequest {

    @NotBlank
    private String username;

    @NotBlank
    private String password;
}
