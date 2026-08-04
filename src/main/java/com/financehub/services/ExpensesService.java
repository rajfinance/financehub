package com.financehub.services;

import com.financehub.dtos.CategoryYearAmountRow;
import com.financehub.dtos.ExpenseReportDTO;
import com.financehub.dtos.ExpenseRequest;
import com.financehub.dtos.ExpensesCategoriesDTO;
import com.financehub.entities.ExpenseCategories;
import com.financehub.entities.Expenses;
import com.financehub.entities.Salary;
import com.financehub.repositories.ExpensesCategoriesRepository;
import com.financehub.repositories.ExpensesRepository;
import com.financehub.utils.FormatterUtils;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.VerticalAlignment;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.TimeUnit;
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
    @Autowired
    private ExpenseCategoryImageStorage expenseCategoryImageStorage;
    public List<ExpenseCategories> getAllCategories(Long userId) {
        return expensesCategoriesRepository.findByUserIdOrderBySortOrder(userId);
    }
    public List<ExpenseCategories> getEnabledCategories(Long userId){
        return expensesCategoriesRepository.findByUserIdOrderBySortOrder(userId)
                .stream()
                .filter(ExpenseCategories::isEnabled)
                .collect(Collectors.toList());
    }

    public ResponseEntity<byte[]> getCategoryIcon(int id) {
        Long userId = userService.getUserId();
        if (userId == null || userId <= 0) {
            return ResponseEntity.status(401).build();
        }
        return expensesCategoriesRepository.findByIdAndUserId(id, userId)
                .filter(c -> c.getIconData() != null && c.getIconData().length > 0)
                .map(c -> {
                    MediaType mediaType = MediaType.IMAGE_JPEG;
                    try {
                        if (c.getIconContentType() != null && !c.getIconContentType().isBlank()) {
                            mediaType = MediaType.parseMediaType(c.getIconContentType());
                        }
                    } catch (Exception ignored) {
                        mediaType = MediaType.IMAGE_JPEG;
                    }
                    return ResponseEntity.ok()
                            .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePrivate())
                            .header(HttpHeaders.CONTENT_LENGTH, Integer.toString(c.getIconData().length))
                            .contentType(mediaType)
                            .body(c.getIconData());
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Transactional
    public void saveCategory(ExpensesCategoriesDTO expensesCategoriesDTO) {
        ExpenseCategories entity = mapDtoToEntity(expensesCategoriesDTO);
        MultipartFile iconFile = expensesCategoriesDTO.getIconImage();
        if (iconFile != null && !iconFile.isEmpty()) {
            try {
                ExpenseCategoryImageStorage.StoredIcon stored = expenseCategoryImageStorage.readValidated(iconFile);
                if (stored != null) {
                    entity.setIconData(stored.data());
                    entity.setIconContentType(stored.contentType());
                }
            } catch (IOException e) {
                throw new IllegalStateException("Could not save category image", e);
            }
        }
        expensesCategoriesRepository.save(entity);
    }

    @Transactional
    public void reorderCategories(List<Integer> orderedIds) {
        if (orderedIds == null || orderedIds.isEmpty()) {
            return;
        }
        Long userId = userService.getUserId();
        for (int i = 0; i < orderedIds.size(); i++) {
            final int sortOrder = i;
            int id = orderedIds.get(i);
            expensesCategoriesRepository.findByIdAndUserId(id, userId).ifPresent(c -> {
                c.setSortOrder(sortOrder);
                c.setUpdatedAt(LocalDateTime.now());
                expensesCategoriesRepository.save(c);
            });
        }
    }

    private ExpenseCategories mapDtoToEntity(ExpensesCategoriesDTO dto) {
        ExpenseCategories entity = new ExpenseCategories();

        if (dto.getCategoryId() == null || dto.getCategoryId() == 0) {
            entity.setName(dto.getCategoryName());
            entity.setSortOrder(dto.getSortOrder());
            entity.setUserId(userService.getUserId());
            entity.setEnabled(dto.isEnabled());
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
        } else {
            entity = expensesCategoriesRepository
                    .findByIdAndUserId(Math.toIntExact(dto.getCategoryId()), userService.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));
            entity.setName(dto.getCategoryName());
            entity.setSortOrder(dto.getSortOrder());
            entity.setEnabled(dto.isEnabled());
            entity.setUpdatedAt(LocalDateTime.now());
            // Keep existing iconData unless a new file is uploaded in saveCategory
        }
        return entity;
    }

    public void deleteCategoryByID(int id) {
        Long userId = userService.getUserId();
        expensesCategoriesRepository.findByIdAndUserId(id, userId).ifPresent(expensesCategoriesRepository::delete);
    }

    @Transactional
    public void saveExpense(ExpenseRequest expenseRequest) {
            if (expenseRequest.getMonth() == null || expenseRequest.getMonth().isBlank()) {
                throw new IllegalArgumentException("Month is required.");
            }
            if (expenseRequest.getExpenseType() == null || expenseRequest.getExpenseType().isBlank()) {
                throw new IllegalArgumentException("Expense type is required.");
            }
            Long userId = userService.getUserId();
            if (userId == null || userId <= 0) {
                throw new IllegalStateException("You must be signed in to save expenses.");
            }

            YearMonth yearMonth = YearMonth.parse(expenseRequest.getMonth());
            int year = yearMonth.getYear();
            int month = yearMonth.getMonthValue();

            Map<Integer, Double> amounts = expenseRequest.getExpenses();
            if (amounts == null) {
                amounts = new HashMap<>();
            }

            boolean isPlanned = "plan".equalsIgnoreCase(expenseRequest.getExpenseType());

            Optional<Expenses> existingExpenseOpt = expensesRepository.findByUserIdAndExpenseYearAndExpenseMonth(userId, year, month);

            Expenses expenseEntity;
            if (existingExpenseOpt.isPresent()) {
                expenseEntity = existingExpenseOpt.get();
                expenseEntity.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));

                if (isPlanned) {
                    expenseEntity.setPlannedExpenses(amounts);
                } else {
                    expenseEntity.setActualExpenses(amounts);
                }
            } else {
                expenseEntity = new Expenses();
                expenseEntity.setUserId(userId);
                expenseEntity.setExpenseYear(year);
                expenseEntity.setExpenseMonth(month);
                expenseEntity.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
                expenseEntity.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));

                if (isPlanned) {
                    expenseEntity.setPlannedExpenses(amounts);
                    expenseEntity.setActualExpenses(null);
                } else {
                    expenseEntity.setActualExpenses(amounts);
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
        reportList.sort(Comparator.comparingInt(r -> categorySortedMap.getOrDefault(r.getCategory(), Integer.MAX_VALUE)));

        return reportList;
    }
    @Transactional
    public void deleteExpense(Long expenseId, String type) {
        Optional<Expenses> opt = expensesRepository.findByIdAndUserId(expenseId, userService.getUserId());
        if (opt.isEmpty()) {
            return;
        }
        Expenses expense = opt.get();
        int id = expense.getId();
        boolean hasPlan = expense.getPlannedExpenses() != null && !expense.getPlannedExpenses().isEmpty();
        boolean hasActual = expense.getActualExpenses() != null && !expense.getActualExpenses().isEmpty();

        if ("plan".equalsIgnoreCase(type)) {
            if (hasActual) {
                expensesRepository.clearPlanById((long) id);
            } else {
                expensesRepository.deleteById(id);
            }
        } else if ("actual".equalsIgnoreCase(type)) {
            if (hasPlan) {
                expensesRepository.clearActualById((long) id);
            } else {
                expensesRepository.deleteById(id);
            }
        }
    }

    public List<ExpenseReportDTO> getYearlyCategoryWiseExpenses(int year, Map<String, Double> categorySums, Map<Integer, Double> monthlySums, Map<String, Double> categoryAverages) {
        List<Expenses> expenses = expensesRepository.findByUserIdAndExpenseYearOrderByExpenseMonth(userService.getUserId(),year);

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

    public Map<String, Object> getYearWiseExpenseData(int year) {

        Map<String, Double> categorySums = new LinkedHashMap<>();
        Map<Integer, Double> monthlySums = new HashMap<>();
        Map<String, Double> categoryAverages = new HashMap<>();

        DecimalFormat decimalFormat = new DecimalFormat("#,###");
        List<ExpenseReportDTO> report = getYearlyCategoryWiseExpenses(year, categorySums, monthlySums, categoryAverages);

        double grandTotalSum = categorySums.values().stream().mapToDouble(Double::doubleValue).sum();
        double grandTotalAverage = categoryAverages.values().stream().mapToDouble(Double::doubleValue).sum();

        for (ExpenseReportDTO dto : report) {
            dto.setActualAmountStr(decimalFormat.format(dto.getActualAmount()));
        }
        Map<String, String> formattedCategorySums = new LinkedHashMap<>();
        categorySums.forEach((key, value) -> formattedCategorySums.put(key, decimalFormat.format(value)));

        Map<String, String> formattedCategoryAverages = new LinkedHashMap<>();
        categoryAverages.forEach((key, value) -> formattedCategoryAverages.put(key, decimalFormat.format(value)));

        Map<String, String> formattedMonthlySums = new LinkedHashMap<>();
        monthlySums.forEach((key, value) -> formattedMonthlySums.put(String.valueOf(key), decimalFormat.format(value)));

        Map<String, Object> data = new HashMap<>();
        data.put("expenseReport", report);
        data.put("categorySums", formattedCategorySums);
        data.put("monthlySums", formattedMonthlySums);
        data.put("categoryAverages", formattedCategoryAverages);
        data.put("grandTotal", decimalFormat.format(grandTotalSum));
        data.put("totalAverage", decimalFormat.format(grandTotalAverage));
        data.put("year", year);

        return data;
    }

    /**
     * Category rows × year columns (actual amounts), for all years that have expense data.
     */
    public Map<String, Object> getCategoryYearWiseExpenseData() {
        Long userId = userService.getUserId();
        List<Expenses> expenses = expensesRepository.findByUserId(userId);
        DecimalFormat decimalFormat = new DecimalFormat("#,###");

        Map<Integer, Map<Integer, Double>> categoryYearAmounts = new HashMap<>();
        TreeSet<Integer> years = new TreeSet<>();

        for (Expenses expense : expenses) {
            if (expense.getActualExpenses() == null || expense.getActualExpenses().isEmpty()) {
                continue;
            }
            int year = expense.getExpenseYear();
            years.add(year);
            for (Map.Entry<Integer, Double> entry : expense.getActualExpenses().entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                categoryYearAmounts
                        .computeIfAbsent(entry.getKey(), id -> new HashMap<>())
                        .merge(year, entry.getValue(), Double::sum);
            }
        }

        Map<String, Object> empty = new HashMap<>();
        empty.put("years", List.of());
        empty.put("rows", List.of());
        empty.put("yearTotals", Map.of());
        empty.put("grandTotal", "0");
        if (years.isEmpty() || categoryYearAmounts.isEmpty()) {
            return empty;
        }

        List<Object[]> categoryData = (List<Object[]>) expensesCategoriesRepository
                .findCategoryNamesByIds(new ArrayList<>(categoryYearAmounts.keySet()));
        LinkedHashMap<Integer, String> categoryNamesById = categoryData.stream()
                .sorted(Comparator.comparing(row -> (Integer) row[2]))
                .collect(Collectors.toMap(
                        row -> (Integer) row[0],
                        row -> (String) row[1],
                        (a, b) -> a,
                        LinkedHashMap::new));

        Map<Integer, Double> yearTotalsRaw = new TreeMap<>();
        for (Integer year : years) {
            yearTotalsRaw.put(year, 0.0);
        }

        List<CategoryYearAmountRow> rows = new ArrayList<>();
        double grandTotal = 0.0;
        for (Map.Entry<Integer, String> catEntry : categoryNamesById.entrySet()) {
            Integer categoryId = catEntry.getKey();
            Map<Integer, Double> byYear = categoryYearAmounts.getOrDefault(categoryId, Map.of());
            CategoryYearAmountRow row = new CategoryYearAmountRow();
            row.setCategory(catEntry.getValue());
            Map<Integer, String> formatted = new LinkedHashMap<>();
            double rowTotal = 0.0;
            for (Integer year : years) {
                double amount = byYear.getOrDefault(year, 0.0);
                formatted.put(year, amount > 0 ? decimalFormat.format(Math.floor(amount)) : "0");
                rowTotal += amount;
                yearTotalsRaw.merge(year, amount, Double::sum);
            }
            row.setAmountsByYear(formatted);
            row.setTotal(decimalFormat.format(Math.floor(rowTotal)));
            rows.add(row);
            grandTotal += rowTotal;
        }

        Map<Integer, String> yearTotals = new LinkedHashMap<>();
        for (Integer year : years) {
            yearTotals.put(year, decimalFormat.format(Math.floor(yearTotalsRaw.getOrDefault(year, 0.0))));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("years", new ArrayList<>(years));
        data.put("rows", rows);
        data.put("yearTotals", yearTotals);
        data.put("grandTotal", decimalFormat.format(Math.floor(grandTotal)));
        return data;
    }

    public void generateCategoryYearExpensePdf(OutputStream outputStream, Map<String, Object> data) {
        try {
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDoc = new PdfDocument(writer);
            pdfDoc.setDefaultPageSize(PageSize.A4.rotate());
            Document document = new Document(pdfDoc);

            document.add(new Paragraph("CATEGORY-WISE YEAR AMOUNTS")
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(10));

            @SuppressWarnings("unchecked")
            List<Integer> years = (List<Integer>) data.get("years");
            @SuppressWarnings("unchecked")
            List<CategoryYearAmountRow> rows = (List<CategoryYearAmountRow>) data.get("rows");
            @SuppressWarnings("unchecked")
            Map<Integer, String> yearTotals = (Map<Integer, String>) data.get("yearTotals");
            String grandTotal = (String) data.get("grandTotal");

            if (years == null || years.isEmpty() || rows == null || rows.isEmpty()) {
                document.add(new Paragraph("No expense data available.")
                        .setTextAlignment(TextAlignment.CENTER));
                document.close();
                return;
            }

            int cols = years.size() + 2;
            Table table = new Table(cols).useAllAvailableWidth();
            table.setHorizontalAlignment(HorizontalAlignment.CENTER);

            table.addHeaderCell(headerCell("CATEGORY"));
            for (Integer year : years) {
                table.addHeaderCell(headerCell(String.valueOf(year)));
            }
            table.addHeaderCell(headerCell("TOTAL"));

            for (CategoryYearAmountRow row : rows) {
                table.addCell(new Cell().add(new Paragraph(row.getCategory() != null ? row.getCategory() : ""))
                        .setBold().setPaddingLeft(5));
                for (Integer year : years) {
                    String value = row.getAmountsByYear() != null
                            ? row.getAmountsByYear().getOrDefault(year, "0")
                            : "0";
                    table.addCell(new Cell().add(new Paragraph(value)).setTextAlignment(TextAlignment.RIGHT));
                }
                table.addCell(new Cell().add(new Paragraph(row.getTotal() != null ? row.getTotal() : "0"))
                        .setTextAlignment(TextAlignment.RIGHT).setBold());
            }

            DeviceRgb footerBg = new DeviceRgb(241, 248, 233);
            table.addCell(new Cell().add(new Paragraph("Total")).setBold().setBackgroundColor(footerBg));
            for (Integer year : years) {
                table.addCell(new Cell()
                        .add(new Paragraph(yearTotals.getOrDefault(year, "0")))
                        .setBold()
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setBackgroundColor(footerBg));
            }
            table.addCell(new Cell()
                    .add(new Paragraph(grandTotal != null ? grandTotal : "0"))
                    .setBold()
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBackgroundColor(footerBg));

            document.add(table);
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Cell headerCell(String text) {
        return new Cell()
                .add(new Paragraph(text))
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setFontColor(new DeviceRgb(255, 255, 255))
                .setBackgroundColor(new DeviceRgb(0, 100, 148));
    }

public void generateYearlyExpensePdf(OutputStream outputStream, Map<String, Object> data) {
    try {
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdfDoc = new PdfDocument(writer);
        pdfDoc.setDefaultPageSize(PageSize.A4.rotate());
        Document document = new Document(pdfDoc);

        Paragraph title = new Paragraph("YEARLY EXPENSE SUMMARY - " + data.get("year"))
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(10);
        document.add(title);

        @SuppressWarnings("unchecked")
        Map<String, String> categorySums = (Map<String, String>) data.get("categorySums");
        @SuppressWarnings("unchecked")
        List<ExpenseReportDTO> expenseReport = (List<ExpenseReportDTO>) data.get("expenseReport");
        @SuppressWarnings("unchecked")
        Map<Integer, String> monthlySums = (Map<Integer, String>) data.get("monthlySums");
        @SuppressWarnings("unchecked")
        Map<String, String> formattedCategoryAverages = (Map<String, String>) data.get("categoryAverages");

        String grandTotal = (String) data.get("grandTotal");
        String totalAverage = (String) data.get("totalAverage");

        Map<String, String[]> categoryMonthData = new HashMap<>();
        for (ExpenseReportDTO expense : expenseReport) {
            categoryMonthData.putIfAbsent(expense.getCategory(), new String[12]);
            String[] monthlyValues = categoryMonthData.get(expense.getCategory());
            monthlyValues[expense.getMonth() - 1] = expense.getActualAmountStr();
        }

        float[] columnWidths = new float[]{3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2};
        Table table = new Table(columnWidths).useAllAvailableWidth();
        table.setAutoLayout();
        table.setHorizontalAlignment(HorizontalAlignment.CENTER);

        String[] headers = {"CATEGORY", "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC", "TOTAL", "AVG"};
        for (String header : headers) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(header))
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setFontColor(new DeviceRgb(255,255,255))
                    .setBackgroundColor(new DeviceRgb(0, 100, 148)));
        }

        for (Map.Entry<String, String> entry : categorySums.entrySet()) {
            String category = entry.getKey();
            table.addCell(new Cell().add(new Paragraph(category)).setBold().setPaddingLeft(5));

            String[] monthlyExpenses = categoryMonthData.getOrDefault(category, new String[12]);
            for (int month = 0; month < 12; month++) {
                String value = (monthlyExpenses[month] != null) ? monthlyExpenses[month] : "0";
                table.addCell(new Cell().add(new Paragraph(value)).setTextAlignment(TextAlignment.RIGHT));
            }

            String total = entry.getValue();
            String avg = formattedCategoryAverages.getOrDefault(category, "0");

            table.addCell(new Cell().add(new Paragraph(total)).setTextAlignment(TextAlignment.RIGHT).setBold());
            table.addCell(new Cell().add(new Paragraph(avg)).setTextAlignment(TextAlignment.RIGHT).setBold());
        }

        table.addCell(new Cell().add(new Paragraph("Total")).setBold().setBackgroundColor(new DeviceRgb(241,248,233)));
        for (int month = 1; month <= 12; month++) {
            String monthTotal = monthlySums.getOrDefault(month, "0");
            table.addCell(new Cell().add(new Paragraph(monthTotal)).setBold().setTextAlignment(TextAlignment.RIGHT).setBackgroundColor(new DeviceRgb(241,248,233)));
        }

        table.addCell(new Cell().add(new Paragraph(grandTotal)).setBold().setTextAlignment(TextAlignment.RIGHT).setBackgroundColor(new DeviceRgb(241,248,233)));
        table.addCell(new Cell().add(new Paragraph(totalAverage)).setBold().setTextAlignment(TextAlignment.RIGHT).setBackgroundColor(new DeviceRgb(241,248,233)));

        document.add(table);
        document.close();
    } catch (Exception e) {
        e.printStackTrace();
    }
}
    public Map<String, Integer> getYearlyExpenseData() {
        List<Expenses> expenses = expensesRepository.findByUserId(userService.getUserId());
        Map<String, Integer> yearlyExpensesMap = new LinkedHashMap<>();
        yearlyExpensesMap = expenses.stream()
                .collect(Collectors.groupingBy(
                        exp -> String.valueOf(exp.getExpenseYear()),
                        Collectors.collectingAndThen(
                                Collectors.summingDouble(exp -> {
                                    Map<Integer, Double> actuals = exp.getActualExpenses();
                                    return actuals != null
                                            ? actuals.values().stream().mapToDouble(d -> d).sum()
                                            : 0.0;
                                }),
                                sum -> (int) Math.round(sum)
                        )
                ));
        return yearlyExpensesMap;
    }
public Map<String, Integer> getMonthlyExpenseData(int currentYear) {
    List<Expenses> expenses = expensesRepository.findByUserIdAndExpenseYearOrderByExpenseMonth(userService.getUserId(), currentYear);
    Map<String, Integer> expenseMap = new LinkedHashMap<>();

    for (Expenses expense : expenses) {
        String monthName = formatterUtils.getMonthName(expense.getExpenseMonth());
        int totalAmount = expense.getActualExpenses().values().stream()
                .mapToInt(amount -> (int) Math.round(amount))
                .sum();
        expenseMap.put(monthName, expenseMap.getOrDefault(monthName, 0) + totalAmount);
    }

    return expenseMap;
    }

}
