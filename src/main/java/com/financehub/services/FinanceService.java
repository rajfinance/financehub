package com.financehub.services;

import com.financehub.dtos.*;
import com.financehub.entities.*;
import com.financehub.repositories.*;
import com.financehub.utils.FormatterUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class FinanceService {

	private static final int INSURANCE_ALERT_DAYS = 30;
	private static final int CARD_DUE_SOON_DAYS = 7;
	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);

	private final FinanceAccountRepository accountRepository;
	private final FinanceLedgerEntryRepository ledgerRepository;
	private final FinanceCreditCardRepository creditCardRepository;
	private final FinanceInsurancePolicyRepository insuranceRepository;
	private final SalaryRepository salaryRepository;
	private final ExpensesRepository expensesRepository;
	private final RentPaymentRepository rentPaymentRepository;
	private final UserService userService;
	private final FormatterUtils formatterUtils;
	private final LoanService loanService;

	public FinanceService(FinanceAccountRepository accountRepository,
			FinanceLedgerEntryRepository ledgerRepository,
			FinanceCreditCardRepository creditCardRepository,
			FinanceInsurancePolicyRepository insuranceRepository,
			SalaryRepository salaryRepository,
			ExpensesRepository expensesRepository,
			RentPaymentRepository rentPaymentRepository,
			UserService userService,
			FormatterUtils formatterUtils,
			LoanService loanService) {
		this.accountRepository = accountRepository;
		this.ledgerRepository = ledgerRepository;
		this.creditCardRepository = creditCardRepository;
		this.insuranceRepository = insuranceRepository;
		this.salaryRepository = salaryRepository;
		this.expensesRepository = expensesRepository;
		this.rentPaymentRepository = rentPaymentRepository;
		this.userService = userService;
		this.formatterUtils = formatterUtils;
		this.loanService = loanService;
	}

	private long uid() {
		return userService.getUserId();
	}

	public List<FinanceAccountDTO> listAccounts() {
		return accountRepository.findByUserIdOrderByNameAsc(uid()).stream()
				.map(this::toAccountDto)
				.collect(Collectors.toList());
	}

	public FinanceAccountDTO getAccountDto(Long id) {
		return toAccountDto(requireAccount(id));
	}

	@Transactional
	public void saveAccount(FinanceAccountDTO dto) {
		if (dto.getName() == null || dto.getName().isBlank()) {
			throw new IllegalArgumentException("Account name is required.");
		}
		if (dto.getAccountType() == null || dto.getAccountType().isBlank()) {
			throw new IllegalArgumentException("Account type is required.");
		}
		LocalDateTime now = LocalDateTime.now();
		FinanceAccount account;
		if (dto.getId() != null) {
			account = requireAccount(dto.getId());
		} else {
			account = new FinanceAccount();
			account.setUserId(uid());
			account.setCreatedAt(now);
			account.setCurrentBalance(0.0);
		}
		account.setName(dto.getName().trim());
		account.setAccountType(dto.getAccountType().trim().toUpperCase(Locale.ROOT));
		account.setBankName(blankToNull(dto.getBankName()));
		account.setAccountMask(blankToNull(dto.getAccountMask()));
		if (dto.getCurrentBalance() != null) {
			account.setCurrentBalance(dto.getCurrentBalance());
		} else if (account.getCurrentBalance() == null) {
			account.setCurrentBalance(0.0);
		}
		account.setNotes(blankToNull(dto.getNotes()));
		account.setUpdatedAt(now);
		accountRepository.save(account);
	}

	@Transactional
	public void deleteAccount(Long id) {
		FinanceAccount account = requireAccount(id);
		ledgerRepository.deleteAll(ledgerRepository.findByUserIdAndAccountIdOrderByEntryDateDescIdDesc(uid(), id));
		accountRepository.delete(account);
	}

	private FinanceAccount requireAccount(Long id) {
		return accountRepository.findByIdAndUserId(id, uid())
				.orElseThrow(() -> new IllegalArgumentException("Account not found."));
	}

	private FinanceAccountDTO toAccountDto(FinanceAccount a) {
		FinanceAccountDTO dto = new FinanceAccountDTO();
		dto.setId(a.getId());
		dto.setName(a.getName());
		dto.setAccountType(a.getAccountType());
		dto.setBankName(a.getBankName());
		dto.setAccountMask(a.getAccountMask());
		dto.setCurrentBalance(a.getCurrentBalance());
		dto.setFormattedBalance(formatterUtils.formatInIndianStyle(nz(a.getCurrentBalance())));
		dto.setNotes(a.getNotes());
		return dto;
	}

	public List<FinanceLedgerEntryDTO> listLedger(Long accountId) {
		List<FinanceLedgerEntry> entries = accountId != null
				? ledgerRepository.findByUserIdAndAccountIdOrderByEntryDateDescIdDesc(uid(), accountId)
				: ledgerRepository.findByUserIdOrderByEntryDateDescIdDesc(uid());
		return entries.stream().map(this::toLedgerDto).collect(Collectors.toList());
	}

	@Transactional
	public void saveLedgerEntry(FinanceLedgerEntryDTO dto) {
		if (dto.getAccountId() == null) {
			throw new IllegalArgumentException("Select an account.");
		}
		if (dto.getAmount() == null || dto.getAmount() <= 0) {
			throw new IllegalArgumentException("Amount must be greater than zero.");
		}
		String type = dto.getEntryType() == null ? "" : dto.getEntryType().trim().toUpperCase(Locale.ROOT);
		if (!"CREDIT".equals(type) && !"DEBIT".equals(type)) {
			throw new IllegalArgumentException("Entry type must be Credit or Debit.");
		}
		FinanceAccount account = requireAccount(dto.getAccountId());
		LocalDate date = dto.getEntryDate() != null ? dto.getEntryDate() : LocalDate.now();

		FinanceLedgerEntry entry = new FinanceLedgerEntry();
		entry.setUserId(uid());
		entry.setAccountId(account.getId());
		entry.setEntryDate(date);
		entry.setEntryType(type);
		entry.setAmount(dto.getAmount());
		entry.setDescription(blankToNull(dto.getDescription()));
		entry.setCreatedAt(LocalDateTime.now());
		ledgerRepository.save(entry);

		double bal = nz(account.getCurrentBalance());
		bal = "CREDIT".equals(type) ? bal + dto.getAmount() : bal - dto.getAmount();
		account.setCurrentBalance(bal);
		account.setUpdatedAt(LocalDateTime.now());
		accountRepository.save(account);
	}

	@Transactional
	public void deleteLedgerEntry(Long id) {
		FinanceLedgerEntry entry = ledgerRepository.findByIdAndUserId(id, uid())
				.orElseThrow(() -> new IllegalArgumentException("Ledger entry not found."));
		FinanceAccount account = requireAccount(entry.getAccountId());
		double bal = nz(account.getCurrentBalance());
		if ("CREDIT".equalsIgnoreCase(entry.getEntryType())) {
			bal -= nz(entry.getAmount());
		} else {
			bal += nz(entry.getAmount());
		}
		account.setCurrentBalance(bal);
		account.setUpdatedAt(LocalDateTime.now());
		accountRepository.save(account);
		ledgerRepository.delete(entry);
	}

	private FinanceLedgerEntryDTO toLedgerDto(FinanceLedgerEntry e) {
		FinanceLedgerEntryDTO dto = new FinanceLedgerEntryDTO();
		dto.setId(e.getId());
		dto.setAccountId(e.getAccountId());
		accountRepository.findByIdAndUserId(e.getAccountId(), uid())
				.ifPresent(a -> dto.setAccountName(a.getName()));
		dto.setEntryDate(e.getEntryDate());
		dto.setEntryType(e.getEntryType());
		dto.setAmount(e.getAmount());
		dto.setFormattedAmount(formatterUtils.formatInIndianStyle(nz(e.getAmount())));
		dto.setDescription(e.getDescription());
		return dto;
	}

	public List<FinanceCreditCardDTO> listCreditCards() {
		LocalDate today = LocalDate.now();
		return creditCardRepository.findByUserIdOrderByCardNameAsc(uid()).stream()
				.map(c -> toCardDto(c, today))
				.collect(Collectors.toList());
	}

	public FinanceCreditCardDTO getCreditCardDto(Long id) {
		return toCardDto(requireCard(id), LocalDate.now());
	}

	@Transactional
	public void saveCreditCard(FinanceCreditCardDTO dto) {
		if (dto.getCardName() == null || dto.getCardName().isBlank()) {
			throw new IllegalArgumentException("Card name is required.");
		}
		LocalDateTime now = LocalDateTime.now();
		FinanceCreditCard card;
		if (dto.getId() != null) {
			card = requireCard(dto.getId());
		} else {
			card = new FinanceCreditCard();
			card.setUserId(uid());
			card.setCreatedAt(now);
		}
		card.setCardName(dto.getCardName().trim());
		card.setBankName(blankToNull(dto.getBankName()));
		card.setCreditLimit(dto.getCreditLimit());
		card.setOutstandingBalance(dto.getOutstandingBalance() != null ? dto.getOutstandingBalance() : 0.0);
		card.setBillingDay(sanitizeDay(dto.getBillingDay()));
		card.setDueDay(sanitizeDay(dto.getDueDay()));
		card.setNotes(blankToNull(dto.getNotes()));
		card.setUpdatedAt(now);
		creditCardRepository.save(card);
	}

	@Transactional
	public void deleteCreditCard(Long id) {
		creditCardRepository.delete(requireCard(id));
	}

	private FinanceCreditCard requireCard(Long id) {
		return creditCardRepository.findByIdAndUserId(id, uid())
				.orElseThrow(() -> new IllegalArgumentException("Credit card not found."));
	}

	private FinanceCreditCardDTO toCardDto(FinanceCreditCard c, LocalDate today) {
		FinanceCreditCardDTO dto = new FinanceCreditCardDTO();
		dto.setId(c.getId());
		dto.setCardName(c.getCardName());
		dto.setBankName(c.getBankName());
		dto.setCreditLimit(c.getCreditLimit());
		dto.setFormattedCreditLimit(c.getCreditLimit() == null ? "—" : formatterUtils.formatInIndianStyle(c.getCreditLimit()));
		dto.setOutstandingBalance(c.getOutstandingBalance());
		dto.setFormattedOutstanding(formatterUtils.formatInIndianStyle(nz(c.getOutstandingBalance())));
		dto.setBillingDay(c.getBillingDay());
		dto.setDueDay(c.getDueDay());
		dto.setNotes(c.getNotes());
		dto.setDueSoon(isCardDueSoon(c.getDueDay(), today));
		return dto;
	}

	private boolean isCardDueSoon(Integer dueDay, LocalDate today) {
		if (dueDay == null || dueDay < 1 || dueDay > 31) {
			return false;
		}
		int due = Math.min(dueDay, today.lengthOfMonth());
		LocalDate dueDate = today.withDayOfMonth(due);
		if (dueDate.isBefore(today)) {
			dueDate = dueDate.plusMonths(1);
			dueDate = dueDate.withDayOfMonth(Math.min(dueDay, dueDate.lengthOfMonth()));
		}
		long days = ChronoUnit.DAYS.between(today, dueDate);
		return days >= 0 && days <= CARD_DUE_SOON_DAYS;
	}

	public List<FinanceInsuranceDTO> listInsurance() {
		LocalDate today = LocalDate.now();
		return insuranceRepository.findByUserIdOrderByNextDueDateAsc(uid()).stream()
				.map(p -> toInsuranceDto(p, today))
				.collect(Collectors.toList());
	}

	public FinanceInsuranceDTO getInsuranceDto(Long id) {
		return toInsuranceDto(requireInsurance(id), LocalDate.now());
	}

	@Transactional
	public void saveInsurance(FinanceInsuranceDTO dto) {
		if (dto.getPolicyName() == null || dto.getPolicyName().isBlank()) {
			throw new IllegalArgumentException("Policy name is required.");
		}
		if (dto.getPremiumAmount() == null || dto.getPremiumAmount() <= 0) {
			throw new IllegalArgumentException("Premium amount must be greater than zero.");
		}
		if (dto.getNextDueDate() == null) {
			throw new IllegalArgumentException("Next due date is required.");
		}
		LocalDateTime now = LocalDateTime.now();
		FinanceInsurancePolicy policy;
		if (dto.getId() != null) {
			policy = requireInsurance(dto.getId());
		} else {
			policy = new FinanceInsurancePolicy();
			policy.setUserId(uid());
			policy.setCreatedAt(now);
		}
		policy.setPolicyName(dto.getPolicyName().trim());
		policy.setInsurerName(blankToNull(dto.getInsurerName()));
		policy.setPolicyType(dto.getPolicyType() == null || dto.getPolicyType().isBlank()
				? "OTHER" : dto.getPolicyType().trim().toUpperCase(Locale.ROOT));
		policy.setPremiumAmount(dto.getPremiumAmount());
		policy.setPremiumFrequency(dto.getPremiumFrequency() == null || dto.getPremiumFrequency().isBlank()
				? "YEARLY" : dto.getPremiumFrequency().trim().toUpperCase(Locale.ROOT));
		policy.setNextDueDate(dto.getNextDueDate());
		policy.setCoverAmount(dto.getCoverAmount());
		policy.setNotes(blankToNull(dto.getNotes()));
		policy.setUpdatedAt(now);
		insuranceRepository.save(policy);
	}

	@Transactional
	public void markInsurancePaid(Long id) {
		FinanceInsurancePolicy policy = requireInsurance(id);
		policy.setNextDueDate(advanceDueDate(policy.getNextDueDate(), policy.getPremiumFrequency()));
		policy.setUpdatedAt(LocalDateTime.now());
		insuranceRepository.save(policy);
	}

	@Transactional
	public void deleteInsurance(Long id) {
		insuranceRepository.delete(requireInsurance(id));
	}

	private FinanceInsurancePolicy requireInsurance(Long id) {
		return insuranceRepository.findByIdAndUserId(id, uid())
				.orElseThrow(() -> new IllegalArgumentException("Insurance policy not found."));
	}

	private FinanceInsuranceDTO toInsuranceDto(FinanceInsurancePolicy p, LocalDate today) {
		FinanceInsuranceDTO dto = new FinanceInsuranceDTO();
		dto.setId(p.getId());
		dto.setPolicyName(p.getPolicyName());
		dto.setInsurerName(p.getInsurerName());
		dto.setPolicyType(p.getPolicyType());
		dto.setPremiumAmount(p.getPremiumAmount());
		dto.setFormattedPremium(formatterUtils.formatInIndianStyle(nz(p.getPremiumAmount())));
		dto.setPremiumFrequency(p.getPremiumFrequency());
		dto.setNextDueDate(p.getNextDueDate());
		dto.setFormattedDueDate(p.getNextDueDate() != null ? p.getNextDueDate().format(DATE_FMT) : "—");
		dto.setCoverAmount(p.getCoverAmount());
		dto.setFormattedCover(p.getCoverAmount() == null ? "—" : formatterUtils.formatInIndianStyle(p.getCoverAmount()));
		dto.setNotes(p.getNotes());
		boolean overdue = p.getNextDueDate() != null && p.getNextDueDate().isBefore(today);
		boolean dueSoon = !overdue && p.getNextDueDate() != null
				&& !p.getNextDueDate().isAfter(today.plusDays(INSURANCE_ALERT_DAYS));
		dto.setOverdue(overdue);
		dto.setDueSoon(dueSoon);
		if (overdue) {
			dto.setAlertLabel("Overdue");
		} else if (dueSoon) {
			dto.setAlertLabel("Due soon");
		} else {
			dto.setAlertLabel("");
		}
		return dto;
	}

	private LocalDate advanceDueDate(LocalDate from, String frequency) {
		if (from == null) {
			return LocalDate.now();
		}
		String freq = frequency == null ? "YEARLY" : frequency.toUpperCase(Locale.ROOT);
		if ("MONTHLY".equals(freq)) {
			return from.plusMonths(1);
		}
		if ("QUARTERLY".equals(freq)) {
			return from.plusMonths(3);
		}
		if ("HALF_YEARLY".equals(freq)) {
			return from.plusMonths(6);
		}
		return from.plusYears(1);
	}

	public List<FinanceAlertDTO> getDashboardAlerts() {
		List<FinanceAlertDTO> alerts = new ArrayList<>();
		LocalDate today = LocalDate.now();
		LocalDate horizon = today.plusDays(INSURANCE_ALERT_DAYS);
		for (FinanceInsurancePolicy p : insuranceRepository
				.findByUserIdAndNextDueDateLessThanEqualOrderByNextDueDateAsc(uid(), horizon)) {
			boolean overdue = p.getNextDueDate().isBefore(today);
			alerts.add(new FinanceAlertDTO(
					"INSURANCE",
					p.getPolicyName(),
					(overdue ? "Premium overdue since " : "Premium due on ")
							+ p.getNextDueDate().format(DATE_FMT)
							+ " · ₹" + formatterUtils.formatInIndianStyle(nz(p.getPremiumAmount())),
					overdue ? "danger" : "warn"));
		}
		for (FinanceCreditCard c : creditCardRepository.findByUserIdOrderByCardNameAsc(uid())) {
			if (isCardDueSoon(c.getDueDay(), today)) {
				alerts.add(new FinanceAlertDTO(
						"CREDIT_CARD",
						c.getCardName(),
						"Payment due around day " + c.getDueDay()
								+ " · Outstanding ₹" + formatterUtils.formatInIndianStyle(nz(c.getOutstandingBalance())),
						"warn"));
			}
		}
		return alerts;
	}

	public FinanceCashFlowReportDTO buildCashFlow(int year) {
		YearTotals totals = loadYearTotals(year);
		FinanceCashFlowReportDTO report = new FinanceCashFlowReportDTO();
		report.setYear(year);
		List<FinanceCashFlowLineDTO> lines = new ArrayList<>();
		addLine(lines, "Salary (Professional)", totals.salary, true);
		addLine(lines, "Rental income", totals.rent, true);
		addLine(lines, "Ledger credits (manual)", totals.ledgerCredit, true);
		addLine(lines, "Expenses", totals.expense, false);
		addLine(lines, "Loans paid (EMI / settlements)", totals.loansPaid, false);
		addLine(lines, "Ledger debits (manual)", totals.ledgerDebit, false);
		report.setLines(lines);
		report.setInflow(totals.inflow());
		report.setOutflow(totals.outflow());
		report.setNet(totals.inflow() - totals.outflow());
		report.setFormattedInflow(formatterUtils.formatInIndianStyle(report.getInflow()));
		report.setFormattedOutflow(formatterUtils.formatInIndianStyle(report.getOutflow()));
		report.setFormattedNet(formatterUtils.formatInIndianStyle(report.getNet()));
		return report;
	}

	public FinanceYearEndPackDTO buildYearEndPack(int year) {
		YearTotals totals = loadYearTotals(year);
		FinanceYearEndPackDTO pack = new FinanceYearEndPackDTO();
		pack.setYear(year);
		List<FinanceYearEndSectionDTO> sections = new ArrayList<>();
		sections.add(section("Salary", totals.salary, true));
		sections.add(section("Rental income", totals.rent, true));
		sections.add(section("Ledger credits", totals.ledgerCredit, true));
		sections.add(section("Expenses", totals.expense, false));
		sections.add(section("Loans paid", totals.loansPaid, false));
		sections.add(section("Ledger debits", totals.ledgerDebit, false));
		sections.add(section("Insurance (annualised estimate)", totals.insuranceEstimate, false));
		pack.setSections(sections);
		double net = totals.inflow() - totals.outflow();
		pack.setNet(net);
		pack.setFormattedNet(formatterUtils.formatInIndianStyle(net));
		return pack;
	}

	private YearTotals loadYearTotals(int year) {
		long userId = uid();
		YearTotals t = new YearTotals();
		t.salary = salaryRepository.sumAmountByUserIdAndYear(userId, year);
		t.expense = sumActualExpensesForYear(userId, year);
		t.rent = rentPaymentRepository.sumAmountByUserIdAndYear(userId, year);
		t.loansPaid = loanService.getYearlyPaidLoansReportRows().stream()
				.filter(r -> String.valueOf(year).equals(r.getYear()))
				.mapToDouble(YearlyAmountRowDTO::getAmount)
				.findFirst()
				.orElse(0);
		t.ledgerCredit = ledgerRepository.sumByUserIdAndTypeAndYear(userId, "CREDIT", year);
		t.ledgerDebit = ledgerRepository.sumByUserIdAndTypeAndYear(userId, "DEBIT", year);
		t.insuranceEstimate = estimateInsuranceAnnual();
		return t;
	}

	private double sumActualExpensesForYear(long userId, int year) {
		List<Expenses> rows = expensesRepository.findByUserIdAndExpenseYearOrderByExpenseMonth(userId, year);
		double total = 0;
		for (Expenses expense : rows) {
			if (expense.getActualExpenses() == null) {
				continue;
			}
			total += expense.getActualExpenses().values().stream().mapToDouble(Double::doubleValue).sum();
		}
		return total;
	}

	private double estimateInsuranceAnnual() {
		double total = 0;
		for (FinanceInsurancePolicy p : insuranceRepository.findByUserIdOrderByNextDueDateAsc(uid())) {
			double premium = nz(p.getPremiumAmount());
			String freq = p.getPremiumFrequency() == null ? "YEARLY" : p.getPremiumFrequency().toUpperCase(Locale.ROOT);
			int times = 1;
			if ("MONTHLY".equals(freq)) {
				times = 12;
			} else if ("QUARTERLY".equals(freq)) {
				times = 4;
			} else if ("HALF_YEARLY".equals(freq)) {
				times = 2;
			}
			total += premium * times;
		}
		return total;
	}

	private void addLine(List<FinanceCashFlowLineDTO> lines, String label, double amount, boolean inflow) {
		lines.add(new FinanceCashFlowLineDTO(label, formatterUtils.formatInIndianStyle(amount), amount, inflow));
	}

	private FinanceYearEndSectionDTO section(String title, double total, boolean inflow) {
		return new FinanceYearEndSectionDTO(title, formatterUtils.formatInIndianStyle(total), total, inflow);
	}

	private static class YearTotals {
		double salary;
		double expense;
		double rent;
		double loansPaid;
		double ledgerCredit;
		double ledgerDebit;
		double insuranceEstimate;

		double inflow() {
			return salary + rent + ledgerCredit;
		}

		double outflow() {
			return expense + loansPaid + ledgerDebit;
		}
	}

	private static double nz(Double v) {
		return v == null ? 0.0 : v;
	}

	private static String blankToNull(String s) {
		return s == null || s.isBlank() ? null : s.trim();
	}

	private static Integer sanitizeDay(Integer day) {
		if (day == null) {
			return null;
		}
		if (day < 1 || day > 31) {
			throw new IllegalArgumentException("Day of month must be between 1 and 31.");
		}
		return day;
	}

	public List<Integer> reportYears() {
		int current = Year.now().getValue();
		List<Integer> years = new ArrayList<>();
		for (int y = current; y >= current - 10; y--) {
			years.add(y);
		}
		return years;
	}
}
