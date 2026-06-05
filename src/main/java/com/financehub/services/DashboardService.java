package com.financehub.services;

import com.financehub.dtos.DashboardChartDataDTO;
import com.financehub.entities.Expenses;
import com.financehub.repositories.ExpensesRepository;
import com.financehub.repositories.RentPaymentRepository;
import com.financehub.repositories.SalaryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Year;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class DashboardService {

    private final UserService userService;
    private final WorkService workService;
    private final ExpensesService expensesService;
    private final RentalService rentalService;
    private final LoanService loanService;
    private final SalaryRepository salaryRepository;
    private final ExpensesRepository expensesRepository;
    private final RentPaymentRepository rentPaymentRepository;

    public DashboardService(UserService userService,
                            WorkService workService,
                            ExpensesService expensesService,
                            RentalService rentalService,
                            LoanService loanService,
                            SalaryRepository salaryRepository,
                            ExpensesRepository expensesRepository,
                            RentPaymentRepository rentPaymentRepository) {
        this.userService = userService;
        this.workService = workService;
        this.expensesService = expensesService;
        this.rentalService = rentalService;
        this.loanService = loanService;
        this.salaryRepository = salaryRepository;
        this.expensesRepository = expensesRepository;
        this.rentPaymentRepository = rentPaymentRepository;
    }

    public int getKpiYear() {
        return Year.now().getValue();
    }

    public int getChartYear() {
        int chartYear = Year.now().getValue();
        if (LocalDate.now().getMonthValue() == 1) {
            chartYear -= 1;
        }
        return chartYear;
    }

    /** Fast path for first paint after login — no full-table scans or loan schedule build. */
    public Map<String, Integer> getKpiSummary() {
        long userId = userService.getUserId();
        int kpiYear = getKpiYear();
        int salary = (int) Math.round(salaryRepository.sumAmountByUserIdAndYear(userId, kpiYear));
        int expense = sumActualExpensesForYear(userId, kpiYear);
        int rent = (int) Math.round(rentPaymentRepository.sumAmountByUserIdAndYear(userId, kpiYear));
        int netBalance = salary - expense;
        return Map.of(
                "currentYearSalary", salary,
                "currentYearExpense", expense,
                "currentYearRent", rent,
                "currentYearNetBalance", netBalance
        );
    }

    /** Loaded asynchronously after dashboard shell is shown. */
    public DashboardChartDataDTO getChartData() {
        int chartYear = getChartYear();
        DashboardChartDataDTO data = new DashboardChartDataDTO();

        Map<String, Integer> monthlySal = workService.getMonthlySalaryData(chartYear);
        data.setMonthlySalaryData(monthlySal);
        data.setSalaryData(monthlySal);

        Map<String, Integer> yearlySal = workService.getYearlySalaryData();
        Map<String, Integer> yearlyExp = expensesService.getYearlyExpenseData();
        Set<String> allYears = new HashSet<>();
        allYears.addAll(yearlySal.keySet());
        allYears.addAll(yearlyExp.keySet());
        for (String year : allYears) {
            yearlySal.putIfAbsent(year, 0);
            yearlyExp.putIfAbsent(year, 0);
        }
        data.setYearlySalaryData(yearlySal);
        data.setYearlyExpenseData(yearlyExp);
        data.setYearlyRentData(rentalService.getYearlyRentData());
        data.setExpenseData(expensesService.getMonthlyExpenseData(chartYear));
        data.setPendingEmi(loanService.getCurrentYearPendingEmiAmount());
        return data;
    }

    private int sumActualExpensesForYear(long userId, int year) {
        List<Expenses> rows = expensesRepository.findByUserIdAndExpenseYearOrderByExpenseMonth(userId, year);
        double total = 0;
        for (Expenses expense : rows) {
            if (expense.getActualExpenses() == null) {
                continue;
            }
            total += expense.getActualExpenses().values().stream().mapToDouble(Double::doubleValue).sum();
        }
        return (int) Math.round(total);
    }
}
