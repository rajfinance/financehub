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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FinanceService {

	private static final int INSURANCE_ALERT_DAYS = 30;
	private static final int CARD_DUE_SOON_DAYS = 7;
	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);
	private static final DateTimeFormatter DAY_MONTH_FMT = DateTimeFormatter.ofPattern("dd/MM", Locale.ENGLISH);

	private final FinanceAccountRepository accountRepository;
	private final FinanceCreditCardRepository creditCardRepository;
	private final FinanceCreditCardBillRepository cardBillRepository;
	private final FinanceInsurancePolicyRepository insuranceRepository;
	private final SalaryRepository salaryRepository;
	private final ExpensesRepository expensesRepository;
	private final RentPaymentRepository rentPaymentRepository;
	private final UserService userService;
	private final FormatterUtils formatterUtils;
	private final LoanService loanService;

	public FinanceService(FinanceAccountRepository accountRepository,
			FinanceCreditCardRepository creditCardRepository,
			FinanceCreditCardBillRepository cardBillRepository,
			FinanceInsurancePolicyRepository insuranceRepository,
			SalaryRepository salaryRepository,
			ExpensesRepository expensesRepository,
			RentPaymentRepository rentPaymentRepository,
			UserService userService,
			FormatterUtils formatterUtils,
			LoanService loanService) {
		this.accountRepository = accountRepository;
		this.creditCardRepository = creditCardRepository;
		this.cardBillRepository = cardBillRepository;
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
		account.setIfscCode(blankToNull(dto.getIfscCode()));
		account.setHomeBranch(blankToNull(dto.getHomeBranch()));
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
		dto.setIfscCode(a.getIfscCode());
		dto.setHomeBranch(a.getHomeBranch());
		dto.setCurrentBalance(a.getCurrentBalance());
		dto.setFormattedBalance(formatterUtils.formatInIndianStyle(nz(a.getCurrentBalance())));
		dto.setNotes(a.getNotes());
		return dto;
	}

	public List<FinanceCreditCardDTO> listCreditCards() {
		return creditCardRepository.findByUserIdOrderByCardNameAsc(uid()).stream()
				.map(this::toCardDto)
				.collect(Collectors.toList());
	}

	public FinanceCreditCardDTO getCreditCardDto(Long id) {
		return toCardDto(requireCard(id));
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
		card.setCardNumber(normalizeCardNumber(dto.getCardNumber()));
		card.setExpiryMonth(requireExpiryMonth(dto.getExpiryMonth()));
		card.setExpiryYear(requireExpiryYear(dto.getExpiryYear()));
		card.setCvv(normalizeCvv(dto.getCvv()));
		card.setCreditLimit(dto.getCreditLimit());
		if (dto.getId() == null) {
			card.setOutstandingBalance(0.0);
		}
		if (dto.getBillingDay() == null) {
			throw new IllegalArgumentException("Billing day is required.");
		}
		if (dto.getDueDay() == null) {
			throw new IllegalArgumentException("Due day is required.");
		}
		card.setBillingDay(sanitizeDay(dto.getBillingDay()));
		card.setDueDay(sanitizeDay(dto.getDueDay()));
		card.setNotes(blankToNull(dto.getNotes()));
		card.setUpdatedAt(now);
		creditCardRepository.save(card);
	}

	@Transactional
	public void deleteCreditCard(Long id) {
		FinanceCreditCard card = requireCard(id);
		cardBillRepository.deleteAll(
				cardBillRepository.findByUserIdAndCardIdOrderByBillYearDescBillMonthDescIdDesc(uid(), id));
		creditCardRepository.delete(card);
	}

	private FinanceCreditCard requireCard(Long id) {
		return creditCardRepository.findByIdAndUserId(id, uid())
				.orElseThrow(() -> new IllegalArgumentException("Credit card not found."));
	}

	private FinanceCreditCardDTO toCardDto(FinanceCreditCard c) {
		FinanceCreditCardDTO dto = new FinanceCreditCardDTO();
		dto.setId(c.getId());
		dto.setCardName(c.getCardName());
		dto.setBankName(c.getBankName());
		dto.setCardNumber(c.getCardNumber());
		dto.setExpiryMonth(c.getExpiryMonth());
		dto.setExpiryYear(c.getExpiryYear());
		dto.setFormattedExpiry(formatExpiry(c.getExpiryMonth(), c.getExpiryYear()));
		dto.setCvv(c.getCvv());
		dto.setLastFour(lastFour(c.getCardNumber()));
		dto.setDisplayLabel(cardDisplayLabel(c));
		dto.setCreditLimit(c.getCreditLimit());
		dto.setFormattedCreditLimit(c.getCreditLimit() == null ? "—" : formatterUtils.formatInIndianStyle(c.getCreditLimit()));
		dto.setOutstandingBalance(c.getOutstandingBalance());
		dto.setFormattedOutstanding(formatterUtils.formatInIndianStyle(nz(c.getOutstandingBalance())));
		dto.setBillingDay(c.getBillingDay());
		dto.setDueDay(c.getDueDay());
		dto.setNotes(c.getNotes());
		return dto;
	}

	public List<FinanceCreditCardBillDTO> listCardBills(Long cardId) {
		List<FinanceCreditCardBill> bills = cardId != null
				? cardBillRepository.findByUserIdAndCardIdOrderByBillYearDescBillMonthDescIdDesc(uid(), cardId)
				: cardBillRepository.findByUserIdOrderByBillYearDescBillMonthDescIdDesc(uid());
		Map<String, double[]> periodTotals = buildPeriodPaidTotals(bills);
		return bills.stream().map(b -> toCardBillDto(b, periodTotals)).collect(Collectors.toList());
	}

	public String cardBillPeriodSummaryJson(Long cardId, Integer billMonth, Integer billYear) {
		if (cardId == null || billMonth == null || billYear == null
				|| billMonth < 1 || billMonth > 12 || billYear < 2000) {
			return "{\"followUp\":false}";
		}
		requireCard(cardId);
		List<FinanceCreditCardBill> periodBills = cardBillRepository
				.findByUserIdAndCardIdAndBillMonthAndBillYearOrderByIdAsc(uid(), cardId, billMonth, billYear);
		if (periodBills.isEmpty()) {
			return "{\"followUp\":false}";
		}
		double billAmount = periodBills.stream().mapToDouble(b -> nz(b.getBillAmount())).max().orElse(0);
		double totalPaid = periodBills.stream().mapToDouble(b -> nz(b.getPaidAmount())).sum();
		double remaining = Math.max(0, billAmount - totalPaid);
		if (remaining <= 0.009) {
			return "{\"followUp\":false}";
		}
		Double interestAmount = periodBills.stream()
				.map(FinanceCreditCardBill::getInterestAmount)
				.filter(v -> v != null)
				.max(Double::compareTo)
				.orElse(null);
		StringBuilder json = new StringBuilder("{\"followUp\":true");
		json.append(",\"billAmount\":").append(billAmount);
		json.append(",\"interestAmount\":");
		if (interestAmount == null) {
			json.append("null");
		} else {
			json.append(interestAmount);
		}
		json.append(",\"totalPaid\":").append(totalPaid);
		json.append(",\"remaining\":").append(remaining);
		json.append('}');
		return json.toString();
	}

	public List<FinanceCreditCardBillYearGroupDTO> listCardBillsByYear(Long cardId) {
		Map<Integer, Map<Long, FinanceCreditCardBillCardGroupDTO>> yearCards = new LinkedHashMap<>();
		for (FinanceCreditCardBillDTO bill : listCardBills(cardId)) {
			Integer year = bill.getBillYear() != null ? bill.getBillYear() : 0;
			Long cid = bill.getCardId() != null ? bill.getCardId() : 0L;
			Map<Long, FinanceCreditCardBillCardGroupDTO> cards = yearCards.computeIfAbsent(year, y -> new LinkedHashMap<>());
			FinanceCreditCardBillCardGroupDTO cardGroup = cards.computeIfAbsent(cid, id -> {
				FinanceCreditCardBillCardGroupDTO g = new FinanceCreditCardBillCardGroupDTO();
				g.setCardId(id);
				g.setCardName(bill.getCardName() != null ? bill.getCardName() : "Credit card");
				return g;
			});
			cardGroup.getBills().add(bill);
		}

		List<FinanceCreditCardBillYearGroupDTO> years = new ArrayList<>();
		for (Map.Entry<Integer, Map<Long, FinanceCreditCardBillCardGroupDTO>> yearEntry : yearCards.entrySet()) {
			FinanceCreditCardBillYearGroupDTO yearGroup = new FinanceCreditCardBillYearGroupDTO();
			yearGroup.setYear(yearEntry.getKey());
			List<FinanceCreditCardBillCardGroupDTO> cardGroups = new ArrayList<>(yearEntry.getValue().values());
			cardGroups.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(
					a.getCardName() != null ? a.getCardName() : "",
					b.getCardName() != null ? b.getCardName() : ""));
			double yearBill = 0;
			double yearPaid = 0;
			double yearInterest = 0;
			for (FinanceCreditCardBillCardGroupDTO cardGroup : cardGroups) {
				cardGroup.getBills().sort(this::compareBillsForReport);
				Map<String, Double> monthBill = new LinkedHashMap<>();
				Map<String, Double> monthInterest = new LinkedHashMap<>();
				double cardPaid = 0;
				for (FinanceCreditCardBillDTO bill : cardGroup.getBills()) {
					String key = periodKey(bill.getCardId(), bill.getBillMonth(), bill.getBillYear());
					monthBill.merge(key, nz(bill.getBillAmount()), Math::max);
					monthInterest.merge(key, nz(bill.getInterestAmount()), Math::max);
					cardPaid += nz(bill.getPaidAmount());
				}
				double cardBill = monthBill.values().stream().mapToDouble(Double::doubleValue).sum();
				double cardInterest = monthInterest.values().stream().mapToDouble(Double::doubleValue).sum();
				cardGroup.setFormattedTotalBill(formatterUtils.formatInIndianStyle(cardBill));
				cardGroup.setFormattedTotalPaid(formatterUtils.formatInIndianStyle(cardPaid));
				cardGroup.setFormattedTotalInterest(formatterUtils.formatInIndianStyle(cardInterest));
				yearBill += cardBill;
				yearPaid += cardPaid;
				yearInterest += cardInterest;
			}
			yearGroup.setCardGroups(cardGroups);
			yearGroup.setFormattedTotalBill(formatterUtils.formatInIndianStyle(yearBill));
			yearGroup.setFormattedTotalPaid(formatterUtils.formatInIndianStyle(yearPaid));
			yearGroup.setFormattedTotalInterest(formatterUtils.formatInIndianStyle(yearInterest));
			years.add(yearGroup);
		}
		years.sort((a, b) -> Integer.compare(
				a.getYear() != null ? a.getYear() : 0,
				b.getYear() != null ? b.getYear() : 0));
		return years;
	}

	public FinanceCreditCardBillDTO getCardBillDto(Long id) {
		FinanceCreditCardBill bill = requireCardBill(id);
		List<FinanceCreditCardBill> periodBills = cardBillRepository
				.findByUserIdAndCardIdAndBillMonthAndBillYearOrderByIdAsc(
						uid(), bill.getCardId(), bill.getBillMonth(), bill.getBillYear());
		return toCardBillDto(bill, buildPeriodPaidTotals(periodBills));
	}

	public FinanceCreditCardBillDTO newCardBillDefaults() {
		LocalDate today = LocalDate.now();
		FinanceCreditCardBillDTO dto = new FinanceCreditCardBillDTO();
		dto.setBillMonth(today.getMonthValue());
		dto.setBillYear(today.getYear());
		return dto;
	}

	@Transactional
	public void saveCardBill(FinanceCreditCardBillDTO dto) {
		if (dto.getCardId() == null) {
			throw new IllegalArgumentException("Select a credit card.");
		}
		if (dto.getBillMonth() == null || dto.getBillMonth() < 1 || dto.getBillMonth() > 12) {
			throw new IllegalArgumentException("Select a valid bill month.");
		}
		if (dto.getBillYear() == null || dto.getBillYear() < 2000) {
			throw new IllegalArgumentException("Select a valid bill year.");
		}
		requireCard(dto.getCardId());

		List<FinanceCreditCardBill> existingPeriodBills = cardBillRepository
				.findByUserIdAndCardIdAndBillMonthAndBillYearOrderByIdAsc(
						uid(), dto.getCardId(), dto.getBillMonth(), dto.getBillYear());

		LocalDate billingDate = resolveBillingDate(dto.getCardId(), dto.getBillMonth(), dto.getBillYear());
		LocalDate dueDate = resolveDueDate(dto.getCardId(), dto.getBillMonth(), dto.getBillYear());

		LocalDateTime now = LocalDateTime.now();
		FinanceCreditCardBill bill;
		boolean additionalPayment = false;
		if (dto.getId() != null) {
			bill = requireCardBill(dto.getId());
		} else {
			bill = new FinanceCreditCardBill();
			bill.setUserId(uid());
			bill.setCreatedAt(now);
			additionalPayment = !existingPeriodBills.isEmpty();
		}

		double paid = nz(dto.getPaidAmount());
		if (paid < 0) {
			throw new IllegalArgumentException("Paid amount cannot be negative.");
		}
		if (dto.getInterestAmount() != null && dto.getInterestAmount() < 0) {
			throw new IllegalArgumentException("Interest amount cannot be negative.");
		}
		if (paid > 0 && dto.getPaidDate() == null) {
			throw new IllegalArgumentException("Paid date is required when paid amount is entered.");
		}

		double billAmount;
		Double interestAmount;
		if (additionalPayment) {
			if (paid <= 0 || dto.getPaidDate() == null) {
				throw new IllegalArgumentException("Paid amount and paid date are required for an additional payment.");
			}
			FinanceCreditCardBill statement = existingPeriodBills.stream()
					.filter(b -> b.getBillAmount() != null && b.getBillAmount() > 0)
					.findFirst()
					.orElse(existingPeriodBills.get(0));
			billAmount = nz(statement.getBillAmount());
			interestAmount = statement.getInterestAmount();
		} else if (dto.getId() != null && !isPrimaryPeriodBill(bill, existingPeriodBills)) {
			if (paid <= 0 || dto.getPaidDate() == null) {
				throw new IllegalArgumentException("Paid amount and paid date are required for a payment row.");
			}
			FinanceCreditCardBill statement = existingPeriodBills.stream()
					.filter(b -> b.getBillAmount() != null && b.getBillAmount() > 0)
					.findFirst()
					.orElse(bill);
			billAmount = nz(statement.getBillAmount());
			interestAmount = statement.getInterestAmount();
		} else {
			if (dto.getBillAmount() == null || dto.getBillAmount() < 0) {
				throw new IllegalArgumentException("Bill amount is required.");
			}
			billAmount = dto.getBillAmount();
			interestAmount = dto.getInterestAmount();
		}

		bill.setCardId(dto.getCardId());
		bill.setBillMonth(dto.getBillMonth());
		bill.setBillYear(dto.getBillYear());
		bill.setBillingDate(billingDate);
		bill.setDueDate(dueDate);
		bill.setInterestAmount(interestAmount);
		bill.setBillAmount(billAmount);
		bill.setPaidAmount(dto.getPaidAmount());
		bill.setPaidDate(dto.getPaidDate());

		double periodPaidOther = existingPeriodBills.stream()
				.filter(b -> dto.getId() == null || !b.getId().equals(dto.getId()))
				.mapToDouble(b -> nz(b.getPaidAmount()))
				.sum();
		double remainingAfter = Math.max(0, billAmount - periodPaidOther - paid);
		bill.setOutstandingAmount(remainingAfter);
		bill.setUpdatedAt(now);
		cardBillRepository.save(bill);

		recalculatePeriodOutstanding(dto.getCardId(), dto.getBillMonth(), dto.getBillYear());
		updateCardOutstandingFromLatestBill(dto.getCardId());
	}

	@Transactional
	public void deleteCardBill(Long id) {
		FinanceCreditCardBill bill = requireCardBill(id);
		Long cardId = bill.getCardId();
		Integer month = bill.getBillMonth();
		Integer year = bill.getBillYear();
		cardBillRepository.delete(bill);
		recalculatePeriodOutstanding(cardId, month, year);
		updateCardOutstandingFromLatestBill(cardId);
	}

	public LocalDate resolveBillingDate(Long cardId, int month, int year) {
		FinanceCreditCard card = requireCard(cardId);
		if (card.getBillingDay() == null) {
			throw new IllegalArgumentException("Set a billing day on the credit card before adding a bill.");
		}
		LocalDate base = LocalDate.of(year, month, 1);
		int day = Math.min(card.getBillingDay(), base.lengthOfMonth());
		return base.withDayOfMonth(day);
	}

	public LocalDate resolveDueDate(Long cardId, int month, int year) {
		FinanceCreditCard card = requireCard(cardId);
		if (card.getDueDay() == null) {
			throw new IllegalArgumentException("Set a due day on the credit card before adding a bill.");
		}
		LocalDate billingDate = resolveBillingDate(cardId, month, year);
		int dueDay = card.getDueDay();
		int billingDay = card.getBillingDay();
		LocalDate dueMonthStart = dueDay <= billingDay
				? billingDate.plusMonths(1).withDayOfMonth(1)
				: billingDate.withDayOfMonth(1);
		int day = Math.min(dueDay, dueMonthStart.lengthOfMonth());
		return dueMonthStart.withDayOfMonth(day);
	}

	private void updateCardOutstandingFromLatestBill(Long cardId) {
		List<FinanceCreditCardBill> bills = cardBillRepository
				.findByUserIdAndCardIdOrderByBillYearDescBillMonthDescIdDesc(uid(), cardId);
		FinanceCreditCard card = requireCard(cardId);
		if (bills.isEmpty()) {
			card.setOutstandingBalance(0.0);
			card.setUpdatedAt(LocalDateTime.now());
			creditCardRepository.save(card);
			return;
		}
		Integer latestMonth = bills.get(0).getBillMonth();
		Integer latestYear = bills.get(0).getBillYear();
		double statement = 0;
		double paid = 0;
		for (FinanceCreditCardBill bill : bills) {
			if (!latestMonth.equals(bill.getBillMonth()) || !latestYear.equals(bill.getBillYear())) {
				continue;
			}
			statement = Math.max(statement, nz(bill.getBillAmount()));
			paid += nz(bill.getPaidAmount());
		}
		card.setOutstandingBalance(Math.max(0, statement - paid));
		card.setUpdatedAt(LocalDateTime.now());
		creditCardRepository.save(card);
	}

	private void recalculatePeriodOutstanding(Long cardId, Integer month, Integer year) {
		List<FinanceCreditCardBill> periodBills = cardBillRepository
				.findByUserIdAndCardIdAndBillMonthAndBillYearOrderByIdAsc(uid(), cardId, month, year);
		if (periodBills.isEmpty()) {
			return;
		}
		double statement = periodBills.stream().mapToDouble(b -> nz(b.getBillAmount())).max().orElse(0);
		double paid = periodBills.stream().mapToDouble(b -> nz(b.getPaidAmount())).sum();
		double remaining = Math.max(0, statement - paid);
		LocalDateTime now = LocalDateTime.now();
		for (FinanceCreditCardBill bill : periodBills) {
			bill.setOutstandingAmount(remaining);
			bill.setUpdatedAt(now);
		}
		cardBillRepository.saveAll(periodBills);
	}

	private FinanceCreditCardBill requireCardBill(Long id) {
		return cardBillRepository.findByIdAndUserId(id, uid())
				.orElseThrow(() -> new IllegalArgumentException("Card bill not found."));
	}

	private FinanceCreditCardBillDTO toCardBillDto(FinanceCreditCardBill b, Map<String, double[]> periodTotals) {
		FinanceCreditCardBillDTO dto = new FinanceCreditCardBillDTO();
		dto.setId(b.getId());
		dto.setCardId(b.getCardId());
		creditCardRepository.findByIdAndUserId(b.getCardId(), uid())
				.ifPresent(c -> dto.setCardName(cardDisplayLabel(c)));
		dto.setBillMonth(b.getBillMonth());
		dto.setBillYear(b.getBillYear());
		dto.setFormattedPeriod(formatBillPeriod(b.getBillingDate()));
		dto.setBillingDate(b.getBillingDate());
		dto.setFormattedBillingDate(b.getBillingDate() != null ? b.getBillingDate().format(DAY_MONTH_FMT) : "—");
		dto.setDueDate(b.getDueDate());
		dto.setFormattedDueDate(b.getDueDate() != null ? b.getDueDate().format(DAY_MONTH_FMT) : "—");
		dto.setInterestAmount(b.getInterestAmount());
		dto.setFormattedInterestAmount(b.getInterestAmount() == null ? "—"
				: formatterUtils.formatInIndianStyle(b.getInterestAmount()));
		Double billAmount = b.getBillAmount() != null ? b.getBillAmount() : b.getOutstandingAmount();
		dto.setBillAmount(billAmount);
		dto.setFormattedBillAmount(billAmount == null ? "—" : formatterUtils.formatInIndianStyle(billAmount));
		dto.setOutstandingAmount(b.getOutstandingAmount());
		dto.setFormattedOutstanding(formatterUtils.formatInIndianStyle(nz(b.getOutstandingAmount())));
		dto.setPaidAmount(b.getPaidAmount());
		dto.setFormattedPaidAmount(b.getPaidAmount() == null || nz(b.getPaidAmount()) <= 0 ? "—"
				: formatterUtils.formatInIndianStyle(b.getPaidAmount()));
		dto.setPaidDate(b.getPaidDate());
		dto.setFormattedPaidDate(b.getPaidDate() != null ? b.getPaidDate().format(DAY_MONTH_FMT) : "—");
		double[] totals = periodTotals.getOrDefault(
				periodKey(b.getCardId(), b.getBillMonth(), b.getBillYear()),
				new double[] {nz(billAmount), nz(b.getPaidAmount())});
		dto.setPaymentStatus(resolvePaymentStatus(totals[0], totals[1]));
		return dto;
	}

	private Map<String, double[]> buildPeriodPaidTotals(List<FinanceCreditCardBill> bills) {
		Map<String, double[]> totals = new LinkedHashMap<>();
		for (FinanceCreditCardBill bill : bills) {
			String key = periodKey(bill.getCardId(), bill.getBillMonth(), bill.getBillYear());
			double[] row = totals.computeIfAbsent(key, k -> new double[] {0, 0});
			row[0] = Math.max(row[0], nz(bill.getBillAmount()));
			row[1] += nz(bill.getPaidAmount());
		}
		return totals;
	}

	private static String periodKey(Long cardId, Integer month, Integer year) {
		return (cardId != null ? cardId : 0) + "|" + (month != null ? month : 0) + "|" + (year != null ? year : 0);
	}

	private static boolean isPrimaryPeriodBill(FinanceCreditCardBill bill, List<FinanceCreditCardBill> periodBills) {
		if (periodBills.isEmpty()) {
			return true;
		}
		return periodBills.get(0).getId().equals(bill.getId());
	}

	private int compareBillsForReport(FinanceCreditCardBillDTO a, FinanceCreditCardBillDTO b) {
		int monthCmp = Integer.compare(
				b.getBillMonth() != null ? b.getBillMonth() : 0,
				a.getBillMonth() != null ? a.getBillMonth() : 0);
		if (monthCmp != 0) {
			return monthCmp;
		}
		LocalDate aPaid = a.getPaidDate();
		LocalDate bPaid = b.getPaidDate();
		if (aPaid == null && bPaid == null) {
			return Long.compare(a.getId() != null ? a.getId() : 0, b.getId() != null ? b.getId() : 0);
		}
		if (aPaid == null) {
			return 1;
		}
		if (bPaid == null) {
			return -1;
		}
		int paidCmp = aPaid.compareTo(bPaid);
		if (paidCmp != 0) {
			return paidCmp;
		}
		return Long.compare(a.getId() != null ? a.getId() : 0, b.getId() != null ? b.getId() : 0);
	}

	private static String formatBillPeriod(LocalDate billingDate) {
		if (billingDate == null) {
			return "—";
		}
		LocalDate end = billingDate.minusDays(1);
		LocalDate start = billingDate.getDayOfMonth() == 1
				? billingDate.minusMonths(1)
				: end.minusMonths(1);
		return start.format(DAY_MONTH_FMT) + "-" + end.format(DAY_MONTH_FMT);
	}

	private static String resolvePaymentStatus(Double billAmount, Double paidAmount) {
		double bill = nz(billAmount);
		double paid = nz(paidAmount);
		if (paid <= 0) {
			return "Not paid";
		}
		if (paid + 0.009 < bill) {
			return "Partially paid";
		}
		return "Paid";
	}

	private boolean isDueSoon(LocalDate dueDate, LocalDate today) {
		if (dueDate == null) {
			return false;
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
		for (FinanceCreditCardBill bill : cardBillRepository.findByUserIdOrderByBillYearDescBillMonthDescIdDesc(uid())) {
			if (isDueSoon(bill.getDueDate(), today)) {
				String cardName = creditCardRepository.findByIdAndUserId(bill.getCardId(), uid())
						.map(FinanceService::cardDisplayLabel)
						.orElse("Credit card");
				double billAmount = bill.getBillAmount() != null ? bill.getBillAmount() : nz(bill.getOutstandingAmount());
				alerts.add(new FinanceAlertDTO(
						"CREDIT_CARD",
						cardName,
						"Bill due on " + bill.getDueDate().format(DATE_FMT)
								+ " · Bill ₹" + formatterUtils.formatInIndianStyle(billAmount),
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
		addLine(lines, "Expenses", totals.expense, false);
		addLine(lines, "Loans paid (EMI / settlements)", totals.loansPaid, false);
		addLine(lines, "Credit card bills paid", totals.cardBillsPaid, false);
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
		sections.add(section("Expenses", totals.expense, false));
		sections.add(section("Loans paid", totals.loansPaid, false));
		sections.add(section("Credit card bills paid", totals.cardBillsPaid, false));
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
		t.cardBillsPaid = sumCardBillsPaidForYear(year);
		t.insuranceEstimate = estimateInsuranceAnnual();
		return t;
	}

	private double sumCardBillsPaidForYear(int year) {
		return cardBillRepository.findByUserIdOrderByBillYearDescBillMonthDescIdDesc(uid()).stream()
				.filter(b -> b.getBillYear() != null && b.getBillYear() == year)
				.mapToDouble(b -> nz(b.getPaidAmount()))
				.sum();
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
		double cardBillsPaid;
		double insuranceEstimate;

		double inflow() {
			return salary + rent;
		}

		double outflow() {
			return expense + loansPaid + cardBillsPaid;
		}
	}

	private static double nz(Double v) {
		return v == null ? 0.0 : v;
	}

	private static String blankToNull(String s) {
		return s == null || s.isBlank() ? null : s.trim();
	}

	private static String normalizeCardNumber(String raw) {
		if (raw == null || raw.isBlank()) {
			throw new IllegalArgumentException("Credit card number is required.");
		}
		String digits = raw.replaceAll("[\\s-]", "");
		if (!digits.matches("\\d{12,19}")) {
			throw new IllegalArgumentException("Credit card number must be 12 to 19 digits.");
		}
		return digits;
	}

	private static String normalizeCvv(String raw) {
		if (raw == null || raw.isBlank()) {
			throw new IllegalArgumentException("CVV is required.");
		}
		String cvv = raw.trim();
		if (!cvv.matches("\\d{3,4}")) {
			throw new IllegalArgumentException("CVV must be 3 or 4 digits.");
		}
		return cvv;
	}

	private static Integer requireExpiryMonth(Integer month) {
		if (month == null || month < 1 || month > 12) {
			throw new IllegalArgumentException("Select a valid expiry month.");
		}
		return month;
	}

	private static Integer requireExpiryYear(Integer year) {
		int current = Year.now().getValue();
		if (year == null || year < current - 20 || year > current + 30) {
			throw new IllegalArgumentException("Select a valid expiry year.");
		}
		return year;
	}

	private static String lastFour(String cardNumber) {
		if (cardNumber == null || cardNumber.length() < 4) {
			return cardNumber;
		}
		return cardNumber.substring(cardNumber.length() - 4);
	}

	private static String formatExpiry(Integer month, Integer year) {
		if (month == null || year == null) {
			return "—";
		}
		return String.format(Locale.ENGLISH, "%02d/%d", month, year);
	}

	private static String cardDisplayLabel(FinanceCreditCard card) {
		String bank = shortBankName(card.getBankName());
		String name = card.getCardName() != null ? card.getCardName().trim() : "";
		String last4 = lastFour(card.getCardNumber());
		StringBuilder label = new StringBuilder();
		if (bank != null && !bank.isBlank()) {
			label.append(bank);
		}
		if (!name.isBlank()) {
			if (label.length() > 0) {
				label.append('-');
			}
			label.append(name);
		}
		if (last4 != null && !last4.isBlank()) {
			if (label.length() > 0) {
				label.append('-');
			}
			label.append(last4);
		}
		return label.length() == 0 ? "Credit card" : label.toString();
	}

	private static String shortBankName(String bankName) {
		if (bankName == null || bankName.isBlank()) {
			return null;
		}
		String name = bankName.trim();
		return switch (name) {
			case "State Bank of India" -> "SBI";
			case "Union Bank of India" -> "Union";
			case "ICICI Bank" -> "ICICI";
			case "HDFC Bank" -> "HDFC";
			case "Axis Bank" -> "Axis";
			case "Kotak Mahindra Bank" -> "Kotak";
			case "Bank of Baroda" -> "BOB";
			case "Canara Bank" -> "Canara";
			case "Punjab National Bank" -> "PNB";
			case "Indian Bank" -> "Indian";
			case "Yes Bank" -> "Yes";
			case "IDFC First Bank" -> "IDFC";
			default -> name.replace(" Bank", "").replace(" bank", "").trim();
		};
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
