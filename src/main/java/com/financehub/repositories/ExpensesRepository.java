package com.financehub.repositories;

import com.financehub.dtos.ExpenseReportDTO;
import com.financehub.entities.Expenses;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpensesRepository extends JpaRepository<Expenses, Integer> {
    List<Expenses> findByUserId(Long userId);
    List<Expenses> findByExpenseYearAndUserId(int year, Long userId);
    Optional<Expenses> findByIdAndUserId(Long expenseId, Long userId);
    Optional<Expenses> findByUserIdAndExpenseYearAndExpenseMonth(Long userId, int year, int month);
}
