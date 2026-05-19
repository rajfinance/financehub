package com.financehub.services;

import com.financehub.dtos.LoanDTO;
import com.financehub.dtos.LoanEmiPaymentDTO;
import com.financehub.dtos.LoanEmiScheduleGroupDTO;
import com.financehub.dtos.LoanEmiScheduleRowDTO;
import com.financehub.dtos.LoanSummaryDTO;
import com.financehub.entities.Loan;
import com.financehub.entities.LoanEmiPayment;
import com.financehub.repositories.LoanEmiPaymentRepository;
import com.financehub.repositories.LoanRepository;
import com.financehub.utils.FormatterUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final LoanEmiPaymentRepository loanEmiPaymentRepository;
    private final UserService userService;
    private final FormatterUtils formatterUtils;

    public LoanService(LoanRepository loanRepository,
                       LoanEmiPaymentRepository loanEmiPaymentRepository,
                       UserService userService,
                       FormatterUtils formatterUtils) {
        this.loanRepository = loanRepository;
        this.loanEmiPaymentRepository = loanEmiPaymentRepository;
        this.userService = userService;
        this.formatterUtils = formatterUtils;
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

    @Transactional
    public void saveEmiPayment(LoanEmiPaymentDTO dto) {
        long userId = requireUserId();
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
        if (dto.getEmiNumber() > loan.getTenure()) {
            throw new IllegalArgumentException("EMI number cannot exceed loan tenure (" + loan.getTenure() + " months).");
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
            LocalDate end = start.plusMonths(loan.getTenure() - 1L);
            for (int y = start.getYear(); y <= end.getYear(); y++) {
                years.add(y);
            }
        }
        return new ArrayList<>(years);
    }

    public LocalDate getDueDateForInstallment(Loan loan, int emiNumber) {
        return loan.getEmiDate().plusMonths(emiNumber - 1L);
    }

    public int resolveEmiNumberFromDate(Loan loan, LocalDate paidOn) {
        for (int i = 1; i <= loan.getTenure(); i++) {
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
        if (dto.getPaidOn() == null && dto.getEmiNumber() != null && dto.getEmiNumber() >= 1) {
            dto.setPaidOn(getDueDateForInstallment(loan, dto.getEmiNumber()));
        }
    }

    private List<LoanEmiScheduleRowDTO> buildScheduleForLoan(Loan loan, Integer year) {
        if (loan.getEmiDate() == null || loan.getTenure() == null || loan.getEmiAmount() == null) {
            return List.of();
        }
        Map<Integer, LoanEmiPayment> overrides = loanEmiPaymentRepository.findByLoanIdOrderByEmiNumberAsc(loan.getId())
                .stream()
                .collect(Collectors.toMap(LoanEmiPayment::getEmiNumber, p -> p, (a, b) -> b));

        List<LoanEmiScheduleRowDTO> rows = new ArrayList<>();
        for (int i = 1; i <= loan.getTenure(); i++) {
            LocalDate dueDate = getDueDateForInstallment(loan, i);
            if (year != null && dueDate.getYear() != year) {
                continue;
            }
            LoanEmiPayment override = overrides.get(i);
            boolean recorded = override != null;
            double amount = recorded ? override.getEmiAmount() : loan.getEmiAmount();
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
            row.setEmiStatus(resolveEmiStatus(deductionDate));
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
