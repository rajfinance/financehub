package com.financehub.repositories;

import com.financehub.entities.FinanceInsurancePolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FinanceInsurancePolicyRepository extends JpaRepository<FinanceInsurancePolicy, Long> {
	List<FinanceInsurancePolicy> findByUserIdOrderByNextDueDateAsc(Long userId);
	Optional<FinanceInsurancePolicy> findByIdAndUserId(Long id, Long userId);
	List<FinanceInsurancePolicy> findByUserIdAndNextDueDateLessThanEqualOrderByNextDueDateAsc(Long userId, LocalDate date);
}
