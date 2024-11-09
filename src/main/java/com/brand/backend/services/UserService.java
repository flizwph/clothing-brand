package com.brand.backend.services;

import com.brand.backend.dtos.UserDTO;
import com.brand.backend.models.User;
import com.brand.backend.repositories.UserRepository;
import com.brand.backend.exceptions.UserAlreadyExistsException;
import com.brand.backend.mappers.UserMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    public List<UserDTO> getAllUsers() {
        logger.info("Fetching all users");
        return userRepository.findAll().stream()
                .map(UserMapper::toDTO)
                .collect(Collectors.toList());
    }

    public UserDTO getUserById(Long id) {
        logger.info("Fetching user by id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User with id " + id + " not found"));
        return UserMapper.toDTO(user);
    }

    @Transactional
    public UserDTO createUser(UserDTO userDTO) {
        logger.info("Creating user with email: {}", userDTO.getEmail());

        if (userRepository.findByEmail(userDTO.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("User with email " + userDTO.getEmail() + " already exists");
        }

        User user = UserMapper.toEntity(userDTO);
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        prepareUserForSave(user);
        User savedUser = userRepository.save(user);
        logger.info("User with email {} successfully created", user.getEmail());
        return UserMapper.toDTO(savedUser);
    }

    @Transactional
    public UserDTO updateUser(UserDTO userDTO) {
        logger.info("Updating user with id: {}", userDTO.getId());
        User user = userRepository.findById(userDTO.getId())
                .orElseThrow(() -> new IllegalArgumentException("User with id " + userDTO.getId() + " not found"));
        user.setEmail(userDTO.getEmail());
        user.setRole(userDTO.getRole());
        user.setActive(userDTO.isActive());
        user.setEmailVerified(userDTO.isEmailVerified());
        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);
        logger.info("User with id {} successfully updated", user.getId());
        return UserMapper.toDTO(updatedUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        logger.info("Deleting user with id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User with id " + id + " not found"));
        userRepository.deleteById(user.getId());
        logger.info("User with id {} successfully deleted", id);
    }

    @Transactional
    public void setResetToken(Long userId, String token) {
        logger.info("Setting reset token for user with id: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User with id " + userId + " not found"));
        user.setResetToken(token);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        logger.info("Reset token set for user with id: {}", userId);
    }

    @Transactional
    public void verifyEmail(Long userId) {
        logger.info("Verifying email for user with id: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User with id " + userId + " not found"));
        user.setEmailVerified(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        logger.info("Email verified for user with id: {}", userId);
    }

    @Transactional
    public void setLastLogin(Long userId) {
        logger.info("Setting last login for user with id: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User with id " + userId + " not found"));
        user.setLastLogin(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        logger.info("Last login set for user with id: {}", userId);
    }

    private void prepareUserForSave(User user) {
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setRole(user.getRole() != null ? user.getRole() : "customer");
        user.setActive(true);
        user.setEmailVerified(false);
    }
}
