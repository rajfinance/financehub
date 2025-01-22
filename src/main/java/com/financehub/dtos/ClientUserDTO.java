package com.financehub.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClientUserDTO {

    @NotBlank(message = "Username is required.")
    private String username;

    @NotBlank(message = "Email is required.")
    @Email(message = "Email should be valid.")
    private String email;

    @NotBlank(message = "Password is required.")
    @Size(min = 8, message = "Password should be at least 8 characters long.")
    private String password;

    @NotBlank(message = "Password confirmation is required.")
    private String confirmPassword;

}
