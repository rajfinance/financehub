package com.financehub.dtos;

import com.financehub.entities.Owner;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
@Data
public class OwnerDTO {

    private Long ownerId;
    @NotBlank(message = "Owner name is required.")
    private String name;

    @NotBlank(message = "Phone number is required.")
    @Pattern(regexp = "\\d{10}", message = "Phone number must be a valid 10-digit number.")
    private String phoneNumber;

    @NotBlank(message = "Address is required.")
    private String address;

    @NotNull(message = "Advance Months is required.")
    @Positive(message = "Advance amount must be greater than zero.")
    private Integer advanceMonths;

    @NotNull(message = "Advance amount is required.")
    @Positive(message = "Advance amount must be greater than zero.")
    private Double advanceAmount;

    @NotNull(message = "Advance date is required.")
    @PastOrPresent(message = "Advance date cannot be in the future.")
    private LocalDate advanceDate;
    private String formattedAdvanceDate;

    public OwnerDTO(Owner owner) {
        if(owner !=null) {
            this.ownerId = owner.getId();
            this.name = owner.getName();
            this.phoneNumber = owner.getPhoneNumber();
            this.address = owner.getAddress();
            this.advanceMonths = owner.getAdvanceMonths();
            this.advanceAmount = owner.getAdvanceAmount();
            this.advanceDate = owner.getAdvanceDate();
        }
        else{
            this.ownerId = null;
            this.name = "";
            this.phoneNumber = "";
            this.address = "";
            this.advanceMonths = 0;
            this.advanceAmount = null;
            this.advanceDate = null;
        }
    }
}
