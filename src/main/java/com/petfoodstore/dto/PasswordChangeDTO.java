package com.petfoodstore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordChangeDTO {
    @NotBlank(message = "Old password is required")
    private String oldPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 6, message = "New password must be at least 6 characters")
    private String newPassword;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

    public boolean isPasswordMatching() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }
}