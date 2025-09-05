package com.inventory.service.impl;

import com.inventory.entity.User;
import com.inventory.exceptions.NotFoundException;
import com.inventory.repository.UserRepository;
import com.inventory.service.PasswordResetService;
import com.inventory.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Service
public class PasswordServiceImpl implements PasswordResetService {
    private final UserRepository userRepository;
    private final JavaMailSender javaMailSender;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    @Override
    public void updatePassword(String token, String newPassword) {
        User user = userRepository.findByResetPasswordToken(token);
        if (user != null) {
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setResetPasswordToken(null);
            user.setRsTokenCrDate(null);
            userRepository.save(user);
        }
    }

    @Override
    public User findByResetPasswordToken(String token) {
        return userRepository.findByResetPasswordToken((token));
    }

    @Override
    public void updateResetPasswordToken(String email, String token) {
        User existingUser = userRepository.findByEmail(email).orElseThrow(()-> new NotFoundException("User Not Found"));
        existingUser.setResetPasswordToken(token);
        existingUser.setRsTokenCrDate(new Date());
        userRepository.save(existingUser);
    }

    @Override
    public void sendResetPasswordEmail(String toEmail, String resetLink) {
        SimpleMailMessage  mailMessage = new SimpleMailMessage();
        mailMessage.setTo(toEmail);
        mailMessage.setSubject("Reset Password");
        mailMessage.setText("To reset your password, click the link below:\n" + resetLink +
                "\nThis link is valid for 1 hour.");
        mailMessage.setFrom("luannguyentm99@gmail.com");
        javaMailSender.send(mailMessage);
    }

    public void processForgotPassword(String email, String baseUrl) {
        String token = UUID.randomUUID().toString();
        updateResetPasswordToken(email, token);
        String resetLink = baseUrl + "/reset-password?token=" + token;
        sendResetPasswordEmail(email, resetLink);
    }

    public boolean validateResetPasswordToken(String token) {
        User existingUser = userRepository.findByResetPasswordToken(token);
        if (existingUser == null) {
            return false;
        }
        Date createdDate = existingUser.getRsTokenCrDate();
        long diffInMillies = Math.abs(new Date().getTime() - createdDate.getTime());
        long diffInHours = TimeUnit.MILLISECONDS.toHours(diffInMillies);
        return diffInHours <=1;
    }
}
