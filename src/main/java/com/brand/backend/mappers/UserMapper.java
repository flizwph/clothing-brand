package com.brand.backend.mappers;

import com.brand.backend.dtos.UserDTO;
import com.brand.backend.models.User;

public class UserMapper {
    public static UserDTO toDTO(User user) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setUsername(user.getUsername());
        userDTO.setEmail(user.getEmail());
        userDTO.setRole(user.getRole());
        userDTO.setActive(user.isActive());
        userDTO.setEmailVerified(user.isEmailVerified());
        userDTO.setCreatedAt(user.getCreatedAt());
        userDTO.setUpdatedAt(user.getUpdatedAt());
        return userDTO;
    }

    public static User toEntity(UserDTO userDTO) {
        User user = new User();
        user.setId(userDTO.getId());
        user.setUsername(userDTO.getUsername());
        user.setEmail(userDTO.getEmail());
        user.setRole(userDTO.getRole());
        user.setActive(userDTO.isActive());
        user.setEmailVerified(userDTO.isEmailVerified());
        return user;
    }
}
