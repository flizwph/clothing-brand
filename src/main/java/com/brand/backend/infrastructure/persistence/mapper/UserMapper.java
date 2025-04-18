package com.brand.backend.infrastructure.persistence.mapper;

import com.brand.backend.presentation.dto.request.UserDTO;
import com.brand.backend.domain.user.model.User;

public class UserMapper {
    public static UserDTO toDTO(User user) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setUsername(user.getUsername());
        userDTO.setRole(user.getRole());
        userDTO.setActive(user.isActive());
        userDTO.setTelegramId(user.getTelegramId());
        userDTO.setCreatedAt(user.getCreatedAt());
        userDTO.setUpdatedAt(user.getUpdatedAt());
        return userDTO;
    }

    public static User toEntity(UserDTO userDTO) {
        User user = new User();
        user.setId(userDTO.getId());
        user.setUsername(userDTO.getUsername());
        user.setRole(userDTO.getRole());
        user.setActive(userDTO.isActive());
        user.setTelegramId(userDTO.getTelegramId());
        return user;
    }
}
