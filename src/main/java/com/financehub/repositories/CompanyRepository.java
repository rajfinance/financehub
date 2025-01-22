package com.financehub.repositories;

import com.financehub.entities.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    @Query("SELECT c FROM Company c WHERE c.userId = :userId")
    List<Company> findCompaniesByUserId(@Param("userId") Long userId);

}
