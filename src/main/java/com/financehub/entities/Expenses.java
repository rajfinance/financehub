package com.financehub.entities;

import com.financehub.dtos.ExpenseDetail;
import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Entity
@Data
@Table(name = "expenses", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "expense_month", "expense_year", "expense_type"}))
public class Expenses {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "expense_month", nullable = false)
    private Integer expenseMonth;

    @Column(name = "expense_year", nullable = false)
    private Integer expenseYear;

    @Column(name = "planned_expenses")
    @Convert(converter = ExpenseDetail.class)
    private Map<Integer, Double> plannedExpenses;

    @Column(name = "actual_expenses")
    @Convert(converter = ExpenseDetail.class)
    private Map<Integer, Double> actualExpenses;

    @Column(name = "created_at", columnDefinition = "timestamp default CURRENT_TIMESTAMP")
    private Timestamp createdAt;

    @Column(name = "updated_at", columnDefinition = "timestamp default CURRENT_TIMESTAMP")
    private Timestamp updatedAt;

    public Expenses() {}

    public Expenses(Long userId, Integer expenseMonth, Integer expenseYear, Map<Integer, Double> plannedExpenses, Map<Integer, Double> actualExpenses) {
        this.userId = userId;
        this.expenseMonth = expenseMonth;
        this.expenseYear = expenseYear;
        this.plannedExpenses = plannedExpenses;
        this.actualExpenses = actualExpenses;
    }
}
