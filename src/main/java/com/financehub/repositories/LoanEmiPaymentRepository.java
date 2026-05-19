package com.financehub.repositories;

import com.financehub.entities.LoanEmiPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoanEmiPaymentRepository extends JpaRepository<LoanEmiPayment, Long> {

    List<LoanEmiPayment> findByLoanIdOrderByEmiNumberAsc(Long loanId);

    Optional<LoanEmiPayment> findByLoanIdAndEmiNumber(Long loanId, Integer emiNumber);

    void deleteByLoanId(Long loanId);
}
