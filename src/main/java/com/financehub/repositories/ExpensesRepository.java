package com.financehub.repositories;

import com.financehub.entities.Expenses;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ExpensesRepository extends JpaRepository<Expenses, Integer> {
    Optional<Expenses> findByUserIdAndExpenseMonthAndExpenseYearAndExpenseType(Integer userId, Integer expenseMonth, Integer expenseYear, String expenseType);
}
