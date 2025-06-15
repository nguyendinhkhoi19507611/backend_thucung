package com.petfoodstore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public void sendVerificationEmail(String toEmail, String token) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(toEmail);
        helper.setSubject("Xác thực email - Pet Food Store");

        String verificationUrl = frontendUrl + "/verify-email?token=" + token;
        String emailContent = String.format("""
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <h2 style="color: #333;">Xác thực email của bạn</h2>
                <p>Cảm ơn bạn đã đăng ký tài khoản tại Pet Food Store. Để hoàn tất quá trình đăng ký, vui lòng xác thực email của bạn bằng cách nhấp vào nút bên dưới:</p>
                <div style="text-align: center; margin: 30px 0;">
                    <a href="%s" style="background-color: #4CAF50; color: white; padding: 12px 24px; text-decoration: none; border-radius: 4px; display: inline-block;">Xác thực email</a>
                </div>
                <p>Hoặc bạn có thể copy và paste đường dẫn sau vào trình duyệt:</p>
                <p style="word-break: break-all; color: #666;">%s</p>
                <p>Lưu ý: Liên kết này sẽ hết hạn sau 15 phút.</p>
                <p>Nếu bạn không đăng ký tài khoản tại Pet Food Store, vui lòng bỏ qua email này.</p>
                <hr style="border: 1px solid #eee; margin: 20px 0;">
                <p style="color: #666; font-size: 12px;">Email này được gửi tự động, vui lòng không trả lời.</p>
            </div>
            """, verificationUrl, verificationUrl);

        helper.setText(emailContent, true);
        mailSender.send(message);
    }
} 