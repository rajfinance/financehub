package com.financehub.services;

import com.financehub.dtos.ExpenseDetail;
import com.financehub.dtos.ExpenseReportDTO;
import com.financehub.dtos.ExpenseRequest;
import com.financehub.dtos.ExpensesCategoriesDTO;
import com.financehub.entities.ExpenseCategories;
import com.financehub.entities.Expenses;
import com.financehub.repositories.ExpensesCategoriesRepository;
import com.financehub.repositories.ExpensesRepository;
import com.financehub.utils.FormatterUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExpensesService {
    @Autowired
    UserService userService;
    @Autowired
    private FormatterUtils formatterUtils;
    @Autowired
    public ExpensesCategoriesRepository expensesCategoriesRepository;
    @Autowired
    public ExpensesRepository expensesRepository;
    public List<ExpenseCategories> getAllCategories(Long userId) {
        return expensesCategoriesRepository.findByUserIdOrderBySortOrder(userId);
    }
    public List<ExpenseCategories> getEnabledCategories(Long userId){
        return expensesCategoriesRepository.findByUserIdOrderBySortOrder(userId)
                .stream()
                .filter(ExpenseCategories::isEnabled)
                .collect(Collectors.toList());
    }

    public void saveCategory(ExpensesCategoriesDTO expensesCategoriesDTO) {
        ExpenseCategories entity = mapDtoToEntity(expensesCategoriesDTO);
        expensesCategoriesRepository.save(entity);
    }

    private ExpenseCategories mapDtoToEntity(ExpensesCategoriesDTO dto) {
        ExpenseCategories entity = new ExpenseCategories();

        if(dto.getCategoryId() == null || dto.getCategoryId() == 0){
            entity.setName(dto.getCategoryName());
            entity.setIcon(dto.getIconPath());
            entity.setSortOrder(dto.getSortOrder());
            entity.setUserId(userService.getUserId());
            entity.setEnabled(dto.isEnabled());
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
        }
        else{
            Optional<ExpenseCategories> existingCategory = expensesCategoriesRepository.findById(Math.toIntExact(dto.getCategoryId()));

            if (existingCategory.isPresent()) {
               entity = existingCategory.get();
                entity.setName(dto.getCategoryName());
                entity.setIcon(dto.getIconPath());
                entity.setSortOrder(dto.getSortOrder());
                entity.setEnabled(dto.isEnabled());
                entity.setUpdatedAt(LocalDateTime.now());
            }
        }
        return entity;
    }

    public void deleteCategoryByID(int id) {
        expensesCategoriesRepository.deleteById(id);
    }

    public void saveExpense(ExpenseRequest expenseRequest) {
            YearMonth yearMonth = YearMonth.parse(expenseRequest.getMonth());
            int year = yearMonth.getYear();
            int month = yearMonth.getMonthValue();
            Long userId = userService.getUserId();

            boolean isPlanned = "plan".equalsIgnoreCase(expenseRequest.getExpenseType());
            String expenseTypeChar = "plan".equalsIgnoreCase(expenseRequest.getExpenseType()) ? "P" : "A";

            Optional<Expenses> existingExpenseOpt = expensesRepository.findByUserIdAndExpenseYearAndExpenseMonth(userId, year, month);

            Expenses expenseEntity;
            if (existingExpenseOpt.isPresent()) {
                expenseEntity = existingExpenseOpt.get();
                expenseEntity.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));

                if (isPlanned) {
                    expenseEntity.setPlannedExpenses(expenseRequest.getExpenses());
                } else {
                    expenseEntity.setActualExpenses(expenseRequest.getExpenses());
                }
            } else {
                expenseEntity = new Expenses();
                expenseEntity.setUserId(userId);
                expenseEntity.setExpenseYear(year);
                expenseEntity.setExpenseMonth(month);
                expenseEntity.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
                expenseEntity.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));

                if (isPlanned) {
                    expenseEntity.setPlannedExpenses(expenseRequest.getExpenses());
                    expenseEntity.setActualExpenses(null);
                } else {
                    expenseEntity.setActualExpenses(expenseRequest.getExpenses());
                    expenseEntity.setPlannedExpenses(null);
                }
            }

            expensesRepository.save(expenseEntity);
        }

    public Set<Integer> getDistinctExpenseYearsForUser(Long id) {
        return expensesRepository.findByUserId(id)
                .stream()
                .map(Expenses::getExpenseYear)
                .collect(Collectors.toSet());
    }

    public List<ExpenseReportDTO> getExpenseReport(int year) {
        List<Expenses> expenses = expensesRepository.findByExpenseYearAndUserId(year, userService.getUserId());
        return expenses.stream().map(expense -> {
            double totalPlanAmount = 0.0;
            double totalActualAmount = 0.0;
            if (expense.getPlannedExpenses() != null) {
                for (Double value : expense.getPlannedExpenses().values()) {
                    totalPlanAmount += value;
                }
            }
            if (expense.getActualExpenses() != null) {
                for (Double value : expense.getActualExpenses().values()) {
                    totalActualAmount += value;
                }
            }
            return new ExpenseReportDTO(
                    expense.getId(),
                    expense.getExpenseYear(),
                    expense.getExpenseMonth(),
                    formatterUtils.getMonthName(expense.getExpenseMonth()),
                    expense.getPlannedExpenses(),
                    expense.getActualExpenses(),
                    totalPlanAmount,
                    totalActualAmount
            );
        }).collect(Collectors.toList());
    }

    public ExpenseReportDTO getExpenseDetailsById(Long id) {
        Optional<Expenses> optExpense = expensesRepository.findByIdAndUserId(id, userService.getUserId());
        if (optExpense.isPresent()) {
            Expenses expense = optExpense.get();
            return new ExpenseReportDTO(
                    expense.getId(),
                    expense.getExpenseYear(),
                    expense.getExpenseMonth(),"",
                    expense.getPlannedExpenses(),
                    expense.getActualExpenses(),
                    0,0
            );
        }
        return null;
    }
}
