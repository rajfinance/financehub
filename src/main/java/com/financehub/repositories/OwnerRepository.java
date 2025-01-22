package com.financehub.repositories;

import com.financehub.entities.Owner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface OwnerRepository extends JpaRepository<Owner, Long> {
    List<Owner> findByUserId(Long userId);
    boolean existsByAdvanceDate(LocalDate advanceDate);
    boolean existsByAdvanceDateAndIdNot(LocalDate advanceDate, Long ownerId);
}
