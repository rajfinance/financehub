package com.financehub.services;

import com.financehub.dtos.ExpenseDetail;
import com.financehub.dtos.ExpenseReportDTO;
import com.financehub.dtos.ExpenseRequest;
import com.financehub.dtos.ExpensesCategoriesDTO;
import com.financehub.entities.ExpenseCategories;
import com.financehub.entities.Expenses;
import com.financehub.repositories.ExpensesCategoriesRepository;
import com.financehub.repositories.ExpensesRepository;
import com.financehub.utils.FormatterUtils;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExpensesService {
    @Autowired
    UserService userService;
    @Autowired
    private FormatterUtils formatterUtils;
    @Autowired
    public ExpensesCategoriesRepository expensesCategoriesRepository;
    @Autowired
    public ExpensesRepository expensesRepository;
    public List<ExpenseCategories> getAllCategories(Long userId) {
        return expensesCategoriesRepository.findByUserIdOrderBySortOrder(userId);
    }
    public List<ExpenseCategories> getEnabledCategories(Long userId){
        return expensesCategoriesRepository.findByUserIdOrderBySortOrder(userId)
                .stream()
                .filter(ExpenseCategories::isEnabled)
                .collect(Collectors.toList());
    }

    public void saveCategory(ExpensesCategoriesDTO expensesCategoriesDTO) {
        ExpenseCategories entity = mapDtoToEntity(expensesCategoriesDTO);
        expensesCategoriesRepository.save(entity);
    }

    private ExpenseCategories mapDtoToEntity(ExpensesCategoriesDTO dto) {
        ExpenseCategories entity = new ExpenseCategories();

        if(dto.getCategoryId() == null || dto.getCategoryId() == 0){
            entity.setName(dto.getCategoryName());
            entity.setIcon(dto.getIconPath());
            entity.setSortOrder(dto.getSortOrder());
            entity.setUserId(userService.getUserId());
            entity.setEnabled(dto.isEnabled());
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
        }
        else{
            Optional<ExpenseCategories> existingCategory = expensesCategoriesRepository.findById(Math.toIntExact(dto.getCategoryId()));

            if (existingCategory.isPresent()) {
               entity = existingCategory.get();
                entity.setName(dto.getCategoryName());
                entity.setIcon(dto.getIconPath());
                entity.setSortOrder(dto.getSortOrder());
                entity.setEnabled(dto.isEnabled());
                entity.setUpdatedAt(LocalDateTime.now());
            }
        }
        return entity;
    }

    public void deleteCategoryByID(int id) {
        expensesCategoriesRepository.deleteById(id);
    }

    public void saveExpense(ExpenseRequest expenseRequest) {
            YearMonth yearMonth = YearMonth.parse(expenseRequest.getMonth());
            int year = yearMonth.getYear();
            int month = yearMonth.getMonthValue();
            Long userId = userService.getUserId();

            boolean isPlanned = "plan".equalsIgnoreCase(expenseRequest.getExpenseType());
            String expenseTypeChar = "plan".equalsIgnoreCase(expenseRequest.getExpenseType()) ? "P" : "A";

            Optional<Expenses> existingExpenseOpt = expensesRepository.findByUserIdAndExpenseYearAndExpenseMonth(userId, year, month);

            Expenses expenseEntity;
            if (existingExpenseOpt.isPresent()) {
                expenseEntity = existingExpenseOpt.get();
                expenseEntity.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));

                if (isPlanned) {
                    expenseEntity.setPlannedExpenses(expenseRequest.getExpenses());
                } else {
                    expenseEntity.setActualExpenses(expenseRequest.getExpenses());
                }
            } else {
                expenseEntity = new Expenses();
                expenseEntity.setUserId(userId);
                expenseEntity.setExpenseYear(year);
                expenseEntity.setExpenseMonth(month);
                expenseEntity.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
                expenseEntity.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));

                if (isPlanned) {
                    expenseEntity.setPlannedExpenses(expenseRequest.getExpenses());
                    expenseEntity.setActualExpenses(null);
                } else {
                    expenseEntity.setActualExpenses(expenseRequest.getExpenses());
                    expenseEntity.setPlannedExpenses(null);
                }
            }

            expensesRepository.save(expenseEntity);
        }

    public Set<Integer> getDistinctExpenseYearsForUser(Long id) {
        return expensesRepository.findByUserId(id)
                .stream()
                .map(Expenses::getExpenseYear)
                .collect(Collectors.toSet());
    }

    public List<ExpenseReportDTO> getExpenseReport(int year) {
        List<Expenses> expenses = expensesRepository.findByExpenseYearAndUserId(year, userService.getUserId());
        return expenses.stream()
           .sorted(Comparator.comparing(Expenses::getExpenseMonth))
           .map(expense -> {
            double totalPlanAmount = 0.0;
            double totalActualAmount = 0.0;
            if (expense.getPlannedExpenses() != null) {
                for (Double value : expense.getPlannedExpenses().values()) {
                    totalPlanAmount += value;
                }
            }
            if (expense.getActualExpenses() != null) {
                for (Double value : expense.getActualExpenses().values()) {
                    totalActualAmount += value;
                }
            }
            return new ExpenseReportDTO(
                    expense.getId(),
                    expense.getExpenseYear(),
                    expense.getExpenseMonth(),
                    formatterUtils.getMonthName(expense.getExpenseMonth()),
                    expense.getPlannedExpenses(),
                    expense.getActualExpenses(),
                    totalPlanAmount,
                    totalActualAmount,""
            );
        }).collect(Collectors.toList());
    }

    public ExpenseReportDTO getExpenseDetailsById(Long id) {
        Optional<Expenses> optExpense = expensesRepository.findByIdAndUserId(id, userService.getUserId());
        if (optExpense.isPresent()) {
            Expenses expense = optExpense.get();
            return new ExpenseReportDTO(
                    expense.getId(),
                    expense.getExpenseYear(),
                    expense.getExpenseMonth(),"",
                    expense.getPlannedExpenses(),
                    expense.getActualExpenses(),
                    0,0,""
            );
        }
        return null;
    }

    public List<ExpenseReportDTO> getYearlyPlanActual(Long userId, int year) {
        List<Object[]> results = expensesRepository.getYearlyPlanActual(userId, year);
        Set<Integer> categoryIds = new HashSet<>();
        for (Object[] row : results) {
            Map<Integer, Double> plannedMap = (Map<Integer, Double>) row[1];
            Map<Integer, Double> actualMap = (Map<Integer, Double>) row[2];
            categoryIds.addAll(plannedMap.keySet());
            categoryIds.addAll(actualMap.keySet());
        }

        Map<Integer, String> categoryMap = expensesCategoriesRepository.findCategoryNamesByIds(new ArrayList<>(categoryIds))
                .stream().collect(Collectors.toMap( row -> (Integer) ((Object[]) row)[0], row -> (String) ((Object[]) row)[1]));

        Map<String, Map<Integer, double[]>> categoryMonthData = new LinkedHashMap<>();

        for (Object[] row : results) {
            Integer month = (Integer) row[0];
            Map<Integer, Double> plannedMap = (Map<Integer, Double>) row[1];
            Map<Integer, Double> actualMap = (Map<Integer, Double>) row[2];

            for (Map.Entry<Integer, Double> entry : plannedMap.entrySet()) {
                String category = categoryMap.get(entry.getKey());
                if (category == null) continue;

                double plannedValue = entry.getValue();
                categoryMonthData.putIfAbsent(category, new LinkedHashMap<>());
                categoryMonthData.get(category).putIfAbsent(month, new double[]{0.0, 0.0});
                categoryMonthData.get(category).get(month)[0] += plannedValue;
            }

            for (Map.Entry<Integer, Double> entry : actualMap.entrySet()) {
                String category = categoryMap.get(entry.getKey());
                if (category == null) continue;

                double actualValue = entry.getValue();
                categoryMonthData.putIfAbsent(category, new LinkedHashMap<>());
                categoryMonthData.get(category).putIfAbsent(month, new double[]{0.0, 0.0});
                categoryMonthData.get(category).get(month)[1] += actualValue;
            }
        }

        List<ExpenseReportDTO> reportList = new ArrayList<>();
        for (String category : categoryMonthData.keySet()) {
            for (int month = 1; month <= 12; month++) {
                double planAmount = categoryMonthData.get(category).getOrDefault(month, new double[]{0.0, 0.0})[0];
                double actualAmount = categoryMonthData.get(category).getOrDefault(month, new double[]{0.0, 0.0})[1];
                reportList.add(new ExpenseReportDTO(0,year, month,"",null,null, planAmount, actualAmount,category));
            }
        }

        return reportList;
    }
    @Transactional
    public void deleteExpense(Long expenseId, String type) {
        boolean hasPlan = expensesRepository.existsByIdAndPlannedExpensesNotNull(expenseId);
        boolean hasActual = expensesRepository.existsByIdAndActualExpensesNotNull(expenseId);

        if ("plan".equalsIgnoreCase(type)) {
            if (hasActual) {
                expensesRepository.clearPlanById(expenseId);
            } else {
                expensesRepository.deleteById(Math.toIntExact(expenseId));
            }
        } else if ("actual".equalsIgnoreCase(type)) {
            if (hasPlan) {
                expensesRepository.clearActualById(expenseId);
            } else {
                expensesRepository.deleteById(Math.toIntExact(expenseId));
            }
        }
    }

    public List<ExpenseReportDTO> getYearlyCategoryWiseExpenses(int year, Map<String, Double> categorySums, Map<Integer, Double> monthlySums, Map<String, Double> categoryAverages) {
        List<Expenses> expenses = expensesRepository.findByUserIdAndExpenseYear(userService.getUserId(),year);

        Set<Integer> categoryIds = expenses.stream()
                .flatMap(expense -> expense.getActualExpenses().keySet().stream())
                .collect(Collectors.toSet());

        List<Object[]> categoryData = (List<Object[]>) expensesCategoriesRepository.findCategoryNamesByIds(new ArrayList<>(categoryIds));

        LinkedHashMap<String, Integer> categorySortedMap = categoryData.stream()
                .sorted(Comparator.comparing(row -> (Integer) row[2]))
                .collect(Collectors.toMap(
                        row -> (String) row[1],
                        row -> (Integer) row[2],
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        Map<Integer, String> categoryMap = categoryData.stream()
                .collect(Collectors.toMap(
                        row -> (Integer) row[0],
                        row -> (String) row[1]
                ));

        Map<String, double[]> categoryMonthData = new LinkedHashMap<>();
        double[] totalMonthlySums = new double[12];

        for (Expenses expense : expenses) {
            int month = expense.getExpenseMonth();

            if (expense.getActualExpenses() != null) {
                for (Map.Entry<Integer, Double> entry : expense.getActualExpenses().entrySet()) {
                    Integer categoryId = entry.getKey();
                    Double actualAmount = entry.getValue();

                    String categoryName = categoryMap.get(categoryId);
                    if (categoryName == null) continue;

                    categoryMonthData.putIfAbsent(categoryName, new double[13]);

                    categoryMonthData.get(categoryName)[month - 1] += actualAmount;
                    categoryMonthData.get(categoryName)[12] += actualAmount;
                    totalMonthlySums[month - 1] += actualAmount;
                }
            }
        }

        categorySortedMap.forEach((categoryName, sortOrder) -> {
            double totalForCategory = categoryMonthData.getOrDefault(categoryName, new double[13])[12];
            categorySums.put(categoryName, totalForCategory);
            categoryAverages.put(categoryName, totalForCategory / 12);
        });

        for (int i = 0; i < 12; i++) {
            monthlySums.put(i + 1, totalMonthlySums[i]);
        }

        List<ExpenseReportDTO> reportList = new ArrayList<>();
        for (Map.Entry<String, double[]> entry : categoryMonthData.entrySet()) {
            String category = entry.getKey();
            double[] values = entry.getValue();

            for (int i = 0; i < 12; i++) {
                if (values[i] > 0) {
                    reportList.add(new ExpenseReportDTO(
                            0, year, i + 1, formatterUtils.getMonthName(i + 1),
                            null, null, 0.0, values[i], category
                    ));
                }
            }
        }

        reportList.sort(Comparator.comparing(dto -> categorySortedMap.getOrDefault(dto.getCategory(), Integer.MAX_VALUE)));

        Map<String, Double> sortedCategorySums = categorySums.entrySet()
                .stream()
                .sorted(Comparator.comparing(entry -> categorySortedMap.getOrDefault(entry.getKey(), Integer.MAX_VALUE)))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
        categorySums.clear();
        categorySums.putAll(sortedCategorySums);
        return reportList;
    }
}
