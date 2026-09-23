package com.financehub.repositories;

import com.financehub.entities.FinanceCreditCardBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FinanceCreditCardBillRepository extends JpaRepository<FinanceCreditCardBill, Long> {

	List<FinanceCreditCardBill> findByUserIdOrderByBillYearDescBillMonthDescIdDesc(Long userId);

	List<FinanceCreditCardBill> findByUserIdAndCardIdOrderByBillYearDescBillMonthDescIdDesc(Long userId, Long cardId);

	Optional<FinanceCreditCardBill> findByIdAndUserId(Long id, Long userId);

	List<FinanceCreditCardBill> findByUserIdAndCardIdAndBillMonthAndBillYearOrderByIdAsc(
			Long userId, Long cardId, Integer billMonth, Integer billYear);
}
