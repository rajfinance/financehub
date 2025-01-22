package com.financehub.dtos;

import com.financehub.entities.Owner;
import com.financehub.entities.RentPayment;
import lombok.Data;

import java.time.LocalDate;
@Data
public class RentPaymentDTO {

    private Long id;
    private Long paymentId;
    private Owner owner;
    private Long ownerId;
    private LocalDate rentPeriodStart;
    private LocalDate rentPeriodEnd;
    private LocalDate paidOn;
    private Double amount;
    private String formattedRentPeriodStart;
    private String formattedRentPeriodEnd;
    private String formattedPaidOn;
    private String formattedAmount;

    public RentPaymentDTO(RentPayment rentPayment) {
        if(rentPayment!=null) {
            this.id = rentPayment.getId();
            this.owner = rentPayment.getOwner();
            this.ownerId = rentPayment.getOwner().getId();
            this.rentPeriodStart = rentPayment.getRentPeriodStart();
            this.rentPeriodEnd = rentPayment.getRentPeriodEnd();
            this.paidOn = rentPayment.getPaidOn();
            this.amount = rentPayment.getAmount();
        }
        else{
            this.id=null;
            this.owner=null;
            this.ownerId = null;
            this.rentPeriodStart = null;
            this.rentPeriodEnd = null;
            this.paidOn = null;
            this.amount = null;
        }
    }

}
