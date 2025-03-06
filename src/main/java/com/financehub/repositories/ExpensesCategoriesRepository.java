package com.financehub.repositories;

import com.financehub.entities.ExpenseCategories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Repository
public interface ExpensesCategoriesRepository extends JpaRepository<ExpenseCategories, Integer> {
    List<ExpenseCategories> findByUserIdOrderBySortOrder(Long userId);
    @Query("SELECT c.id, c.name,c.sortOrder FROM ExpenseCategories c WHERE c.id IN :ids")
    Collection<Object[]> findCategoryNamesByIds(@Param("ids")ArrayList<Integer> integers);
}
