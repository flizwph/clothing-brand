package com.brand.backend.services;

import com.brand.backend.models.User;
import com.brand.backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;


    public User registerUser(User user) {

        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());


        String verificationCode = generateVerificationCode();
        user.setVerificationCode(verificationCode);
        user.setEmailVerified(false);

        User createdUser = userRepository.save(user);

        emailService.sendVerificationEmail(user.getEmail(), verificationCode);

        return createdUser;
    }


    public Optional<User> authenticateUser(String email, String password) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent() && passwordEncoder.matches(password, userOptional.get().getPasswordHash())) {
            return userOptional;
        } else {
            return Optional.empty();
        }
    }


    public boolean verifyUser(String email, String code) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (user.getVerificationCode().equals(code)) {
                user.setEmailVerified(true);
                user.setVerificationCode(null);
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }

    private String generateVerificationCode() {
        return org.apache.commons.lang3.RandomStringUtils.randomNumeric(6);
    }
}
