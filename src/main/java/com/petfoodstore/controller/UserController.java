package com.petfoodstore.controller;

import com.petfoodstore.dto.UserDTO;
import com.petfoodstore.dto.PasswordChangeDTO;
import com.petfoodstore.dto.MessageResponse;
import com.petfoodstore.entity.User;
import com.petfoodstore.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Get current user profile
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> getCurrentUser(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        return ResponseEntity.ok(convertToDTO(user));
    }

    // Update current user profile
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> updateProfile(@Valid @RequestBody UserDTO userDTO,
                                                 Authentication authentication) {
        User currentUser = userService.findByUsername(authentication.getName());

        // Update allowed fields
        currentUser.setFullName(userDTO.getFullName());
        currentUser.setPhone(userDTO.getPhone());
        currentUser.setAddress(userDTO.getAddress());

        // Check if email is being changed and is unique
        if (!currentUser.getEmail().equals(userDTO.getEmail())) {
            if (userService.existsByEmail(userDTO.getEmail())) {
                return ResponseEntity.badRequest().build();
            }
            currentUser.setEmail(userDTO.getEmail());
        }

        User updatedUser = userService.updateUser(currentUser.getId(), currentUser);
        return ResponseEntity.ok(convertToDTO(updatedUser));
    }

    // Change password
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changePassword(@Valid @RequestBody PasswordChangeDTO passwordDTO,
                                            Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());

        // Check old password
        if (!passwordEncoder.matches(passwordDTO.getOldPassword(), user.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Mật khẩu cũ không chính xác!"));
        }

        // Update password
        user.setPassword(passwordEncoder.encode(passwordDTO.getNewPassword()));
        userService.updateUser(user.getId(), user);

        return ResponseEntity.ok(new MessageResponse("Đổi mật khẩu thành công!"));
    }

    // Admin endpoints

    // Get all users
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        List<UserDTO> userDTOs = users.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(userDTOs);
    }

    // Get user by ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        User user = userService.findById(id);
        return ResponseEntity.ok(convertToDTO(user));
    }

    // Create new user (Admin only)
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> createUser(@Valid @RequestBody UserDTO userDTO) {
        // Check if username exists
        if (userService.existsByUsername(userDTO.getUsername())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Username đã tồn tại!"));
        }

        // Check if email exists
        if (userService.existsByEmail(userDTO.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Email đã được sử dụng!"));
        }

        // Create new user
        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setEmail(userDTO.getEmail());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setFullName(userDTO.getFullName());
        user.setPhone(userDTO.getPhone());
        user.setAddress(userDTO.getAddress());
        user.setRole(User.UserRole.valueOf(userDTO.getRole()));
        user.setActive(true);

        User savedUser = userService.createUser(user);
        return ResponseEntity.ok(convertToDTO(savedUser));
    }

    // Update user (Admin only)
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id,
                                              @Valid @RequestBody UserDTO userDTO) {
        User user = userService.findById(id);

        // Update fields
        user.setFullName(userDTO.getFullName());
        user.setPhone(userDTO.getPhone());
        user.setAddress(userDTO.getAddress());

        // Update role if provided
        if (userDTO.getRole() != null) {
            user.setRole(User.UserRole.valueOf(userDTO.getRole()));
        }

        // Check if email is being changed
        if (!user.getEmail().equals(userDTO.getEmail())) {
            if (userService.existsByEmail(userDTO.getEmail())) {
                return ResponseEntity.badRequest().build();
            }
            user.setEmail(userDTO.getEmail());
        }

        User updatedUser = userService.updateUser(id, user);
        return ResponseEntity.ok(convertToDTO(updatedUser));
    }

    // Toggle user status (Admin only)
    @PutMapping("/{id}/toggle-status")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long id) {
        userService.toggleUserStatus(id);
        return ResponseEntity.ok(new MessageResponse("Cập nhật trạng thái thành công!"));
    }

    // Delete user (Admin only)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(new MessageResponse("Xóa người dùng thành công!"));
    }

    // Helper method to convert User to UserDTO
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setPhone(user.getPhone());
        dto.setAddress(user.getAddress());
        dto.setRole(user.getRole().name());
        dto.setActive(user.getActive());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}