package com.financehub.services;

import com.financehub.dtos.LoanDTO;
import com.financehub.dtos.LoanBankEmiProjectionReportDTO;
import com.financehub.dtos.LoanBankEmiProjectionRowDTO;
import com.financehub.dtos.LoanEmiPaymentDTO;
import com.financehub.dtos.LoanEmiScheduleGroupDTO;
import com.financehub.dtos.LoanEmiScheduleRowDTO;
import com.financehub.dtos.LoanPreClosureDTO;
import com.financehub.dtos.LoanSummaryDTO;
import com.financehub.entities.Loan;
import com.financehub.entities.LoanEmiPayment;
import com.financehub.repositories.LoanEmiPaymentRepository;
import com.financehub.repositories.LoanRepository;
import com.financehub.utils.FormatterUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final LoanEmiPaymentRepository loanEmiPaymentRepository;
    private final UserService userService;
    private final FormatterUtils formatterUtils;
    private final JdbcTemplate jdbcTemplate;
    private volatile boolean preClosureTableChecked = false;

    public LoanService(LoanRepository loanRepository,
                       LoanEmiPaymentRepository loanEmiPaymentRepository,
                       UserService userService,
                       FormatterUtils formatterUtils,
                       JdbcTemplate jdbcTemplate) {
        this.loanRepository = loanRepository;
        this.loanEmiPaymentRepository = loanEmiPaymentRepository;
        this.userService = userService;
        this.formatterUtils = formatterUtils;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void addLoanFromDto(LoanDTO loanDto) {
        long userId = requireUserId();
        validateNewLoan(loanDto);
        if (loanRepository.existsByLoanAccountNumber(loanDto.getLoanAccountNumber().trim())) {
            throw new IllegalArgumentException("A loan with this account number already exists.");
        }

        Loan loan = new Loan();
        loan.setUserId(userId);
        applyLoanFields(loan, loanDto);
        loan.setCreatedAt(LocalDateTime.now());
        loan.setUpdatedAt(LocalDateTime.now());
        loanRepository.save(loan);
    }

    public LoanDTO getLoanForEdit(Long loanId) {
        Loan loan = requireOwnedLoan(loanId);
        LoanDTO dto = new LoanDTO();
        dto.setId(loan.getId());
        dto.setLoanAccountNumber(loan.getLoanAccountNumber());
        dto.setBankName(loan.getBankName());
        dto.setLoanType(loan.getLoanType());
        dto.setLoanAmount(loan.getLoanAmount());
        dto.setInterestRate(loan.getInterestRate());
        dto.setEmiAmount(loan.getEmiAmount());
        dto.setTenure(loan.getTenure());
        dto.setEmiDate(loan.getEmiDate());
        return dto;
    }

    @Transactional
    public void updateLoanFromDto(LoanDTO loanDto) {
        if (loanDto.getId() == null) {
            throw new IllegalArgumentException("Loan id is required.");
        }
        validateNewLoan(loanDto);
        Loan loan = requireOwnedLoan(loanDto.getId());
        String accountNumber = loanDto.getLoanAccountNumber().trim();
        if (loanRepository.existsByLoanAccountNumberAndIdNot(accountNumber, loan.getId())) {
            throw new IllegalArgumentException("A loan with this account number already exists.");
        }
        applyLoanFields(loan, loanDto);
        loanRepository.save(loan);
    }

    public List<LoanSummaryDTO> getLoansForCurrentUser() {
        long userId = requireUserId();
        return loanRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    public LoanSummaryDTO getLoanSummaryById(Long loanId) {
        Loan loan = requireOwnedLoan(loanId);
        return toSummaryDto(loan);
    }

    public LoanEmiPaymentDTO getEmiPaymentById(Long id) {
        LoanEmiPayment payment = loanEmiPaymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("EMI record not found."));
        requireOwnedLoan(payment.getLoanId());
        return toEmiPaymentDto(payment);
    }

    public LoanPreClosureDTO getPreClosureDetails(Long loanId) {
        Loan loan = requireOwnedLoan(loanId);
        LoanPreClosureDTO dto = new LoanPreClosureDTO();
        dto.setLoanId(loan.getId());
        Optional<LoanPreClosureDTO> persistedDetails = getPersistedPreClosure(loanId);
        if (persistedDetails.isPresent()) {
            dto.setPreClosureDate(persistedDetails.get().getPreClosureDate());
            dto.setSettlementAmount(persistedDetails.get().getSettlementAmount());
            dto.setPreClosureType(
                    persistedDetails.get().getPreClosureType() == null ? "FULL" : persistedDetails.get().getPreClosureType());
            dto.setReferenceNumber(persistedDetails.get().getReferenceNumber());
            dto.setUpdatedEmiAmount(persistedDetails.get().getUpdatedEmiAmount());
            dto.setUpdatedTenure(persistedDetails.get().getUpdatedTenure());
        } else {
            dto.setPreClosureDate(LocalDate.now());
            dto.setPreClosureType("FULL");
            dto.setReferenceNumber("");
        }
        if (dto.getPreClosureDate() == null) {
            dto.setPreClosureDate(LocalDate.now());
        }
        return dto;
    }

    @Transactional
    public void savePreClosure(LoanPreClosureDTO dto) {
        if (dto.getLoanId() == null) {
            throw new IllegalArgumentException("Loan is required for pre-closure.");
        }
        if (dto.getPreClosureDate() == null) {
            throw new IllegalArgumentException("Pre-closure date is required.");
        }
        if (dto.getSettlementAmount() == null || dto.getSettlementAmount() <= 0) {
            throw new IllegalArgumentException("Settlement amount must be greater than zero.");
        }
        if (dto.getReferenceNumber() == null || dto.getReferenceNumber().isBlank()) {
            throw new IllegalArgumentException("NOC/Foreclosure reference number is required.");
        }
        String closureType = dto.getPreClosureType() == null ? "FULL" : dto.getPreClosureType().trim().toUpperCase(Locale.ROOT);
        if (!"FULL".equals(closureType) && !"PARTIAL".equals(closureType)) {
            throw new IllegalArgumentException("Pre-closure type must be FULL or PARTIAL.");
        }
        if ("PARTIAL".equals(closureType)) {
            if (dto.getUpdatedEmiAmount() == null || dto.getUpdatedEmiAmount() <= 0) {
                throw new IllegalArgumentException("Updated EMI amount is required for partial closure.");
            }
            if (dto.getUpdatedTenure() == null || dto.getUpdatedTenure() < 1) {
                throw new IllegalArgumentException("Updated tenure is required for partial closure.");
            }
        } else {
            dto.setUpdatedEmiAmount(null);
            dto.setUpdatedTenure(null);
        }
        Loan loan = requireOwnedLoan(dto.getLoanId());
        if (dto.getPreClosureDate().isBefore(loan.getEmiDate())) {
            throw new IllegalArgumentException("Pre-closure date cannot be before first EMI date.");
        }

        dto.setPreClosureType(closureType);
        upsertPreClosureDetails(dto);

        // Cleanup legacy marker; loan_preclosures is the only source of truth.
        loanEmiPaymentRepository.findByLoanIdAndEmiNumber(loan.getId(), 0)
                .ifPresent(loanEmiPaymentRepository::delete);

        // Clean up any manually recorded EMI overrides after pre-closure month.
        int preClosureEmiNumber = resolvePreClosureTargetEmiNumber(loan, dto.getPreClosureDate());
        if ("PARTIAL".equals(closureType) && preClosureEmiNumber >= loan.getTenure()) {
            throw new IllegalArgumentException("Partial closure is valid only when at least one EMI remains after closure month.");
        }
        List<LoanEmiPayment> futureOverrides = loanEmiPaymentRepository.findByLoanIdOrderByEmiNumberAsc(loan.getId())
                .stream()
                .filter(p -> p.getEmiNumber() != null && p.getEmiNumber() > preClosureEmiNumber)
                .collect(Collectors.toList());
        if (!futureOverrides.isEmpty()) {
            loanEmiPaymentRepository.deleteAll(futureOverrides);
        }
    }

    public long getRemainingEmiCountFromDate(Long loanId, LocalDate date) {
        Loan loan = requireOwnedLoan(loanId);
        if (date == null) {
            return 0;
        }
        long count = 0;
        for (int i = 1; i <= loan.getTenure(); i++) {
            LocalDate dueDate = getDueDateForInstallment(loan, i);
            if (dueDate.isAfter(date)) {
                count++;
            }
        }
        return count;
    }

    public String getFormattedRemainingPendingAmountFromDate(Long loanId, LocalDate date) {
        Loan loan = requireOwnedLoan(loanId);
        if (date == null) {
            return formatterUtils.formatInIndianStyle(0);
        }
        Map<Integer, LoanEmiPayment> overrides = loanEmiPaymentRepository.findByLoanIdOrderByEmiNumberAsc(loanId).stream()
                .filter(p -> p.getEmiNumber() != null && p.getEmiNumber() > 0)
                .collect(Collectors.toMap(LoanEmiPayment::getEmiNumber, p -> p, (a, b) -> b));
        double pending = 0;
        for (int i = 1; i <= loan.getTenure(); i++) {
            LocalDate dueDate = getDueDateForInstallment(loan, i);
            if (!dueDate.isAfter(date)) {
                continue;
            }
            LoanEmiPayment override = overrides.get(i);
            pending += override != null ? override.getEmiAmount() : loan.getEmiAmount();
        }
        return formatterUtils.formatInIndianStyle(pending);
    }

    @Transactional
    public void saveEmiPayment(LoanEmiPaymentDTO dto) {
        requireUserId();
        if (dto.getLoanId() == null) {
            throw new IllegalArgumentException("Please select a loan.");
        }
        if (dto.getEmiAmount() == null || dto.getEmiAmount() <= 0) {
            throw new IllegalArgumentException("EMI amount must be greater than zero.");
        }
        if (dto.getPaidOn() == null) {
            throw new IllegalArgumentException("Deduction date is required.");
        }

        Loan loan = requireOwnedLoan(dto.getLoanId());
        if (dto.getEmiNumber() == null || dto.getEmiNumber() < 1) {
            dto.setEmiNumber(resolveEmiNumberFromDate(loan, dto.getPaidOn()));
        }
        int maxAllowedEmiNumber = getMaxAllowedEmiNumber(loan);
        if (dto.getEmiNumber() > maxAllowedEmiNumber) {
            throw new IllegalArgumentException("EMI number cannot exceed loan tenure limit (" + maxAllowedEmiNumber + " months).");
        }
        LocalDate preClosureDate = getPreClosureDateForLoan(loan.getId());
        LocalDate installmentDueDate = getDueDateForInstallment(loan, dto.getEmiNumber());
        boolean fullPreClosed = isFullPreClosed(loan.getId());
        if (fullPreClosed && preClosureDate != null && installmentDueDate.isAfter(preClosureDate)) {
            throw new IllegalArgumentException("This loan is already pre-closed. Remaining EMIs cannot be recorded.");
        }

        LoanEmiPayment payment;
        if (dto.getId() != null) {
            payment = loanEmiPaymentRepository.findById(dto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("EMI record not found."));
            if (!payment.getLoanId().equals(loan.getId())) {
                throw new IllegalArgumentException("EMI record does not belong to the selected loan.");
            }
        } else {
            payment = loanEmiPaymentRepository.findByLoanIdAndEmiNumber(loan.getId(), dto.getEmiNumber())
                    .orElse(new LoanEmiPayment());
            if (payment.getId() == null) {
                payment.setCreatedAt(LocalDateTime.now());
            }
        }

        payment.setLoanId(loan.getId());
        payment.setEmiNumber(dto.getEmiNumber());
        payment.setEmiAmount(dto.getEmiAmount());
        payment.setPaidOn(dto.getPaidOn());
        payment.setUpdatedAt(LocalDateTime.now());
        loanEmiPaymentRepository.save(payment);

        if (Boolean.TRUE.equals(dto.getPreClosureSelected())) {
            LoanPreClosureDTO preClosure = new LoanPreClosureDTO();
            preClosure.setLoanId(dto.getLoanId());
            preClosure.setPreClosureDate(dto.getPreClosureDate());
            preClosure.setSettlementAmount(dto.getPreClosureAmount());
            preClosure.setPreClosureType(dto.getPreClosureType());
            preClosure.setReferenceNumber(dto.getPreClosureReferenceNumber());
            preClosure.setUpdatedEmiAmount(dto.getPartialUpdatedEmiAmount());
            preClosure.setUpdatedTenure(dto.getPartialUpdatedTenure());
            savePreClosure(preClosure);
        }
    }

    @Transactional
    public void deleteEmiPayment(Long id) {
        LoanEmiPayment payment = loanEmiPaymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("EMI record not found."));
        requireOwnedLoan(payment.getLoanId());
        loanEmiPaymentRepository.delete(payment);
    }

    @Transactional
    public void deleteLoan(Long loanId) {
        requireOwnedLoan(loanId);
        loanEmiPaymentRepository.deleteByLoanId(loanId);
        loanRepository.deleteById(loanId);
    }

    /**
     * Builds EMI schedule from loan sanction data. Uses fixed EMI and first EMI date for each month.
     * Optional {@link LoanEmiPayment} rows override amount and deduction date for that installment.
     */
    public List<LoanEmiScheduleRowDTO> getEmiScheduleForUser(Integer year, Long loanId) {
        long userId = requireUserId();
        List<Loan> loans = loanRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<LoanEmiScheduleRowDTO> rows = new ArrayList<>();
        for (Loan loan : loans) {
            if (loanId != null && !loan.getId().equals(loanId)) {
                continue;
            }
            rows.addAll(buildScheduleForLoan(loan, year));
        }
        rows.sort(Comparator
                .comparing(LoanEmiScheduleRowDTO::getDueDate)
                .thenComparing(LoanEmiScheduleRowDTO::getBankName)
                .thenComparing(LoanEmiScheduleRowDTO::getEmiNumber));
        return rows;
    }

    public Map<Long, List<LoanEmiScheduleRowDTO>> getEmiScheduleGroupedByLoan(Integer year, Long loanId) {
        return getEmiScheduleForUser(year, loanId).stream()
                .collect(Collectors.groupingBy(LoanEmiScheduleRowDTO::getLoanId, LinkedHashMap::new, Collectors.toList()));
    }

    public List<LoanEmiScheduleGroupDTO> getEmiScheduleGroups(Integer year, Long loanId) {
        Map<Long, List<LoanEmiScheduleRowDTO>> grouped = getEmiScheduleGroupedByLoan(year, loanId);
        List<LoanEmiScheduleGroupDTO> groups = new ArrayList<>();
        for (LoanSummaryDTO loan : getLoansForCurrentUser()) {
            if (loanId != null && !loan.getId().equals(loanId)) {
                continue;
            }
            List<LoanEmiScheduleRowDTO> rows = grouped.get(loan.getId());
            if (rows == null || rows.isEmpty()) {
                continue;
            }
            LoanEmiScheduleGroupDTO group = new LoanEmiScheduleGroupDTO();
            group.setLoan(loan);
            group.setScheduleRows(rows);
            groups.add(group);
        }
        return groups;
    }

    public String getFormattedYearTotal(Integer year, Long loanId) {
        double total = getEmiScheduleForUser(year, loanId).stream()
                .mapToDouble(LoanEmiScheduleRowDTO::getEmiAmount)
                .sum();
        return formatterUtils.formatInIndianStyle(total);
    }

    public String getFormattedYearPendingAmount(Integer year, Long loanId) {
        LocalDate today = LocalDate.now();
        double pending = getEmiScheduleForUser(year, loanId).stream()
                .filter(row -> row.getDeductionDate() != null && row.getDeductionDate().isAfter(today))
                .mapToDouble(LoanEmiScheduleRowDTO::getEmiAmount)
                .sum();
        return formatterUtils.formatInIndianStyle(pending);
    }

    public LoanBankEmiProjectionReportDTO getBankNextMonthProjectionReport() {
        LocalDate nextMonthStart = LocalDate.now().plusMonths(1).withDayOfMonth(1);

        List<LoanEmiScheduleRowDTO> scheduleRows = getEmiScheduleForUser(null, null);
        TreeMap<YearMonth, LoanBankEmiProjectionRowDTO> monthlyDeductionRows = new TreeMap<>();
        long axisPendingTotal = 0;
        long iciciPendingTotal = 0;
        long hdfcPendingTotal = 0;

        for (LoanEmiScheduleRowDTO row : scheduleRows) {
            if (row.getDueDate() == null || row.getDueDate().isBefore(nextMonthStart)) {
                continue;
            }

            String bucket = resolveBankBucket(row.getBankName());
            if (bucket == null) {
                continue;
            }

            YearMonth installmentMonth = YearMonth.from(row.getDueDate());
            LoanBankEmiProjectionRowDTO monthlyDeductionRow = monthlyDeductionRows.computeIfAbsent(installmentMonth, ym -> {
                LoanBankEmiProjectionRowDTO dto = new LoanBankEmiProjectionRowDTO();
                dto.setDate(formatterUtils.formatDate(ym.atDay(7)));
                return dto;
            });

            long amount = Math.round(row.getEmiAmount());
            if ("AXIS".equals(bucket)) {
                monthlyDeductionRow.setAxisAmount(monthlyDeductionRow.getAxisAmount() + amount);
                axisPendingTotal += amount;
            } else if ("ICICI".equals(bucket)) {
                monthlyDeductionRow.setIciciAmount(monthlyDeductionRow.getIciciAmount() + amount);
                iciciPendingTotal += amount;
            } else if ("HDFC".equals(bucket)) {
                monthlyDeductionRow.setHdfcAmount(monthlyDeductionRow.getHdfcAmount() + amount);
                hdfcPendingTotal += amount;
            }
        }

        long axisRunningPending = axisPendingTotal;
        long iciciRunningPending = iciciPendingTotal;
        long hdfcRunningPending = hdfcPendingTotal;
        long axisHeaderEmi = 0;
        long iciciHeaderEmi = 0;
        long hdfcHeaderEmi = 0;
        if (!monthlyDeductionRows.isEmpty()) {
            LoanBankEmiProjectionRowDTO firstMonth = monthlyDeductionRows.firstEntry().getValue();
            axisHeaderEmi = firstMonth.getAxisAmount();
            iciciHeaderEmi = firstMonth.getIciciAmount();
            hdfcHeaderEmi = firstMonth.getHdfcAmount();
        }
        List<LoanBankEmiProjectionRowDTO> rows = new ArrayList<>();
        for (LoanBankEmiProjectionRowDTO monthlyDeductionRow : monthlyDeductionRows.values()) {
            axisRunningPending = Math.max(0, axisRunningPending - monthlyDeductionRow.getAxisAmount());
            iciciRunningPending = Math.max(0, iciciRunningPending - monthlyDeductionRow.getIciciAmount());
            hdfcRunningPending = Math.max(0, hdfcRunningPending - monthlyDeductionRow.getHdfcAmount());

            LoanBankEmiProjectionRowDTO pendingRow = new LoanBankEmiProjectionRowDTO();
            pendingRow.setDate(monthlyDeductionRow.getDate());
            pendingRow.setAxisAmount(axisRunningPending);
            pendingRow.setIciciAmount(iciciRunningPending);
            pendingRow.setHdfcAmount(hdfcRunningPending);
            fillProjectionComputedColumns(pendingRow);
            rows.add(pendingRow);
        }

        LoanBankEmiProjectionReportDTO report = new LoanBankEmiProjectionReportDTO();
        report.setAxisHeaderAmount(axisHeaderEmi);
        report.setIciciHeaderAmount(iciciHeaderEmi);
        report.setHdfcHeaderAmount(hdfcHeaderEmi);
        report.setRows(rows);
        return report;
    }

    public Map<String, Integer> getYearlyEmiDataForCurrentUser() {
        return getEmiScheduleForUser(null, null).stream()
                .filter(r -> r.getDueDate() != null)
                .collect(Collectors.groupingBy(
                        r -> String.valueOf(r.getDueDate().getYear()),
                        TreeMap::new,
                        Collectors.summingInt(r -> (int) Math.round(r.getEmiAmount()))
                ));
    }

    public Map<String, Integer> getLoanStatusCountForCurrentUser() {
        return getLoansForCurrentUser().stream()
                .collect(Collectors.groupingBy(
                        loan -> loan.getLoanStatus() == null ? "Unknown" : loan.getLoanStatus(),
                        LinkedHashMap::new,
                        Collectors.summingInt(l -> 1)
                ));
    }

    public Map<String, Integer> getLoanBankCountForCurrentUser() {
        return getLoansForCurrentUser().stream()
                .collect(Collectors.groupingBy(
                        loan -> loan.getBankName() == null ? "Unknown" : loan.getBankName(),
                        LinkedHashMap::new,
                        Collectors.summingInt(l -> 1)
                ));
    }

    public int getCurrentYearTotalEmiAmount() {
        int year = LocalDate.now().getYear();
        return (int) Math.round(getEmiScheduleForUser(year, null).stream()
                .mapToDouble(LoanEmiScheduleRowDTO::getEmiAmount)
                .sum());
    }

    public int getCurrentYearPendingEmiAmount() {
        int year = LocalDate.now().getYear();
        LocalDate today = LocalDate.now();
        return (int) Math.round(getEmiScheduleForUser(year, null).stream()
                .filter(r -> r.getDeductionDate() != null && r.getDeductionDate().isAfter(today))
                .mapToDouble(LoanEmiScheduleRowDTO::getEmiAmount)
                .sum());
    }

    public List<Integer> getScheduleYearsForUser() {
        long userId = requireUserId();
        Set<Integer> years = new TreeSet<>();
        int currentYear = LocalDate.now().getYear();
        years.add(currentYear);
        for (Loan loan : loanRepository.findByUserIdOrderByCreatedAtDesc(userId)) {
            if (loan.getEmiDate() == null || loan.getTenure() == null) {
                continue;
            }
            LocalDate start = loan.getEmiDate();
            LocalDate end = getLastDueDateForLoan(loan);
            for (int y = start.getYear(); y <= end.getYear(); y++) {
                years.add(y);
            }
        }
        return new ArrayList<>(years);
    }

    public List<Integer> getScheduleYearsForLoan(Long loanId) {
        Loan loan = requireOwnedLoan(loanId);
        Set<Integer> years = new TreeSet<>();
        int currentYear = LocalDate.now().getYear();
        years.add(currentYear);
        if (loan.getEmiDate() == null || loan.getTenure() == null) {
            return new ArrayList<>(years);
        }
        LocalDate start = loan.getEmiDate();
        LocalDate end = getLastDueDateForLoan(loan);
        for (int y = start.getYear(); y <= end.getYear(); y++) {
            years.add(y);
        }
        return new ArrayList<>(years);
    }

    public LocalDate getDueDateForInstallment(Loan loan, int emiNumber) {
        LoanPreClosureDTO preClosure = getPersistedPreClosure(loan.getId()).orElse(null);
        if (preClosure != null
                && "PARTIAL".equalsIgnoreCase(preClosure.getPreClosureType())
                && preClosure.getUpdatedTenure() != null
                && preClosure.getUpdatedTenure() > 0
                && preClosure.getPreClosureDate() != null) {
            int cutOffEmiNumber = resolvePreClosureTargetEmiNumber(loan, preClosure.getPreClosureDate());
            if (emiNumber > cutOffEmiNumber) {
                LocalDate cutOffDueDate = loan.getEmiDate().plusMonths(cutOffEmiNumber - 1L);
                return cutOffDueDate.plusMonths(emiNumber - cutOffEmiNumber);
            }
        }
        return loan.getEmiDate().plusMonths(emiNumber - 1L);
    }

    public int resolveEmiNumberFromDate(Loan loan, LocalDate paidOn) {
        int maxAllowedEmiNumber = getMaxAllowedEmiNumber(loan);
        for (int i = 1; i <= maxAllowedEmiNumber; i++) {
            LocalDate dueDate = getDueDateForInstallment(loan, i);
            if (dueDate.equals(paidOn)
                    || (dueDate.getYear() == paidOn.getYear() && dueDate.getMonthValue() == paidOn.getMonthValue())) {
                return i;
            }
        }
        throw new IllegalArgumentException(
                "Deduction date does not match any EMI month for this loan. Select the correct installment number.");
    }

    public void prefillEmiPayment(LoanEmiPaymentDTO dto) {
        if (dto.getLoanId() == null) {
            return;
        }
        Loan loan = requireOwnedLoan(dto.getLoanId());
        if (dto.getEmiAmount() == null) {
            dto.setEmiAmount(loan.getEmiAmount());
        }
        if (dto.getPaidOn() == null) {
            // Keep deduction date editable, but initialize it to a sensible EMI date.
            if (dto.getEmiNumber() != null && dto.getEmiNumber() >= 1) {
                dto.setPaidOn(getDueDateForInstallment(loan, dto.getEmiNumber()));
            } else {
                dto.setPaidOn(loan.getEmiDate());
            }
        }
    }

    private List<LoanEmiScheduleRowDTO> buildScheduleForLoan(Loan loan, Integer year) {
        if (loan.getEmiDate() == null || loan.getTenure() == null || loan.getEmiAmount() == null) {
            return List.of();
        }
        Map<Integer, LoanEmiPayment> overrides = loanEmiPaymentRepository.findByLoanIdOrderByEmiNumberAsc(loan.getId())
                .stream()
                .filter(p -> p.getEmiNumber() != null && p.getEmiNumber() > 0)
                .collect(Collectors.toMap(LoanEmiPayment::getEmiNumber, p -> p, (a, b) -> b));
        LoanPreClosureDTO preClosure = getPersistedPreClosure(loan.getId()).orElse(null);
        LocalDate preClosureDate = preClosure != null ? preClosure.getPreClosureDate() : getPreClosureDateForLoan(loan.getId());
        Integer preClosureEmiNumber = preClosureDate != null ? resolvePreClosureTargetEmiNumber(loan, preClosureDate) : null;
        boolean partialClosure = preClosure != null && "PARTIAL".equalsIgnoreCase(preClosure.getPreClosureType())
                && preClosure.getUpdatedTenure() != null && preClosure.getUpdatedTenure() > 0
                && preClosure.getUpdatedEmiAmount() != null && preClosure.getUpdatedEmiAmount() > 0;
        int maxEmiNumber = partialClosure ? preClosureEmiNumber + preClosure.getUpdatedTenure() : loan.getTenure();

        List<LoanEmiScheduleRowDTO> rows = new ArrayList<>();
        for (int i = 1; i <= maxEmiNumber; i++) {
            LocalDate dueDate = getDueDateForInstallment(loan, i);
            if (!partialClosure && preClosureEmiNumber != null && i > preClosureEmiNumber) {
                continue;
            }
            if (year != null && dueDate.getYear() != year) {
                continue;
            }
            LoanEmiPayment override = overrides.get(i);
            boolean recorded = override != null;
            double recurringAmount = loan.getEmiAmount();
            if (partialClosure && preClosureEmiNumber != null && i > preClosureEmiNumber) {
                recurringAmount = preClosure.getUpdatedEmiAmount();
            }
            double amount = recorded ? override.getEmiAmount() : recurringAmount;
            LocalDate deductionDate = recorded ? override.getPaidOn() : dueDate;

            LoanEmiScheduleRowDTO row = new LoanEmiScheduleRowDTO();
            row.setLoanId(loan.getId());
            row.setLoanAccountNumber(loan.getLoanAccountNumber());
            row.setBankName(loan.getBankName());
            row.setLoanType(loan.getLoanType());
            row.setEmiNumber(i);
            row.setDueDate(dueDate);
            row.setFormattedDueDate(formatterUtils.formatDate(dueDate));
            row.setDeductionDate(deductionDate);
            row.setFormattedDeductionDate(formatterUtils.formatDate(deductionDate));
            row.setEmiAmount(amount);
            row.setFormattedEmiAmount(formatterUtils.formatInIndianStyle(amount));
            String emiStatus = resolveEmiStatus(deductionDate);
            if (preClosureEmiNumber != null && i == preClosureEmiNumber) {
                emiStatus = "Pre-Closure";
            }
            row.setEmiStatus(emiStatus);
            row.setRecorded(recorded);
            row.setOverrideId(recorded ? override.getId() : null);
            rows.add(row);
        }
        return rows;
    }

    private void validateNewLoan(LoanDTO loanDto) {
        if (loanDto.getLoanAccountNumber() == null || loanDto.getLoanAccountNumber().isBlank()) {
            throw new IllegalArgumentException("Loan account number is required.");
        }
        if (loanDto.getBankName() == null || loanDto.getBankName().isBlank()) {
            throw new IllegalArgumentException("Bank name is required.");
        }
        if (loanDto.getLoanType() == null || loanDto.getLoanType().isBlank()) {
            throw new IllegalArgumentException("Loan type is required.");
        }
        if (loanDto.getLoanAmount() == null || loanDto.getLoanAmount() <= 0) {
            throw new IllegalArgumentException("Sanctioned loan amount is required.");
        }
        if (loanDto.getTenure() == null || loanDto.getTenure() < 1) {
            throw new IllegalArgumentException("Tenure must be at least 1 month.");
        }
        if (loanDto.getEmiAmount() == null || loanDto.getEmiAmount() <= 0) {
            throw new IllegalArgumentException("EMI amount is required.");
        }
        if (loanDto.getEmiDate() == null) {
            throw new IllegalArgumentException("First EMI date is required.");
        }
    }

    private void applyLoanFields(Loan loan, LoanDTO loanDto) {
        loan.setLoanAccountNumber(loanDto.getLoanAccountNumber().trim());
        loan.setBankName(loanDto.getBankName());
        loan.setLoanType(loanDto.getLoanType());
        loan.setLoanAmount(loanDto.getLoanAmount());
        loan.setInterestRate(loanDto.getInterestRate());
        loan.setEmiAmount(loanDto.getEmiAmount());
        loan.setTenure(loanDto.getTenure());
        loan.setEmiDate(loanDto.getEmiDate());
        loan.setUpdatedAt(LocalDateTime.now());
    }

    private Loan requireOwnedLoan(Long loanId) {
        long userId = requireUserId();
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found."));
        if (!loan.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Loan not found.");
        }
        return loan;
    }

    private long requireUserId() {
        long userId = userService.getUserId();
        if (userId <= 0) {
            throw new IllegalStateException("Not authenticated");
        }
        return userId;
    }

    private String resolveEmiStatus(LocalDate emiDate) {
        if (emiDate == null) {
            return "Pending";
        }
        return emiDate.isAfter(LocalDate.now()) ? "Pending" : "Completed";
    }

    private void fillProjectionComputedColumns(LoanBankEmiProjectionRowDTO row) {
        long axisPay = (long) Math.ceil(row.getAxisAmount() + (row.getAxisAmount() * 0.05) + (row.getAxisAmount() * 0.05 * 0.12));
        long iciciPay = row.getIciciAmount();
        long hdfcPay = (long) Math.ceil(row.getHdfcAmount() + (row.getHdfcAmount() * 0.04) + (row.getHdfcAmount() * 0.04 * 0.12));
        long total = row.getAxisAmount() + row.getIciciAmount() + row.getHdfcAmount();
        long axisAndHdfc = axisPay + hdfcPay;
        long totalPay = axisPay + iciciPay + hdfcPay;

        row.setTotalAmount(total);
        row.setAxisPayAmount(axisPay);
        row.setIciciPayAmount(iciciPay);
        row.setHdfcPayAmount(hdfcPay);
        row.setAxisAndHdfcPayAmount(axisAndHdfc);
        row.setTotalPayAmount(totalPay);
    }

    private String resolveBankBucket(String bankName) {
        if (bankName == null) {
            return null;
        }
        String normalized = bankName.toLowerCase(Locale.ROOT);
        if (normalized.contains("axis")) {
            return "AXIS";
        }
        if (normalized.contains("icici")) {
            return "ICICI";
        }
        if (normalized.contains("hdfc")) {
            return "HDFC";
        }
        return null;
    }

    private LocalDate getPreClosureDateForLoan(Long loanId) {
        Optional<LoanPreClosureDTO> details = getPersistedPreClosure(loanId);
        return details.map(LoanPreClosureDTO::getPreClosureDate).orElse(null);
    }

    private boolean isFullPreClosed(Long loanId) {
        Optional<LoanPreClosureDTO> details = getPersistedPreClosure(loanId);
        return details.isPresent() && !"PARTIAL".equalsIgnoreCase(details.get().getPreClosureType());
    }

    private int getMaxAllowedEmiNumber(Loan loan) {
        Optional<LoanPreClosureDTO> preClosure = getPersistedPreClosure(loan.getId());
        if (preClosure.isPresent()
                && "PARTIAL".equalsIgnoreCase(preClosure.get().getPreClosureType())
                && preClosure.get().getUpdatedTenure() != null
                && preClosure.get().getUpdatedTenure() > 0
                && preClosure.get().getPreClosureDate() != null) {
            int cutOffEmiNumber = resolvePreClosureTargetEmiNumber(loan, preClosure.get().getPreClosureDate());
            return cutOffEmiNumber + preClosure.get().getUpdatedTenure();
        }
        return loan.getTenure();
    }

    private int resolvePreClosureTargetEmiNumber(Loan loan, LocalDate preClosureDate) {
        int maxSearch = Math.max(loan.getTenure(), getMaxAllowedEmiNumberWithoutRecursion(loan));
        for (int i = 1; i <= maxSearch; i++) {
            LocalDate dueDate = loan.getEmiDate().plusMonths(i - 1L);
            if (!dueDate.isBefore(preClosureDate)) {
                return i;
            }
        }
        return Math.max(1, loan.getTenure());
    }

    private int getMaxAllowedEmiNumberWithoutRecursion(Loan loan) {
        Optional<LoanPreClosureDTO> preClosure = getPersistedPreClosure(loan.getId());
        if (preClosure.isPresent()
                && "PARTIAL".equalsIgnoreCase(preClosure.get().getPreClosureType())
                && preClosure.get().getUpdatedTenure() != null
                && preClosure.get().getUpdatedTenure() > 0
                && preClosure.get().getPreClosureDate() != null) {
            int cutOffEmiNumber = 1;
            for (int i = 1; i <= loan.getTenure(); i++) {
                LocalDate dueDate = loan.getEmiDate().plusMonths(i - 1L);
                if (!dueDate.isBefore(preClosure.get().getPreClosureDate())) {
                    cutOffEmiNumber = i;
                    break;
                }
                cutOffEmiNumber = i;
            }
            return cutOffEmiNumber + preClosure.get().getUpdatedTenure();
        }
        return loan.getTenure();
    }

    private LocalDate getLastDueDateForLoan(Loan loan) {
        int maxEmiNumber = loan.getTenure();
        Optional<LoanPreClosureDTO> preClosure = getPersistedPreClosure(loan.getId());
        if (preClosure.isPresent() && preClosure.get().getPreClosureDate() != null) {
            int cutOffEmiNumber = resolvePreClosureTargetEmiNumber(loan, preClosure.get().getPreClosureDate());
            if ("PARTIAL".equalsIgnoreCase(preClosure.get().getPreClosureType())
                    && preClosure.get().getUpdatedTenure() != null
                    && preClosure.get().getUpdatedTenure() > 0) {
                maxEmiNumber = cutOffEmiNumber + preClosure.get().getUpdatedTenure();
            } else {
                maxEmiNumber = cutOffEmiNumber;
            }
        }
        return getDueDateForInstallment(loan, maxEmiNumber);
    }

    private Optional<LoanPreClosureDTO> getPersistedPreClosure(Long loanId) {
        ensurePreClosureTable();
        List<LoanPreClosureDTO> rows = jdbcTemplate.query(
                "SELECT loan_id, pre_closure_date, settlement_amount, pre_closure_type, " +
                        "reference_number, " +
                        "updated_emi_amount, updated_tenure " +
                        "FROM loan_preclosures WHERE loan_id = ?",
                preClosureRowMapper(),
                loanId
        );
        return rows.stream().findFirst();
    }

    private void upsertPreClosureDetails(LoanPreClosureDTO dto) {
        ensurePreClosureTable();
        int updated = jdbcTemplate.update(
                "UPDATE loan_preclosures SET pre_closure_date = ?, settlement_amount = ?, " +
                        "pre_closure_type = ?, reference_number = ?, updated_emi_amount = ?, updated_tenure = ?, " +
                        "updated_at = CURRENT_TIMESTAMP WHERE loan_id = ?",
                dto.getPreClosureDate(),
                dto.getSettlementAmount(),
                safeTrim(dto.getPreClosureType()),
                safeTrim(dto.getReferenceNumber()),
                dto.getUpdatedEmiAmount(),
                dto.getUpdatedTenure(),
                dto.getLoanId()
        );
        if (updated == 0) {
            jdbcTemplate.update(
                    "INSERT INTO loan_preclosures (loan_id, pre_closure_date, settlement_amount, pre_closure_type, reference_number, updated_emi_amount, updated_tenure, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    dto.getLoanId(),
                    dto.getPreClosureDate(),
                    dto.getSettlementAmount(),
                    safeTrim(dto.getPreClosureType()),
                    safeTrim(dto.getReferenceNumber()),
                    dto.getUpdatedEmiAmount(),
                    dto.getUpdatedTenure()
            );
        }
    }

    private RowMapper<LoanPreClosureDTO> preClosureRowMapper() {
        return (ResultSet rs, int rowNum) -> {
            LoanPreClosureDTO dto = new LoanPreClosureDTO();
            dto.setLoanId(rs.getLong("loan_id"));
            dto.setPreClosureDate(rs.getObject("pre_closure_date", LocalDate.class));
            dto.setSettlementAmount(rs.getDouble("settlement_amount"));
            dto.setPreClosureType(rs.getString("pre_closure_type"));
            dto.setReferenceNumber(rs.getString("reference_number"));
            Double updatedEmiAmount = rs.getObject("updated_emi_amount", Double.class);
            Integer updatedTenure = rs.getObject("updated_tenure", Integer.class);
            dto.setUpdatedEmiAmount(updatedEmiAmount);
            dto.setUpdatedTenure(updatedTenure);
            return dto;
        };
    }

    private void ensurePreClosureTable() {
        if (preClosureTableChecked) {
            return;
        }
        synchronized (this) {
            if (preClosureTableChecked) {
                return;
            }
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS loan_preclosures (" +
                            "loan_id bigint PRIMARY KEY, " +
                            "pre_closure_date date NOT NULL, " +
                            "settlement_amount numeric(12,2) NOT NULL, " +
                            "pre_closure_type varchar(20) NOT NULL DEFAULT 'FULL', " +
                            "reference_number varchar(80), " +
                            "updated_emi_amount numeric(12,2), " +
                            "updated_tenure int4, " +
                            "created_at timestamp default CURRENT_TIMESTAMP, " +
                            "updated_at timestamp default CURRENT_TIMESTAMP" +
                            ")"
            );
            jdbcTemplate.execute("ALTER TABLE loan_preclosures ADD COLUMN IF NOT EXISTS pre_closure_type varchar(20) NOT NULL DEFAULT 'FULL'");
            jdbcTemplate.execute("ALTER TABLE loan_preclosures ADD COLUMN IF NOT EXISTS reference_number varchar(80)");
            jdbcTemplate.execute("ALTER TABLE loan_preclosures ADD COLUMN IF NOT EXISTS updated_emi_amount numeric(12,2)");
            jdbcTemplate.execute("ALTER TABLE loan_preclosures ADD COLUMN IF NOT EXISTS updated_tenure int4");
            preClosureTableChecked = true;
        }
    }

    private String safeTrim(String value) {
        return value == null ? null : value.trim();
    }

    private String resolveLoanStatus(LocalDate endDate) {
        if (endDate == null) {
            return "Open";
        }
        return LocalDate.now().isAfter(endDate) ? "Closed" : "Open";
    }

    private LoanSummaryDTO toSummaryDto(Loan loan) {
        LoanSummaryDTO dto = new LoanSummaryDTO();
        dto.setId(loan.getId());
        dto.setLoanAccountNumber(loan.getLoanAccountNumber());
        dto.setBankName(loan.getBankName());
        dto.setLoanType(loan.getLoanType());
        dto.setLoanAmount(loan.getLoanAmount());
        dto.setFormattedLoanAmount(formatterUtils.formatInIndianStyle(loan.getLoanAmount()));
        dto.setTenure(loan.getTenure());
        dto.setInterestRate(loan.getInterestRate());
        dto.setEmiAmount(loan.getEmiAmount());
        dto.setFormattedEmiAmount(formatterUtils.formatInIndianStyle(loan.getEmiAmount()));
        dto.setFirstEmiDate(loan.getEmiDate());
        dto.setFormattedFirstEmiDate(formatterUtils.formatDate(loan.getEmiDate()));
        LocalDate lastPaidDate = buildScheduleForLoan(loan, null).stream()
                .filter(r -> r.getDeductionDate() != null && !r.getDeductionDate().isAfter(LocalDate.now()))
                .map(LoanEmiScheduleRowDTO::getDeductionDate)
                .max(LocalDate::compareTo)
                .orElse(null);
        dto.setFormattedLastEmiPaidDate(formatterUtils.formatDate(lastPaidDate));

        Optional<LoanPreClosureDTO> preClosure = getPersistedPreClosure(loan.getId());
        if (preClosure.isPresent()) {
            dto.setPreClosed(true);
            dto.setPreClosureDate(preClosure.get().getPreClosureDate());
            dto.setFormattedPreClosureDate(formatterUtils.formatDate(preClosure.get().getPreClosureDate()));
            dto.setPreClosureAmount(preClosure.get().getSettlementAmount());
            dto.setFormattedPreClosureAmount(formatterUtils.formatInIndianStyle(preClosure.get().getSettlementAmount()));
            dto.setPreClosureType(preClosure.get().getPreClosureType());
            dto.setPreClosureReferenceNumber(preClosure.get().getReferenceNumber());
            LocalDate lastDate = getLastDueDateForLoan(loan);
            dto.setEndDate(lastDate);
            dto.setFormattedEndDate(formatterUtils.formatDate(lastDate));
            dto.setLoanStatus("PARTIAL".equalsIgnoreCase(preClosure.get().getPreClosureType())
                    ? "Partially Closed"
                    : "Pre-Closed");
            return dto;
        }
        if (loan.getEmiDate() != null && loan.getTenure() != null && loan.getTenure() > 0) {
            LocalDate endDate = getDueDateForInstallment(loan, loan.getTenure());
            dto.setEndDate(endDate);
            dto.setFormattedEndDate(formatterUtils.formatDate(endDate));
            dto.setLoanStatus(resolveLoanStatus(endDate));
        } else {
            dto.setLoanStatus("Open");
        }
        return dto;
    }

    private LoanEmiPaymentDTO toEmiPaymentDto(LoanEmiPayment payment) {
        LoanEmiPaymentDTO dto = new LoanEmiPaymentDTO();
        dto.setId(payment.getId());
        dto.setLoanId(payment.getLoanId());
        dto.setEmiNumber(payment.getEmiNumber());
        dto.setEmiAmount(payment.getEmiAmount());
        dto.setPaidOn(payment.getPaidOn());
        return dto;
    }
}
