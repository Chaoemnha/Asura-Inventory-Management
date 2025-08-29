package com.inventory.service;

import com.inventory.entity.User;

public interface PasswordResetService {
    void updatePassword(String token, String newPassword);
    User findByResetPasswordToken(String token);
    void updateResetPasswordToken(String email, String token);
    void sendResetPasswordEmail(String toEmail, String resetLink);
}
