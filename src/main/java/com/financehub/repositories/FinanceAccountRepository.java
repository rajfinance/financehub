package com.financehub.repositories;

import com.financehub.entities.FinanceAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FinanceAccountRepository extends JpaRepository<FinanceAccount, Long> {
	List<FinanceAccount> findByUserIdOrderByNameAsc(Long userId);
	Optional<FinanceAccount> findByIdAndUserId(Long id, Long userId);
}
