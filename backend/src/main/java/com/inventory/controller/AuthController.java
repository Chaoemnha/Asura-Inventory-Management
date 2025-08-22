package com.inventory.controller;

import com.inventory.dto.LoginRequest;
import com.inventory.dto.RegisterRequest;
import com.inventory.dto.Response;
import com.inventory.service.PasswordResetService;
import com.inventory.service.UserService;
import com.inventory.service.impl.PasswordServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.naming.directory.Attribute;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final PasswordServiceImpl passwordServiceImpl;

    @PostMapping("/register")
    public ResponseEntity<Response> registerUser(@RequestBody @Valid RegisterRequest registerRequest){
        return ResponseEntity.ok(userService.registerUser(registerRequest));
    }

    @PostMapping("/login")
    public ResponseEntity<Response> loginUser(@RequestBody @Valid LoginRequest loginRequest){
        return ResponseEntity.ok(userService.loginUser(loginRequest));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Response> processFogotPassword(@RequestParam("email") String email, HttpServletRequest httpServletRequest){
        try {
            String baseUrl = "http://localhost:4200";
            passwordServiceImpl.processForgotPassword(email, baseUrl);
            return ResponseEntity.ok(Response.builder().status(200).message("A reset link has been sent to your email.").build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().eTag("Email not found or error occurred.").build();
        }
    }

    @GetMapping("/reset-password")
    public ResponseEntity<Response> showResetPasswordPage(@RequestParam("token")  String token){
        if(passwordServiceImpl.validateResetPasswordToken(token)){
            return ResponseEntity.ok(Response.builder().status(200).message("A reset link has been sent to your email.").build());
        }
        return ResponseEntity.badRequest().eTag("Invalid or expired token.").build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Response> processResetPassword(@RequestParam("token")  String token,
                                                    @RequestParam("password") String password){
        if (passwordServiceImpl.validateResetPasswordToken(token)) {
            passwordServiceImpl.updatePassword(token, password);
            return ResponseEntity.ok(Response.builder().status(200).message("Password reset successfully. Please login.").build());
        } else {
            return ResponseEntity.badRequest().eTag("Invalid or expired token.").build();
        }
    }
}
