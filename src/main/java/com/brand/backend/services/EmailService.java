package com.brand.backend.services;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Async
@RequiredArgsConstructor
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendVerificationEmail(String to, String verificationCode) {
        String subject = "Account Verification Code";
        String content = "Dear user,\n\nYour verification code is: " + verificationCode + "\n\nThank you for registering with us.";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, false);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new IllegalStateException("Failed to send email", e);
        }
    }
}
