package com.financehub.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClientUserDTO {

    @NotBlank(message = "Username is required.")
    private String username;

    @NotBlank(message = "Email is required.")
    @Email(message = "Email should be valid.")
    private String email;

    @NotBlank(message = "Phone is required.")
    @Size(min = 6, max = 32, message = "Phone must be between 6 and 32 characters.")
    @Pattern(regexp = "^[0-9+\\-\\s()]{6,32}$", message = "Phone should contain only digits and common phone symbols (+, -, spaces, parentheses).")
    private String phone;

    @NotBlank(message = "Password is required.")
    @Size(min = 8, message = "Password should be at least 8 characters long.")
    private String password;

    @NotBlank(message = "Password confirmation is required.")
    private String confirmPassword;

}
