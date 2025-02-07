package com.financehub.repositories;

import com.financehub.entities.ExpenseCategories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpensesCategoriesRepository extends JpaRepository<ExpenseCategories, Integer> {
    List<ExpenseCategories> findByUserIdOrderBySortOrder(Long userId);
}
