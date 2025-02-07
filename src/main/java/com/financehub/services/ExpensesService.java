package com.financehub.services;

import com.financehub.dtos.ExpensesCategoriesDTO;
import com.financehub.entities.ExpenseCategories;
import com.financehub.repositories.ExpensesCategoriesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ExpensesService {
    @Autowired
    UserService userService;
    @Autowired
    public ExpensesCategoriesRepository expensesCategoriesRepository;
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
}
