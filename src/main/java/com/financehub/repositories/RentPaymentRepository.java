package com.financehub.repositories;

import com.financehub.entities.RentPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RentPaymentRepository extends JpaRepository<RentPayment, Long> {
    List<RentPayment> findByOwnerId(Long ownerId);
    boolean existsByOwner_Id(Long ownerId);
    boolean existsByPaidOnAndOwnerId(LocalDate paidOn, Long ownerId);
    @Query("SELECT r FROM RentPayment r WHERE r.owner.userId = :userId")
    List<RentPayment> findByUserId(@Param("userId") Long userId);

}
