package com.financehub.repositories;

import com.financehub.entities.FinanceLedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FinanceLedgerEntryRepository extends JpaRepository<FinanceLedgerEntry, Long> {
	List<FinanceLedgerEntry> findByUserIdAndAccountIdOrderByEntryDateDescIdDesc(Long userId, Long accountId);

	List<FinanceLedgerEntry> findByUserIdOrderByEntryDateDescIdDesc(Long userId);

	Optional<FinanceLedgerEntry> findByIdAndUserId(Long id, Long userId);

	@Query("SELECT COALESCE(SUM(e.amount), 0) FROM FinanceLedgerEntry e WHERE e.userId = :userId AND e.entryType = :entryType AND YEAR(e.entryDate) = :year")
	double sumByUserIdAndTypeAndYear(@Param("userId") Long userId, @Param("entryType") String entryType, @Param("year") int year);
}
