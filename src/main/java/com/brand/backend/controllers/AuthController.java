package com.brand.backend.controllers;

import com.brand.backend.dtos.UserRegistrationRequest;
import com.brand.backend.models.User;
import com.brand.backend.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody @Valid UserRegistrationRequest request) {

        User user = new User();
        user.setUsername(request.getUsername());
        user.setVerified(false);

        authService.registerUser(user, request.getPassword());

        System.out.println("Verification code for user " + user.getUsername() + ": " + user.getVerificationCode());

        return ResponseEntity.ok("User registered successfully. Check console for verification code.");
    }

    @PostMapping("/login")
    public ResponseEntity<String> loginUser(@RequestParam String username, @RequestParam String password) {
        Optional<User> userOptional = authService.authenticateUser(username, password);
        if (userOptional.isPresent()) {
            if (!userOptional.get().isVerified()) {
                return ResponseEntity.status(403).body("Please verify your account before logging in.");
            }
            return ResponseEntity.ok("Login successful");
        } else {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }
}
