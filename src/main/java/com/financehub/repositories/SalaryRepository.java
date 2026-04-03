package com.financehub.repositories;

import com.financehub.entities.ClientUser;
import com.financehub.entities.Salary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SalaryRepository extends JpaRepository<Salary, Long> {
    @Query("SELECT s FROM Salary s WHERE s.user = :user ORDER BY s.salaryYear DESC, s.salaryMonth ASC")
    List<Salary> findAllSalariesByUser(@Param("user") ClientUser user);
    boolean existsByCompanyId(Long companyId);
    List<Salary> findByUserIdAndSalaryYear(Long userId, int salaryYear);
    List<Salary> findByUserId(Long userId);

    @Query("SELECT s FROM Salary s WHERE s.id = :id AND s.user.id = :userId")
    Optional<Salary> findByIdAndUser_Id(@Param("id") Long id, @Param("userId") int userId);
}
