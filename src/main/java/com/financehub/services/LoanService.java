package com.financehub.services;

import com.financehub.dtos.LoanDTO;
import com.financehub.entities.Loan;
import com.financehub.repositories.LoanRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class LoanService {

	private final LoanRepository loanRepository;
	private final UserService userService;

	public LoanService(LoanRepository loanRepository, UserService userService) {
		this.loanRepository = loanRepository;
		this.userService = userService;
	}

	public void addLoanFromDto(LoanDTO loanDto) {
		long userId = userService.getUserId();
		if (userId <= 0) {
			throw new IllegalStateException("Not authenticated");
		}
		if (loanRepository.existsByLoanAccountNumber(loanDto.getLoanId())) {
			throw new IllegalArgumentException("A loan with this account number already exists.");
		}
		LocalDate startDate = loanDto.getEmiDate() != null ? loanDto.getEmiDate() : LocalDate.now();

		Loan loan = new Loan();
		loan.setUserId(userId);
		loan.setLoanAccountNumber(loanDto.getLoanId());
		loan.setBankName(loanDto.getBankName());
		loan.setLoanType(loanDto.getLoanType());
		loan.setLoanAmount(loanDto.getLoanAmount());
		loan.setStartDate(startDate);
		loan.setInterestRate(loanDto.getInterestRate() != null ? loanDto.getInterestRate() : 0d);
		loan.setEmiAmount(loanDto.getEmiAmount());
		loan.setTenure(loanDto.getTenure());
		loan.setEmiDate(loanDto.getEmiDate());
		loan.setCreatedAt(LocalDateTime.now());
		loan.setUpdatedAt(LocalDateTime.now());

		loanRepository.save(loan);
	}
}
