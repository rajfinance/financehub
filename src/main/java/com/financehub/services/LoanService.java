package com.financehub.services;

import com.financehub.dtos.LoanDTO;
import com.financehub.entities.Loan;
import com.financehub.repositories.LoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
@Service
public class LoanService {
    @Autowired
    private LoanRepository loanRepository;
    public void addLoanFromDto(LoanDTO loanDto, Long userId) {
            Loan loan = new Loan();

            loan.setUserId(userId);
            loan.setLoanAccountNumber(loanDto.getLoanId());
            loan.setBankName(loanDto.getBankName());
            loan.setLoanType(loanDto.getLoanType());
            loan.setLoanAmount(loanDto.getLoanAmount());
            loan.setStartDate(loanDto.getStartDate());
            loan.setInterestRate(loanDto.getInterestRate());
            loan.setEmiAmount(loanDto.getEmiAmount());
            loan.setTenure(loanDto.getTenure());
            loan.setEmiDate(loanDto.getEmiDate());

            loan.setCreatedAt(LocalDateTime.now());
            loan.setUpdatedAt(LocalDateTime.now());

           // return loanRepository.save(loan);
        }
}