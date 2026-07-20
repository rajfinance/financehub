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
import com.financehub.utils.LoanTypeUtils;
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
    /** Older DBs may still have NOT NULL foreclosure_ref_number alongside reference_number. */
    private volatile boolean hasLegacyForeclosureRefColumn = false;
    private volatile boolean hasLegacyNocNumberColumn = false;

    private record ScheduleAmountSlice(int emiNumber,
                                       LocalDate dueDate,
                                       double amount,
                                       LocalDate deductionDate,
                                       boolean recorded,
                                       Long overrideId) {
    }

    private static final class ScheduleContext {
        private final List<Loan> loans;
        private final Map<Long, Map<Integer, LoanEmiPayment>> overridesByLoan;
        private final Map<Long, LoanPreClosureDTO> preClosureByLoan;

        private ScheduleContext(List<Loan> loans,
                                Map<Long, Map<Integer, LoanEmiPayment>> overridesByLoan,
                                Map<Long, LoanPreClosureDTO> preClosureByLoan) {
            this.loans = loans;
            this.overridesByLoan = overridesByLoan;
            this.preClosureByLoan = preClosureByLoan;
        }
    }

    public static final class EmiScheduleReportBundle {
        private final List<LoanSummaryDTO> loanOptions;
        private final List<LoanEmiScheduleGroupDTO> scheduleGroups;
        private final String yearTotal;
        private final String yearPendingTotal;

        public EmiScheduleReportBundle(List<LoanSummaryDTO> loanOptions,
                                        List<LoanEmiScheduleGroupDTO> scheduleGroups,
                                        String yearTotal,
                                        String yearPendingTotal) {
            this.loanOptions = loanOptions;
            this.scheduleGroups = scheduleGroups;
            this.yearTotal = yearTotal;
            this.yearPendingTotal = yearPendingTotal;
        }

        public List<LoanSummaryDTO> getLoanOptions() {
            return loanOptions;
        }

        public List<LoanEmiScheduleGroupDTO> getScheduleGroups() {
            return scheduleGroups;
        }

        public String getYearTotal() {
            return yearTotal;
        }

        public String getYearPendingTotal() {
            return yearPendingTotal;
        }
    }

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
        dto.setGoldLoan(LoanTypeUtils.isGoldLoan(loan));
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
        ScheduleContext context = buildScheduleContext();
        return context.loans.stream()
                .map(loan -> toSummaryDto(
                        loan,
                        context.preClosureByLoan.get(loan.getId()),
                        context.overridesByLoan.getOrDefault(loan.getId(), Map.of())))
                .sorted(Comparator.comparingInt(this::loanStatusSortOrder)
                        .thenComparing(LoanSummaryDTO::getLoanAccountNumber, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
    }

    /** Open loans first; closed / pre-closed last. */
    private int loanStatusSortOrder(LoanSummaryDTO loan) {
        if (loan == null || loan.getLoanStatus() == null) {
            return 3;
        }
        return switch (loan.getLoanStatus()) {
            case "Open" -> 0;
            case "Partially Closed" -> 1;
            case "Pre-Closed", "Closed" -> 2;
            default -> 3;
        };
    }

    public LoanSummaryDTO getLoanSummaryById(Long loanId) {
        Loan loan = requireOwnedLoan(loanId);
        List<Long> loanIds = List.of(loan.getId());
        return toSummaryDto(
                loan,
                loadPreClosuresByLoanIds(loanIds).get(loan.getId()),
                loadOverridesByLoan(loanIds).getOrDefault(loan.getId(), Map.of()));
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
        for (int i = 1; i <= LoanTypeUtils.getScheduleInstallmentCount(loan); i++) {
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
        for (int i = 1; i <= LoanTypeUtils.getScheduleInstallmentCount(loan); i++) {
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
        return buildScheduleRows(buildScheduleContext(), year, loanId);
    }

    public EmiScheduleReportBundle buildEmiScheduleReport(Integer year, Long loanId) {
        ScheduleContext context = buildScheduleContext();
        List<Loan> eligibleLoans = context.loans.stream()
                .filter(loan -> isProjectionEligibleLoan(loan, context.preClosureByLoan.get(loan.getId())))
                .toList();
        Set<Long> eligibleIds = eligibleLoans.stream()
                .map(Loan::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Long effectiveLoanId = loanId;
        if (effectiveLoanId != null && !eligibleIds.contains(effectiveLoanId)) {
            effectiveLoanId = null;
        }

        List<LoanSummaryDTO> loanOptions = eligibleLoans.stream()
                .map(this::toLightSummaryDto)
                .toList();

        List<LoanEmiScheduleRowDTO> rows = buildScheduleRows(context, year, effectiveLoanId).stream()
                .filter(row -> eligibleIds.contains(row.getLoanId()))
                .toList();
        Map<Long, List<LoanEmiScheduleRowDTO>> grouped = rows.stream()
                .collect(Collectors.groupingBy(LoanEmiScheduleRowDTO::getLoanId, LinkedHashMap::new, Collectors.toList()));

        List<LoanEmiScheduleGroupDTO> groups = new ArrayList<>();
        for (Loan loan : eligibleLoans) {
            if (effectiveLoanId != null && !loan.getId().equals(effectiveLoanId)) {
                continue;
            }
            List<LoanEmiScheduleRowDTO> loanRows = grouped.get(loan.getId());
            if (loanRows == null || loanRows.isEmpty()) {
                continue;
            }
            LoanEmiScheduleGroupDTO group = new LoanEmiScheduleGroupDTO();
            group.setLoan(toLightSummaryDto(loan));
            group.setScheduleRows(loanRows);
            groups.add(group);
        }

        double total = sumScheduleAmounts(context, year, effectiveLoanId, false, eligibleIds);
        double pending = sumScheduleAmounts(context, year, effectiveLoanId, true, eligibleIds);
        return new EmiScheduleReportBundle(
                loanOptions,
                groups,
                formatterUtils.formatInIndianStyle(total),
                formatterUtils.formatInIndianStyle(pending));
    }

    public LoanBankEmiProjectionReportDTO getBankNextMonthProjectionReport(List<Long> selectedLoanIds,
                                                                            List<String> comboBanks,
                                                                            boolean selectionApplied) {
        ScheduleContext context = buildScheduleContext();
        List<Loan> eligibleLoans = context.loans.stream()
                .filter(loan -> isProjectionEligibleLoan(loan, context.preClosureByLoan.get(loan.getId())))
                .toList();

        Set<Long> eligibleIds = eligibleLoans.stream().map(Loan::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        List<Long> effectiveLoanIds;
        if (!selectionApplied) {
            effectiveLoanIds = new ArrayList<>(eligibleIds);
        } else if (selectedLoanIds == null || selectedLoanIds.isEmpty()) {
            effectiveLoanIds = List.of();
        } else {
            effectiveLoanIds = selectedLoanIds.stream()
                    .filter(eligibleIds::contains)
                    .distinct()
                    .toList();
        }

        List<Loan> selectedLoans = eligibleLoans.stream()
                .filter(loan -> effectiveLoanIds.contains(loan.getId()))
                .toList();

        LinkedHashSet<String> bankOrder = new LinkedHashSet<>();
        for (Loan loan : selectedLoans) {
            bankOrder.add(resolveBankBucket(loan.getBankName()));
        }
        List<String> banks = new ArrayList<>(bankOrder);

        List<String> effectiveComboBanks = new ArrayList<>();
        if (comboBanks != null) {
            for (String bank : comboBanks) {
                String key = normalizeBankKey(bank);
                // Allow single-bank or multi-bank combo as long as bank is in the report.
                if (banks.contains(key) && !effectiveComboBanks.contains(key)) {
                    effectiveComboBanks.add(key);
                }
            }
        }

        LocalDate nextMonthStart = LocalDate.now().plusMonths(1).withDayOfMonth(1);
        TreeMap<YearMonth, Map<String, Long>> monthlyEmiByBank = new TreeMap<>();
        Map<String, Long> pendingTotalByBank = new LinkedHashMap<>();
        for (String bank : banks) {
            pendingTotalByBank.put(bank, 0L);
        }

        for (Loan loan : selectedLoans) {
            if (loan.getEmiDate() == null || loan.getTenure() == null) {
                continue;
            }
            // Gold loans have no monthly EMI; still include their annual due in the due month.
            LoanPreClosureDTO preClosure = context.preClosureByLoan.get(loan.getId());
            Map<Integer, LoanEmiPayment> overrides = context.overridesByLoan.getOrDefault(loan.getId(), Map.of());
            String bucket = resolveBankBucket(loan.getBankName());
            for (ScheduleAmountSlice slice : buildScheduleAmountSlices(loan, null, preClosure, overrides)) {
                if (slice.dueDate() == null || slice.dueDate().isBefore(nextMonthStart)) {
                    continue;
                }
                long amount = Math.round(slice.amount());
                if (amount <= 0) {
                    continue;
                }
                YearMonth installmentMonth = YearMonth.from(slice.dueDate());
                monthlyEmiByBank
                        .computeIfAbsent(installmentMonth, ym -> {
                            Map<String, Long> bankMap = new LinkedHashMap<>();
                            for (String bank : banks) {
                                bankMap.put(bank, 0L);
                            }
                            return bankMap;
                        })
                        .merge(bucket, amount, Long::sum);
                pendingTotalByBank.merge(bucket, amount, Long::sum);
            }
        }

        Map<String, Long> runningPending = new LinkedHashMap<>(pendingTotalByBank);
        List<LoanBankEmiProjectionRowDTO> rows = new ArrayList<>();
        for (Map.Entry<YearMonth, Map<String, Long>> entry : monthlyEmiByBank.entrySet()) {
            Map<String, Long> monthEmi = entry.getValue();
            for (String bank : banks) {
                long monthAmount = monthEmi.getOrDefault(bank, 0L);
                runningPending.put(bank, Math.max(0L, runningPending.getOrDefault(bank, 0L) - monthAmount));
            }

            List<Long> amounts = new ArrayList<>();
            List<Long> pays = new ArrayList<>();
            long total = 0;
            long totalPay = 0;
            long comboPay = 0;
            for (String bank : banks) {
                long amount = runningPending.getOrDefault(bank, 0L);
                long pay = computeForeclosurePay(bank, amount);
                amounts.add(amount);
                pays.add(pay);
                total += amount;
                totalPay += pay;
                if (effectiveComboBanks.contains(bank)) {
                    comboPay += pay;
                }
            }

            LoanBankEmiProjectionRowDTO row = new LoanBankEmiProjectionRowDTO();
            row.setDate(formatterUtils.formatDate(entry.getKey().atDay(7)));
            row.setFormattedAmounts(amounts.stream()
                    .map(formatterUtils::formatInIndianStyleWholeNumber)
                    .toList());
            row.setFormattedPays(pays.stream()
                    .map(formatterUtils::formatInIndianStyleWholeNumber)
                    .toList());
            row.setFormattedTotalAmount(formatterUtils.formatInIndianStyleWholeNumber(total));
            row.setFormattedComboPayAmount(formatterUtils.formatInIndianStyleWholeNumber(comboPay));
            row.setFormattedTotalPayAmount(formatterUtils.formatInIndianStyleWholeNumber(totalPay));
            rows.add(row);
        }

        // Header EMI: use each bank's own next monthly EMI (not only values present in the first shared month).
        Map<String, Long> headerEmiByBank = resolveHeaderMonthlyEmiByBank(selectedLoans, banks, context, nextMonthStart);

        LoanBankEmiProjectionReportDTO report = new LoanBankEmiProjectionReportDTO();
        report.setEligibleLoans(eligibleLoans.stream().map(loan -> {
            LoanSummaryDTO dto = toLightSummaryDto(loan);
            dto.setGoldLoan(false);
            dto.setFormattedEmiAmount(formatterUtils.formatInIndianStyle(loan.getEmiAmount()));
            dto.setEmiAmount(loan.getEmiAmount());
            return dto;
        }).toList());
        report.setSelectedLoanIds(effectiveLoanIds);
        report.setBanks(banks);
        report.setFormattedHeaderEmis(banks.stream()
                .map(bank -> formatterUtils.formatInIndianStyleWholeNumber(headerEmiByBank.getOrDefault(bank, 0L)))
                .toList());
        report.setComboBanks(effectiveComboBanks);
        report.setComboLabel(effectiveComboBanks.isEmpty() ? "" : String.join(" + ", effectiveComboBanks));
        report.setRows(rows);
        return report;
    }

    /**
     * Next upcoming monthly EMI total per bank (from next month onward), so each bank header
     * shows its EMI even when the earliest shared projection month has no installment for that bank.
     */
    private Map<String, Long> resolveHeaderMonthlyEmiByBank(List<Loan> selectedLoans,
                                                            List<String> banks,
                                                            ScheduleContext context,
                                                            LocalDate nextMonthStart) {
        Map<String, Long> headerEmiByBank = new LinkedHashMap<>();
        for (String bank : banks) {
            headerEmiByBank.put(bank, 0L);
        }
        Map<String, YearMonth> firstMonthByBank = new HashMap<>();
        Map<String, Long> firstMonthAmountByBank = new HashMap<>();

        for (Loan loan : selectedLoans) {
            if (loan.getEmiDate() == null || loan.getTenure() == null) {
                continue;
            }
            String bucket = resolveBankBucket(loan.getBankName());
            LoanPreClosureDTO preClosure = context.preClosureByLoan.get(loan.getId());
            Map<Integer, LoanEmiPayment> overrides = context.overridesByLoan.getOrDefault(loan.getId(), Map.of());
            for (ScheduleAmountSlice slice : buildScheduleAmountSlices(loan, null, preClosure, overrides)) {
                if (slice.dueDate() == null || slice.dueDate().isBefore(nextMonthStart)) {
                    continue;
                }
                long amount = Math.round(slice.amount());
                if (amount <= 0) {
                    continue;
                }
                YearMonth ym = YearMonth.from(slice.dueDate());
                YearMonth currentFirst = firstMonthByBank.get(bucket);
                if (currentFirst == null || ym.isBefore(currentFirst)) {
                    firstMonthByBank.put(bucket, ym);
                    firstMonthAmountByBank.put(bucket, amount);
                } else if (ym.equals(currentFirst)) {
                    firstMonthAmountByBank.merge(bucket, amount, Long::sum);
                }
            }
        }
        for (String bank : banks) {
            headerEmiByBank.put(bank, firstMonthAmountByBank.getOrDefault(bank, 0L));
        }
        return headerEmiByBank;
    }

    private boolean isProjectionEligibleLoan(Loan loan, LoanPreClosureDTO preClosure) {
        if (LoanTypeUtils.isGoldLoan(loan)) {
            return false;
        }
        if (loan.getEmiDate() == null || loan.getTenure() == null || loan.getEmiAmount() == null || loan.getEmiAmount() <= 0) {
            return false;
        }
        if (preClosure != null && !"PARTIAL".equalsIgnoreCase(preClosure.getPreClosureType())) {
            return false;
        }
        return true;
    }

    public int getCurrentYearPendingEmiAmount() {
        int year = LocalDate.now().getYear();
        return (int) Math.round(sumScheduleAmounts(buildScheduleContext(), year, null, true));
    }

    public List<Integer> getScheduleYearsForUser() {
        ScheduleContext context = buildScheduleContext();
        Set<Integer> years = new TreeSet<>();
        int currentYear = LocalDate.now().getYear();
        years.add(currentYear);
        for (Loan loan : context.loans) {
            if (!isProjectionEligibleLoan(loan, context.preClosureByLoan.get(loan.getId()))) {
                continue;
            }
            LocalDate start = loan.getEmiDate();
            LocalDate end = getLastDueDateForLoan(loan, context.preClosureByLoan.get(loan.getId()));
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
        LocalDate end = getLastDueDateForLoan(loan, getPersistedPreClosure(loanId).orElse(null));
        for (int y = start.getYear(); y <= end.getYear(); y++) {
            years.add(y);
        }
        return new ArrayList<>(years);
    }

    public LocalDate getDueDateForInstallment(Loan loan, int emiNumber) {
        return getDueDateForInstallment(loan, emiNumber, getPersistedPreClosure(loan.getId()).orElse(null));
    }

    private LocalDate getDueDateForInstallment(Loan loan, int emiNumber, LoanPreClosureDTO preClosure) {
        if (preClosure != null
                && "PARTIAL".equalsIgnoreCase(preClosure.getPreClosureType())
                && preClosure.getUpdatedTenure() != null
                && preClosure.getUpdatedTenure() > 0
                && preClosure.getPreClosureDate() != null
                && !LoanTypeUtils.isGoldLoan(loan)) {
            int cutOffEmiNumber = resolvePreClosureTargetEmiNumber(loan, preClosure.getPreClosureDate());
            if (emiNumber > cutOffEmiNumber) {
                LocalDate cutOffDueDate = shiftInstallmentDate(loan, loan.getEmiDate(), cutOffEmiNumber - 1);
                return shiftInstallmentDate(loan, cutOffDueDate, emiNumber - cutOffEmiNumber);
            }
        }
        return shiftInstallmentDate(loan, loan.getEmiDate(), emiNumber - 1);
    }

    private LocalDate shiftInstallmentDate(Loan loan, LocalDate baseDate, int periodOffset) {
        if (baseDate == null) {
            return null;
        }
        if (LoanTypeUtils.isGoldLoan(loan)) {
            int tenureMonths = loan.getTenure() != null && loan.getTenure() > 0
                    ? loan.getTenure()
                    : LoanTypeUtils.goldLoanTenureMonths();
            int installmentCount = LoanTypeUtils.goldLoanInstallmentCount();
            long emiNumber = periodOffset + 1L;
            return baseDate.plusMonths(emiNumber * tenureMonths / installmentCount);
        }
        return baseDate.plusMonths(periodOffset);
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
        throw new IllegalArgumentException(LoanTypeUtils.isGoldLoan(loan)
                ? "Payment date does not match the annual due date for this gold loan."
                : "Deduction date does not match any EMI month for this loan. Select the correct installment number.");
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

    private ScheduleContext buildScheduleContext() {
        long userId = requireUserId();
        List<Loan> loans = loanRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<Long> loanIds = loans.stream().map(Loan::getId).toList();
        return new ScheduleContext(loans, loadOverridesByLoan(loanIds), loadPreClosuresByLoanIds(loanIds));
    }

    private List<LoanEmiScheduleRowDTO> buildScheduleRows(ScheduleContext context, Integer year, Long loanId) {
        List<LoanEmiScheduleRowDTO> rows = new ArrayList<>();
        for (Loan loan : context.loans) {
            if (loanId != null && !loan.getId().equals(loanId)) {
                continue;
            }
            rows.addAll(buildScheduleForLoan(
                    loan,
                    year,
                    context.preClosureByLoan.get(loan.getId()),
                    context.overridesByLoan.getOrDefault(loan.getId(), Map.of())));
        }
        rows.sort(Comparator
                .comparing(LoanEmiScheduleRowDTO::getDueDate)
                .thenComparing(LoanEmiScheduleRowDTO::getBankName)
                .thenComparing(LoanEmiScheduleRowDTO::getEmiNumber));
        return rows;
    }

    private Map<Long, Map<Integer, LoanEmiPayment>> loadOverridesByLoan(List<Long> loanIds) {
        if (loanIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Map<Integer, LoanEmiPayment>> overridesByLoan = new HashMap<>();
        for (LoanEmiPayment payment : loanEmiPaymentRepository
                .findByLoanIdInAndEmiNumberGreaterThanOrderByLoanIdAscEmiNumberAsc(loanIds, 0)) {
            if (payment.getEmiNumber() == null) {
                continue;
            }
            overridesByLoan
                    .computeIfAbsent(payment.getLoanId(), ignored -> new HashMap<>())
                    .put(payment.getEmiNumber(), payment);
        }
        return overridesByLoan;
    }

    private Map<Long, LoanPreClosureDTO> loadPreClosuresByLoanIds(List<Long> loanIds) {
        if (loanIds.isEmpty()) {
            return Map.of();
        }
        ensurePreClosureTable();
        String placeholders = String.join(",", Collections.nCopies(loanIds.size(), "?"));
        List<LoanPreClosureDTO> rows = jdbcTemplate.query(
                "SELECT loan_id, pre_closure_date, settlement_amount, pre_closure_type, " +
                        preClosureReferenceSelectExpr() + " AS reference_number, " +
                        "updated_emi_amount, updated_tenure " +
                        "FROM loan_preclosures WHERE loan_id IN (" + placeholders + ")",
                preClosureRowMapper(),
                loanIds.toArray());
        Map<Long, LoanPreClosureDTO> preClosureByLoan = new HashMap<>();
        for (LoanPreClosureDTO row : rows) {
            preClosureByLoan.putIfAbsent(row.getLoanId(), row);
        }
        return preClosureByLoan;
    }

    private double sumScheduleAmounts(ScheduleContext context, Integer year, Long loanId, boolean pendingOnly) {
        return sumScheduleAmounts(context, year, loanId, pendingOnly, null);
    }

    private double sumScheduleAmounts(ScheduleContext context,
                                      Integer year,
                                      Long loanId,
                                      boolean pendingOnly,
                                      Set<Long> allowedLoanIds) {
        LocalDate today = LocalDate.now();
        double total = 0;
        for (Loan loan : context.loans) {
            if (loanId != null && !loan.getId().equals(loanId)) {
                continue;
            }
            if (allowedLoanIds != null && !allowedLoanIds.contains(loan.getId())) {
                continue;
            }
            for (ScheduleAmountSlice slice : buildScheduleAmountSlices(
                    loan,
                    year,
                    context.preClosureByLoan.get(loan.getId()),
                    context.overridesByLoan.getOrDefault(loan.getId(), Map.of()))) {
                if (pendingOnly && (slice.deductionDate() == null || !slice.deductionDate().isAfter(today))) {
                    continue;
                }
                total += slice.amount();
            }
        }
        return total;
    }

    private List<ScheduleAmountSlice> buildScheduleAmountSlices(Loan loan,
                                                                Integer year,
                                                                LoanPreClosureDTO preClosure,
                                                                Map<Integer, LoanEmiPayment> overrides) {
        if (loan.getEmiDate() == null || loan.getTenure() == null || loan.getEmiAmount() == null) {
            return List.of();
        }
        LocalDate preClosureDate = preClosure != null ? preClosure.getPreClosureDate() : null;
        Integer preClosureEmiNumber = preClosureDate != null
                ? resolvePreClosureTargetEmiNumber(loan, preClosureDate, preClosure)
                : null;
        boolean partialClosure = preClosure != null && "PARTIAL".equalsIgnoreCase(preClosure.getPreClosureType())
                && preClosure.getUpdatedTenure() != null && preClosure.getUpdatedTenure() > 0
                && preClosure.getUpdatedEmiAmount() != null && preClosure.getUpdatedEmiAmount() > 0;
        int maxEmiNumber = partialClosure
                ? preClosureEmiNumber + preClosure.getUpdatedTenure()
                : LoanTypeUtils.getScheduleInstallmentCount(loan);

        List<ScheduleAmountSlice> slices = new ArrayList<>();
        for (int i = 1; i <= maxEmiNumber; i++) {
            LocalDate dueDate = getDueDateForInstallment(loan, i, preClosure);
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
            Long overrideId = recorded ? override.getId() : null;
            slices.add(new ScheduleAmountSlice(i, dueDate, amount, deductionDate, recorded, overrideId));
        }
        return slices;
    }

    private List<LoanEmiScheduleRowDTO> buildScheduleForLoan(Loan loan,
                                                               Integer year,
                                                               LoanPreClosureDTO preClosure,
                                                               Map<Integer, LoanEmiPayment> overrides) {
        List<LoanEmiScheduleRowDTO> rows = new ArrayList<>();
        LocalDate preClosureDate = preClosure != null ? preClosure.getPreClosureDate() : null;
        Integer preClosureEmiNumber = preClosureDate != null
                ? resolvePreClosureTargetEmiNumber(loan, preClosureDate, preClosure)
                : null;

        for (ScheduleAmountSlice slice : buildScheduleAmountSlices(loan, year, preClosure, overrides)) {
            LoanEmiScheduleRowDTO row = new LoanEmiScheduleRowDTO();
            row.setLoanId(loan.getId());
            row.setLoanAccountNumber(loan.getLoanAccountNumber());
            row.setBankName(loan.getBankName());
            row.setLoanType(loan.getLoanType());
            row.setEmiNumber(slice.emiNumber());
            row.setDueDate(slice.dueDate());
            row.setFormattedDueDate(formatterUtils.formatDate(slice.dueDate()));
            row.setDeductionDate(slice.deductionDate());
            row.setFormattedDeductionDate(formatterUtils.formatDate(slice.deductionDate()));
            row.setEmiAmount(slice.amount());
            row.setFormattedEmiAmount(formatterUtils.formatInIndianStyle(slice.amount()));
            String emiStatus = resolveEmiStatus(slice.deductionDate());
            if (preClosureEmiNumber != null && slice.emiNumber() == preClosureEmiNumber) {
                emiStatus = "Pre-Closure";
            }
            row.setEmiStatus(emiStatus);
            row.setRecorded(slice.recorded());
            row.setOverrideId(slice.overrideId());
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
        if (LoanTypeUtils.isGoldLoan(loanDto.getLoanType())) {
            if (loanDto.getEmiDate() == null) {
                throw new IllegalArgumentException("Loan start date is required for gold loans.");
            }
            loanDto.setTenure(LoanTypeUtils.goldLoanTenureMonths());
            if (loanDto.getEmiAmount() == null || loanDto.getEmiAmount() < 0) {
                loanDto.setEmiAmount(0.0);
            }
            return;
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
        if (LoanTypeUtils.isGoldLoan(loanDto.getLoanType())) {
            loan.setTenure(LoanTypeUtils.goldLoanTenureMonths());
            loan.setEmiDate(loanDto.getEmiDate());
            loan.setEmiAmount(loanDto.getEmiAmount() == null ? 0.0 : loanDto.getEmiAmount());
        } else {
            loan.setEmiAmount(loanDto.getEmiAmount());
            loan.setTenure(loanDto.getTenure());
            loan.setEmiDate(loanDto.getEmiDate());
        }
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

    private long computeForeclosurePay(String bank, long amount) {
        if (amount <= 0) {
            return 0L;
        }
        if ("AXIS".equals(bank)) {
            return (long) Math.ceil(amount + (amount * 0.05) + (amount * 0.05 * 0.12));
        }
        if ("HDFC".equals(bank)) {
            return (long) Math.ceil(amount + (amount * 0.04) + (amount * 0.04 * 0.12));
        }
        // ICICI and others: outstanding as-is
        return amount;
    }

    private String resolveBankBucket(String bankName) {
        if (bankName == null || bankName.isBlank()) {
            return "OTHER";
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
        if (normalized.contains("state bank") || normalized.contains("sbi")) {
            return "SBI";
        }
        if (normalized.contains("kotak")) {
            return "KOTAK";
        }
        return "OTHER";
    }

    private String normalizeBankKey(String bank) {
        if (bank == null || bank.isBlank()) {
            return "OTHER";
        }
        return bank.trim().toUpperCase(Locale.ROOT);
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
                && preClosure.get().getPreClosureDate() != null
                && !LoanTypeUtils.isGoldLoan(loan)) {
            int cutOffEmiNumber = resolvePreClosureTargetEmiNumber(loan, preClosure.get().getPreClosureDate());
            return cutOffEmiNumber + preClosure.get().getUpdatedTenure();
        }
        return LoanTypeUtils.getScheduleInstallmentCount(loan);
    }

    private int resolvePreClosureTargetEmiNumber(Loan loan, LocalDate preClosureDate) {
        return resolvePreClosureTargetEmiNumber(loan, preClosureDate, getPersistedPreClosure(loan.getId()).orElse(null));
    }

    private int resolvePreClosureTargetEmiNumber(Loan loan, LocalDate preClosureDate, LoanPreClosureDTO preClosure) {
        int maxSearch = LoanTypeUtils.getScheduleInstallmentCount(loan);
        if (preClosure != null
                && "PARTIAL".equalsIgnoreCase(preClosure.getPreClosureType())
                && preClosure.getUpdatedTenure() != null
                && preClosure.getUpdatedTenure() > 0
                && !LoanTypeUtils.isGoldLoan(loan)) {
            int cutOffEmiNumber = 1;
            for (int i = 1; i <= LoanTypeUtils.getScheduleInstallmentCount(loan); i++) {
                LocalDate dueDate = getDueDateForInstallment(loan, i, preClosure);
                if (!dueDate.isBefore(preClosureDate)) {
                    cutOffEmiNumber = i;
                    break;
                }
                cutOffEmiNumber = i;
            }
            maxSearch = Math.max(maxSearch, cutOffEmiNumber + preClosure.getUpdatedTenure());
        }
        for (int i = 1; i <= maxSearch; i++) {
            LocalDate dueDate = getDueDateForInstallment(loan, i, preClosure);
            if (!dueDate.isBefore(preClosureDate)) {
                return i;
            }
        }
        return Math.max(1, LoanTypeUtils.getScheduleInstallmentCount(loan));
    }

    private int getMaxAllowedEmiNumberWithoutRecursion(Loan loan) {
        Optional<LoanPreClosureDTO> preClosure = getPersistedPreClosure(loan.getId());
        if (preClosure.isPresent()
                && "PARTIAL".equalsIgnoreCase(preClosure.get().getPreClosureType())
                && preClosure.get().getUpdatedTenure() != null
                && preClosure.get().getUpdatedTenure() > 0
                && preClosure.get().getPreClosureDate() != null
                && !LoanTypeUtils.isGoldLoan(loan)) {
            int cutOffEmiNumber = 1;
            for (int i = 1; i <= LoanTypeUtils.getScheduleInstallmentCount(loan); i++) {
                LocalDate dueDate = getDueDateForInstallment(loan, i, preClosure.get());
                if (!dueDate.isBefore(preClosure.get().getPreClosureDate())) {
                    cutOffEmiNumber = i;
                    break;
                }
                cutOffEmiNumber = i;
            }
            return cutOffEmiNumber + preClosure.get().getUpdatedTenure();
        }
        return LoanTypeUtils.getScheduleInstallmentCount(loan);
    }

    private LocalDate getLastDueDateForLoan(Loan loan, LoanPreClosureDTO preClosure) {
        int maxEmiNumber = LoanTypeUtils.getScheduleInstallmentCount(loan);
        if (preClosure != null && preClosure.getPreClosureDate() != null) {
            int cutOffEmiNumber = resolvePreClosureTargetEmiNumber(loan, preClosure.getPreClosureDate(), preClosure);
            if ("PARTIAL".equalsIgnoreCase(preClosure.getPreClosureType())
                    && preClosure.getUpdatedTenure() != null
                    && preClosure.getUpdatedTenure() > 0) {
                maxEmiNumber = cutOffEmiNumber + preClosure.getUpdatedTenure();
            } else {
                maxEmiNumber = cutOffEmiNumber;
            }
        }
        return getDueDateForInstallment(loan, maxEmiNumber, preClosure);
    }

    private Optional<LoanPreClosureDTO> getPersistedPreClosure(Long loanId) {
        ensurePreClosureTable();
        List<LoanPreClosureDTO> rows = jdbcTemplate.query(
                "SELECT loan_id, pre_closure_date, settlement_amount, pre_closure_type, " +
                        preClosureReferenceSelectExpr() + " AS reference_number, " +
                        "updated_emi_amount, updated_tenure " +
                        "FROM loan_preclosures WHERE loan_id = ?",
                preClosureRowMapper(),
                loanId
        );
        return rows.stream().findFirst();
    }

    private void upsertPreClosureDetails(LoanPreClosureDTO dto) {
        ensurePreClosureTable();
        String referenceNumber = safeTrim(dto.getReferenceNumber());
        if (referenceNumber == null || referenceNumber.isEmpty()) {
            throw new IllegalArgumentException("NOC / Foreclosure number is required for pre-closure.");
        }
        int updated;
        if (hasLegacyForeclosureRefColumn || hasLegacyNocNumberColumn) {
            StringBuilder updateSql = new StringBuilder(
                    "UPDATE loan_preclosures SET pre_closure_date = ?, settlement_amount = ?, " +
                            "pre_closure_type = ?, reference_number = ?");
            List<Object> params = new ArrayList<>();
            params.add(dto.getPreClosureDate());
            params.add(dto.getSettlementAmount());
            params.add(safeTrim(dto.getPreClosureType()));
            params.add(referenceNumber);
            if (hasLegacyForeclosureRefColumn) {
                updateSql.append(", foreclosure_ref_number = ?");
                params.add(referenceNumber);
            }
            if (hasLegacyNocNumberColumn) {
                updateSql.append(", noc_number = ?");
                params.add(referenceNumber);
            }
            updateSql.append(", updated_emi_amount = ?, updated_tenure = ?, updated_at = CURRENT_TIMESTAMP WHERE loan_id = ?");
            params.add(dto.getUpdatedEmiAmount());
            params.add(dto.getUpdatedTenure());
            params.add(dto.getLoanId());
            updated = jdbcTemplate.update(updateSql.toString(), params.toArray());
        } else {
            updated = jdbcTemplate.update(
                    "UPDATE loan_preclosures SET pre_closure_date = ?, settlement_amount = ?, " +
                            "pre_closure_type = ?, reference_number = ?, updated_emi_amount = ?, updated_tenure = ?, " +
                            "updated_at = CURRENT_TIMESTAMP WHERE loan_id = ?",
                    dto.getPreClosureDate(),
                    dto.getSettlementAmount(),
                    safeTrim(dto.getPreClosureType()),
                    referenceNumber,
                    dto.getUpdatedEmiAmount(),
                    dto.getUpdatedTenure(),
                    dto.getLoanId()
            );
        }
        if (updated == 0) {
            if (hasLegacyForeclosureRefColumn || hasLegacyNocNumberColumn) {
                StringBuilder insertCols = new StringBuilder(
                        "INSERT INTO loan_preclosures (loan_id, pre_closure_date, settlement_amount, pre_closure_type, reference_number");
                StringBuilder insertVals = new StringBuilder("VALUES (?, ?, ?, ?, ?");
                List<Object> params = new ArrayList<>();
                params.add(dto.getLoanId());
                params.add(dto.getPreClosureDate());
                params.add(dto.getSettlementAmount());
                params.add(safeTrim(dto.getPreClosureType()));
                params.add(referenceNumber);
                if (hasLegacyForeclosureRefColumn) {
                    insertCols.append(", foreclosure_ref_number");
                    insertVals.append(", ?");
                    params.add(referenceNumber);
                }
                if (hasLegacyNocNumberColumn) {
                    insertCols.append(", noc_number");
                    insertVals.append(", ?");
                    params.add(referenceNumber);
                }
                insertCols.append(", updated_emi_amount, updated_tenure, created_at, updated_at) ");
                insertVals.append(", ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
                params.add(dto.getUpdatedEmiAmount());
                params.add(dto.getUpdatedTenure());
                jdbcTemplate.update(insertCols.append(insertVals).toString(), params.toArray());
            } else {
                jdbcTemplate.update(
                        "INSERT INTO loan_preclosures (loan_id, pre_closure_date, settlement_amount, pre_closure_type, " +
                                "reference_number, updated_emi_amount, updated_tenure, created_at, updated_at) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                        dto.getLoanId(),
                        dto.getPreClosureDate(),
                        dto.getSettlementAmount(),
                        safeTrim(dto.getPreClosureType()),
                        referenceNumber,
                        dto.getUpdatedEmiAmount(),
                        dto.getUpdatedTenure()
                );
            }
        }
    }

    private String preClosureReferenceSelectExpr() {
        if (hasLegacyForeclosureRefColumn || hasLegacyNocNumberColumn) {
            StringBuilder expr = new StringBuilder("COALESCE(NULLIF(TRIM(reference_number), '')");
            if (hasLegacyForeclosureRefColumn) {
                expr.append(", NULLIF(TRIM(foreclosure_ref_number), '')");
            }
            if (hasLegacyNocNumberColumn) {
                expr.append(", NULLIF(TRIM(noc_number), '')");
            }
            expr.append(")");
            return expr.toString();
        }
        return "reference_number";
    }

    private RowMapper<LoanPreClosureDTO> preClosureRowMapper() {
        return (ResultSet rs, int rowNum) -> {
            LoanPreClosureDTO dto = new LoanPreClosureDTO();
            dto.setLoanId(rs.getLong("loan_id"));
            dto.setPreClosureDate(rs.getObject("pre_closure_date", LocalDate.class));
            dto.setSettlementAmount(rs.getDouble("settlement_amount"));
            dto.setPreClosureType(rs.getString("pre_closure_type"));
            dto.setReferenceNumber(rs.getString("reference_number"));
            // PostgreSQL numeric cannot be read with getObject(..., Double.class)
            java.math.BigDecimal updatedEmi = rs.getBigDecimal("updated_emi_amount");
            dto.setUpdatedEmiAmount(updatedEmi == null ? null : updatedEmi.doubleValue());
            int updatedTenure = rs.getInt("updated_tenure");
            dto.setUpdatedTenure(rs.wasNull() ? null : updatedTenure);
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

            Boolean legacyRefExists = jdbcTemplate.queryForObject(
                    "SELECT EXISTS (" +
                            "SELECT 1 FROM information_schema.columns " +
                            "WHERE table_schema = current_schema() " +
                            "AND table_name = 'loan_preclosures' " +
                            "AND column_name = 'foreclosure_ref_number'" +
                            ")",
                    Boolean.class);
            Boolean legacyNocExists = jdbcTemplate.queryForObject(
                    "SELECT EXISTS (" +
                            "SELECT 1 FROM information_schema.columns " +
                            "WHERE table_schema = current_schema() " +
                            "AND table_name = 'loan_preclosures' " +
                            "AND column_name = 'noc_number'" +
                            ")",
                    Boolean.class);
            hasLegacyForeclosureRefColumn = Boolean.TRUE.equals(legacyRefExists);
            hasLegacyNocNumberColumn = Boolean.TRUE.equals(legacyNocExists);
            if (hasLegacyForeclosureRefColumn) {
                // Older schemas require foreclosure_ref_number NOT NULL; keep both columns aligned.
                try {
                    jdbcTemplate.execute("ALTER TABLE loan_preclosures ALTER COLUMN foreclosure_ref_number DROP NOT NULL");
                } catch (Exception ignored) {
                    // Column may already be nullable, or user lacks ALTER privilege — upsert still writes both columns.
                }
                jdbcTemplate.update(
                        "UPDATE loan_preclosures SET reference_number = COALESCE(NULLIF(TRIM(reference_number), ''), foreclosure_ref_number) " +
                                "WHERE COALESCE(NULLIF(TRIM(reference_number), ''), '') = '' " +
                                "AND COALESCE(NULLIF(TRIM(foreclosure_ref_number), ''), '') <> ''");
                jdbcTemplate.update(
                        "UPDATE loan_preclosures SET foreclosure_ref_number = COALESCE(NULLIF(TRIM(foreclosure_ref_number), ''), reference_number) " +
                                "WHERE COALESCE(NULLIF(TRIM(foreclosure_ref_number), ''), '') = '' " +
                                "AND COALESCE(NULLIF(TRIM(reference_number), ''), '') <> ''");
            }
            if (hasLegacyNocNumberColumn) {
                jdbcTemplate.update(
                        "UPDATE loan_preclosures SET reference_number = COALESCE(NULLIF(TRIM(reference_number), ''), noc_number) " +
                                "WHERE COALESCE(NULLIF(TRIM(reference_number), ''), '') = '' " +
                                "AND COALESCE(NULLIF(TRIM(noc_number), ''), '') <> ''");
                jdbcTemplate.update(
                        "UPDATE loan_preclosures SET noc_number = COALESCE(NULLIF(TRIM(noc_number), ''), reference_number) " +
                                "WHERE COALESCE(NULLIF(TRIM(noc_number), ''), '') = '' " +
                                "AND COALESCE(NULLIF(TRIM(reference_number), ''), '') <> ''");
            }
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

    private LoanSummaryDTO toLightSummaryDto(Loan loan) {
        LoanSummaryDTO dto = new LoanSummaryDTO();
        dto.setId(loan.getId());
        dto.setLoanAccountNumber(loan.getLoanAccountNumber());
        dto.setBankName(loan.getBankName());
        dto.setLoanType(loan.getLoanType());
        return dto;
    }

    private LoanSummaryDTO toSummaryDto(Loan loan,
                                        LoanPreClosureDTO preClosure,
                                        Map<Integer, LoanEmiPayment> overrides) {
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
        boolean goldLoan = LoanTypeUtils.isGoldLoan(loan);
        dto.setGoldLoan(goldLoan);
        dto.setFormattedTenureLabel(loan.getTenure() + " months");
        if (goldLoan) {
            dto.setFormattedFullAmountToPay(formatGoldFullAmountToPay(loan));
        }
        if (goldLoan && (loan.getEmiAmount() == null || loan.getEmiAmount() <= 0)) {
            dto.setFormattedEmiAmount("-");
        } else {
            dto.setFormattedEmiAmount(formatterUtils.formatInIndianStyle(loan.getEmiAmount()));
        }
        dto.setFirstEmiDate(loan.getEmiDate());
        dto.setFormattedFirstEmiDate(formatterUtils.formatDate(loan.getEmiDate()));

        if (preClosure != null) {
            dto.setPreClosed(true);
            dto.setPreClosureDate(preClosure.getPreClosureDate());
            dto.setFormattedPreClosureDate(formatterUtils.formatDate(preClosure.getPreClosureDate()));
            dto.setPreClosureAmount(preClosure.getSettlementAmount());
            dto.setFormattedPreClosureAmount(formatterUtils.formatInIndianStyle(preClosure.getSettlementAmount()));
            dto.setPreClosureType(preClosure.getPreClosureType());
            dto.setPreClosureReferenceNumber(preClosure.getReferenceNumber());
            LocalDate lastDate = getLastDueDateForLoan(loan, preClosure);
            dto.setEndDate(lastDate);
            dto.setFormattedEndDate(formatterUtils.formatDate(lastDate));
            // For pre-closed loans, Last EMI Paid Date is the pre-closure date.
            dto.setFormattedLastEmiPaidDate(formatterUtils.formatDate(preClosure.getPreClosureDate()));
            dto.setLoanStatus("PARTIAL".equalsIgnoreCase(preClosure.getPreClosureType())
                    ? "Partially Closed"
                    : "Pre-Closed");
            return dto;
        }

        LocalDate lastPaidDate = resolveLastEmiPaidDate(loan, overrides);
        dto.setFormattedLastEmiPaidDate(formatterUtils.formatDate(lastPaidDate));

        if (loan.getEmiDate() != null && loan.getTenure() != null && loan.getTenure() > 0) {
            LocalDate endDate = getDueDateForInstallment(loan, LoanTypeUtils.getScheduleInstallmentCount(loan), null);
            dto.setEndDate(endDate);
            dto.setFormattedEndDate(formatterUtils.formatDate(endDate));
            dto.setLoanStatus(resolveLoanStatus(endDate));
        } else {
            dto.setLoanStatus("Open");
        }
        return dto;
    }

    /**
     * Last EMI paid date for open loans:
     * 1) Latest recorded deduction date from loan_emi_payments, if any
     * 2) Otherwise the latest installment due date that is on or before today
     *    (EMIs treated as paid by schedule when no manual record exists)
     */
    private LocalDate resolveLastEmiPaidDate(Loan loan, Map<Integer, LoanEmiPayment> overrides) {
        LocalDate recordedLastPaid = overrides.values().stream()
                .filter(p -> p.getPaidOn() != null && p.getEmiNumber() != null && p.getEmiNumber() > 0)
                .map(LoanEmiPayment::getPaidOn)
                .max(LocalDate::compareTo)
                .orElse(null);
        if (recordedLastPaid != null) {
            return recordedLastPaid;
        }
        if (loan.getEmiDate() == null || loan.getTenure() == null || loan.getTenure() < 1) {
            return null;
        }
        LocalDate today = LocalDate.now();
        LocalDate lastDueOnOrBeforeToday = null;
        int installmentCount = LoanTypeUtils.getScheduleInstallmentCount(loan);
        for (int i = 1; i <= installmentCount; i++) {
            LocalDate dueDate = getDueDateForInstallment(loan, i, null);
            if (dueDate == null) {
                continue;
            }
            if (dueDate.isAfter(today)) {
                break;
            }
            lastDueOnOrBeforeToday = dueDate;
        }
        return lastDueOnOrBeforeToday;
    }

    private String formatGoldFullAmountToPay(Loan loan) {
        double principal = loan.getLoanAmount() == null ? 0 : loan.getLoanAmount();
        double ratePercent = loan.getInterestRate() == null ? 0 : loan.getInterestRate();
        double interest = principal * (ratePercent / 100.0);
        return formatterUtils.formatInIndianStyle(principal + interest);
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
