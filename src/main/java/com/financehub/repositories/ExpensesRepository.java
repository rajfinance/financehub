package com.financehub.repositories;

import com.financehub.entities.Expenses;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpensesRepository extends JpaRepository<Expenses, Integer> {
    List<Expenses> findByUserId(Long userId);
    List<Expenses> findByExpenseYearAndUserId(int year, Long userId);
    Optional<Expenses> findByIdAndUserId(Long expenseId, Long userId);
    Optional<Expenses> findByUserIdAndExpenseYearAndExpenseMonth(Long userId, int year, int month);
    @Query("SELECT e.expenseMonth, e.plannedExpenses, e.actualExpenses FROM Expenses e " +
            "WHERE e.userId = :userId AND e.expenseYear = :year")
    List<Object[]> getYearlyPlanActual(@Param("userId") Long userId, @Param("year") int year);
    boolean existsByIdAndPlannedExpensesNotNull(Long expenseId);
    boolean existsByIdAndActualExpensesNotNull(Long expenseId);
    @Modifying
    @Query("UPDATE Expenses e SET e.plannedExpenses = NULL WHERE e.id = :expenseId")
    void clearPlanById(@Param("expenseId") Long expenseId);
    @Modifying
    @Query("UPDATE Expenses e SET e.actualExpenses = NULL WHERE e.id = :expenseId")
    void clearActualById(@Param("expenseId") Long expenseId);
    List<Expenses> findByUserIdAndExpenseYear(Long userId, int year);
}
