package com.petfoodstore.controller;

import com.petfoodstore.dto.LoginRequest;
import com.petfoodstore.dto.SignupRequest;
import com.petfoodstore.dto.JwtResponse;
import com.petfoodstore.dto.MessageResponse;
import com.petfoodstore.entity.User;
import com.petfoodstore.repository.UserRepository;
import com.petfoodstore.security.JwtUtils;
import com.petfoodstore.security.UserDetailsImpl;
import com.petfoodstore.service.EmailVerificationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    EmailVerificationService emailVerificationService;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        // Check if user exists and is verified
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));

        if (!user.getEmailVerified()) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Email chưa được xác thực. Vui lòng kiểm tra email để xác thực tài khoản."));
        }

        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Tên đăng nhập đã được sử dụng!"));
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Email đã được sử dụng!"));
        }

        // Create new user's account
        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(encoder.encode(signUpRequest.getPassword()));
        user.setFullName(signUpRequest.getFullName());
        user.setPhone(signUpRequest.getPhone());
        user.setAddress(signUpRequest.getAddress());
        user.setRole(User.UserRole.CUSTOMER);
        user.setEmailVerified(false);
        user.setActive(false);

        userRepository.save(user);

        // Send verification email
        try {
            emailVerificationService.sendVerificationEmail(user);
            return ResponseEntity.ok(new MessageResponse(
                "Đăng ký thành công! Vui lòng kiểm tra email để xác thực tài khoản."));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Đăng ký thất bại. Không thể gửi email xác thực."));
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        boolean verified = emailVerificationService.verifyEmail(token);
        
        if (verified) {
            return ResponseEntity.ok(new MessageResponse("Xác thực email thành công! Bạn có thể đăng nhập ngay bây giờ."));
        } else {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Mã xác thực không hợp lệ hoặc đã hết hạn."));
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerificationEmail(@RequestParam String email) {
        return userRepository.findByEmail(email)
                .map(user -> {
                    if (user.getEmailVerified()) {
                        return ResponseEntity.badRequest()
                                .body(new MessageResponse("Email đã được xác thực."));
                    }
                    try {
                        emailVerificationService.sendVerificationEmail(user);
                        return ResponseEntity.ok(new MessageResponse(
                            "Đã gửi lại email xác thực. Vui lòng kiểm tra hộp thư của bạn."));
                    } catch (Exception e) {
                        return ResponseEntity.badRequest()
                                .body(new MessageResponse("Không thể gửi lại email xác thực."));
                    }
                })
                .orElse(ResponseEntity.badRequest()
                        .body(new MessageResponse("Không tìm thấy tài khoản với email này.")));
    }
}