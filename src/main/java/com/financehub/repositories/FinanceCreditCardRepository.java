package com.financehub.repositories;

import com.financehub.entities.FinanceCreditCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FinanceCreditCardRepository extends JpaRepository<FinanceCreditCard, Long> {
	List<FinanceCreditCard> findByUserIdOrderByCardNameAsc(Long userId);
	Optional<FinanceCreditCard> findByIdAndUserId(Long id, Long userId);
}
