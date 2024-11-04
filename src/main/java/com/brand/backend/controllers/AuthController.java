package com.brand.backend.controllers;

import com.brand.backend.models.User;
import com.brand.backend.services.AuthService;
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
    public ResponseEntity<String> registerUser(@RequestBody User user) {
        User createdUser = authService.registerUser(user);
        return ResponseEntity.ok("User registered successfully. Please verify your email.");
    }

    @PostMapping("/login")
    public ResponseEntity<String> loginUser(@RequestParam String email, @RequestParam String password) {
        Optional<User> userOptional = authService.authenticateUser(email, password);
        if (userOptional.isPresent()) {
            if (!userOptional.get().isEmailVerified()) {
                return ResponseEntity.status(403).body("Please verify your email before logging in.");
            }
            return ResponseEntity.ok("Login successful");
        } else {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }
    
    @PostMapping("/verify")
    public ResponseEntity<String> verifyUser(@RequestParam String email, @RequestParam String code) {
        boolean isVerified = authService.verifyUser(email, code);
        if (isVerified) {
            return ResponseEntity.ok("Account verified successfully");
        } else {
            return ResponseEntity.status(400).body("Invalid verification code");
        }
    }
}
