package com.financehub.services;

import com.financehub.entities.ExpenseCategories;
import com.financehub.repositories.ExpensesCategoriesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class ExpensesService {
    @Autowired
    public ExpensesCategoriesRepository expensesCategoriesRepository;
    public List<ExpenseCategories> getAllCategories(Long userId) {
        return expensesCategoriesRepository.findByUserIdOrderBySortOrder(userId);
    }
}
