package com.financehub.utils;

import com.financehub.dtos.CompanyDTO;
import com.financehub.dtos.OwnerDTO;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;


@Component
public class FormatterUtils {

    private static final Map<Integer, String> MONTH_NAMES = new LinkedHashMap<>();

    static {
        MONTH_NAMES.put(1, "January");
        MONTH_NAMES.put(2, "February");
        MONTH_NAMES.put(3, "March");
        MONTH_NAMES.put(4, "April");
        MONTH_NAMES.put(5, "May");
        MONTH_NAMES.put(6, "June");
        MONTH_NAMES.put(7, "July");
        MONTH_NAMES.put(8, "August");
        MONTH_NAMES.put(9, "September");
        MONTH_NAMES.put(10, "October");
        MONTH_NAMES.put(11, "November");
        MONTH_NAMES.put(12, "December");
    }
    public String getMonthName(int month) {
        return MONTH_NAMES.getOrDefault(month, "Unknown");
    }

    public String formatDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return date.format(formatter);
    }
    public String formatDateToCustomPattern(LocalDate date) {
        if (date == null) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MMM/yyyy");
        return date.format(formatter);
    }
    public String formatSalary(double salary) {
        NumberFormat currencyFormatter = NumberFormat.getNumberInstance(Locale.US);
        currencyFormatter.setMinimumFractionDigits(2);
        currencyFormatter.setMaximumFractionDigits(2);
        return currencyFormatter.format(salary);
    }

    public String formatInIndianStyle(double salary) {
        DecimalFormat decimalFormat = new DecimalFormat("####.00");
        String formatted = decimalFormat.format(salary);
        formatted = applyIndianNumberingSystem(formatted);
        return formatted;
    }

    private String applyIndianNumberingSystem(String number) {
        String[] parts = number.split("\\.");
        String integerPart = parts[0];
        String decimalPart = parts.length > 1 ? parts[1] : "";
        StringBuilder stringBuilder = new StringBuilder();
        char amountArray[] = integerPart.toCharArray();
        int a = 0, b = 0;
        for (int i = amountArray.length - 1; i >= 0; i--) {
            if (a < 3) {
                stringBuilder.append(amountArray[i]);
                a++;
            } else if (b < 2) {
                if (b == 0) {
                    stringBuilder.append(",");
                    stringBuilder.append(amountArray[i]);
                    b++;
                } else {
                    stringBuilder.append(amountArray[i]);
                    b = 0;
                }
            }
        }
        return stringBuilder.reverse().toString()+"."+decimalPart;
    }

    public Cell createStyledCell(Object content, int isHeader) {
        PdfFont font = null;
        try {
            font = PdfFontFactory.createFont("Helvetica", PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Cell cell = new Cell().add(new Paragraph(content == null ? "" : content.toString()).setFont(font)).setPadding(5);

        if (isHeader == 0) {
            cell.setBackgroundColor(new DeviceRgb(227, 242, 253))
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER);
        } else {
            if (isHeader == 1) {
                cell.setTextAlignment(TextAlignment.CENTER);
            } else if(isHeader == 2) {
                cell.setTextAlignment(TextAlignment.RIGHT).setPaddingRight(10);
            } else if(isHeader == 3) {
                cell.setTextAlignment(TextAlignment.RIGHT).setPaddingRight(10).setBold().setBackgroundColor(new DeviceRgb(241,248,233));
            } else if(isHeader == 4) {
                cell.setTextAlignment(TextAlignment.CENTER).setBold().setBackgroundColor(new DeviceRgb(241,248,233));
            }
        }
        return cell;
    }
    public String calculateDurationAsString(LocalDate fromDate, LocalDate toDate, boolean currentlyEmployed) {
        if (fromDate != null) {
            if (toDate == null && currentlyEmployed) {
                toDate = LocalDate.now();
            }
            LocalDate endDate = (toDate != null) ? toDate : LocalDate.now();
            Period period = Period.between(fromDate, endDate);
            int years = period.getYears();
            int months = period.getMonths();
            int days = period.getDays();
            if (days >= 30) {
                months += days / 30;
                days = days % 30;
            }
            if (months >= 12) {
                years += months / 12;
                months = months % 12;
            }
            return String.format("%d|%d|%d", years, months, days);
        }
        return "0|0|0";
    }

    public String getTotalExp(List<CompanyDTO> companies) {
        int totalYears = 0;
        int totalMonths = 0;
        int totalDays = 0;

        for (CompanyDTO company : companies) {
            String duration = calculateDurationAsString(
                    company.getFromDate(),
                    company.getToDate(),
                    company.isCurrentlyEmployed()
            );

            String[] parts = duration.split("\\|");
            int years = Integer.parseInt(parts[0]);
            int months = Integer.parseInt(parts[1]);
            int days = Integer.parseInt(parts[2]);

            totalYears += years;
            totalMonths += months;
            totalDays += days;
        }

        if (totalDays >= 30) {
            totalMonths += totalDays / 30;
            totalDays = totalDays % 30;
        }

        if (totalMonths >= 12) {
            totalYears += totalMonths / 12;
            totalMonths = totalMonths % 12;
        }

        String explanation = String.format("Total experience %d years %d months %d days", totalYears, totalMonths, totalDays);
        return explanation;
    }

    public String getTotalAdvance(List<OwnerDTO> owners) {
        String totAdvance=null;
        Double totAdv=0.0;
        for (OwnerDTO owner : owners) {
            totAdv+=owner.getAdvanceAmount();
        }
        totAdvance = String.format("Total Advance Amount : "+formatInIndianStyle(totAdv));
        return totAdvance;
    }

    public Period parsePeriod(String periodString) {
        int years = 0, months = 0, days = 0;

        if (periodString.contains("year")) {
            String yearPart = periodString.split("year")[0].trim();
            years = Integer.parseInt(yearPart.split(" ")[yearPart.split(" ").length - 1]);
        }

        if (periodString.contains("month")) {
            String monthPart = periodString.split("month")[0].trim();
            months = Integer.parseInt(monthPart.split(" ")[monthPart.split(" ").length - 1]);
        }

        if (periodString.contains("day")) {
            String dayPart = periodString.split("day")[0].trim();
            days = Integer.parseInt(dayPart.split(" ")[dayPart.split(" ").length - 1]);
        }

        return Period.of(years, months, days);
    }

    public String formatPeriod(Period period) {
        int years = period.getYears();
        int months = period.getMonths();
        int days = period.getDays();

        StringBuilder formattedPeriod = new StringBuilder();
        if (years > 0) {
            formattedPeriod.append(years).append(" year").append(years > 1 ? "s" : "");
        }
        if (months > 0) {
            if (formattedPeriod.length() > 0) {
                formattedPeriod.append(" ");
            }
            formattedPeriod.append(months).append(" month").append(months > 1 ? "s" : "");
        }
        if (days > 0) {
            if (formattedPeriod.length() > 0) {
                formattedPeriod.append(" ");
            }
            formattedPeriod.append(days).append(" day").append(days > 1 ? "s" : "");
        }

        return formattedPeriod.toString().isEmpty() ? "0 days" : formattedPeriod.toString();
    }

}
