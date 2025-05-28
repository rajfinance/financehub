package com.financehub.repositories;

import com.financehub.entities.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByUserId(Long userId);

    boolean existsByLoanAccountNumber(String loanAccountNumber);
}
