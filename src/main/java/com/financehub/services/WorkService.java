package com.financehub.services;

import com.financehub.dtos.CompanyDTO;
import com.financehub.dtos.SalaryDTO;
import com.financehub.entities.ClientUser;
import com.financehub.entities.Company;
import com.financehub.entities.Salary;
import com.financehub.repositories.ClientUserRepository;
import com.financehub.repositories.CompanyRepository;
import com.financehub.repositories.SalaryRepository;
import com.financehub.utils.FormatterUtils;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpSession;
import org.bouncycastle.its.ITSValidityPeriod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.OutputStream;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WorkService {
    @Autowired
    private HttpSession session;
    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private SalaryRepository salaryRepository;
    @Autowired
    private ClientUserRepository clientUserRepository;
    @Autowired
    private FormatterUtils formatterUtils;
    @Autowired
    private UserService userService;

    public List<CompanyDTO> getCompaniesByUserName() {
        List<CompanyDTO> companyDTOs = null;
        if (userService.getUserId() != 0) {
            List<Company> companies = companyRepository.findCompaniesByUserId(userService.getUserId());
            companyDTOs =companies.stream()
                    .sorted(Comparator.comparing(Company::getExperienceFrom))
                    .map(CompanyDTO::new)
                    .toList();
        }
        return companyDTOs;
    }
    public CompanyDTO getExperienceById(Long id) {
        Optional<Company> optionalCompany = companyRepository.findById(id);
        if (optionalCompany.isEmpty()) {
            throw new EntityNotFoundException("Company with ID " + id + " not found");
        }
        return new CompanyDTO(optionalCompany.get());
    }
    public SalaryDTO getSalaryById(Long id) {
        Optional<Salary> optionalSalary = salaryRepository.findById(id);
        if (optionalSalary.isEmpty()) {
            throw new EntityNotFoundException("Salry with ID " + id + " not found");
        }
        return new SalaryDTO(optionalSalary.get());
    }
    public boolean hasSalariesForCompany(Long companyId) {
        return salaryRepository.existsByCompanyId(companyId);
    }
    public void deleteCompany(Long id) {
        Optional<Company> companyOpt = companyRepository.findById(id);
        companyOpt.ifPresent(company -> companyRepository.delete(company));
    }
    public void deleteSalary(Long id) {
        Optional<Salary> salaryOpt = salaryRepository.findById(id);
        salaryOpt.ifPresent(salary -> salaryRepository.delete(salary));
    }

    public void addCompany(CompanyDTO companyDTO) {
        Company company = new Company();
        if (companyDTO.getCompanyId() != null) {
            company = companyRepository.findById(companyDTO.getCompanyId())
                    .orElseThrow(() -> new RuntimeException("Company not found with ID: " + companyDTO.getCompanyId()));
        }
        company.setUserId(Math.toIntExact(userService.getUserId()));
        company.setCompanyName(companyDTO.getCompanyName());
        company.setClient(companyDTO.getClientName());
        company.setProject(companyDTO.getProjectName());
        company.setExperienceFrom(companyDTO.getFromDate());
        company.setExperienceTo(companyDTO.getToDate());
        company.setIsCurrentCompany(companyDTO.isCurrentlyEmployed());
        company.setCreatedAt(company.getCreatedAt() != null ? company.getCreatedAt() : LocalDateTime.now());
        company.setUpdatedAt(LocalDateTime.now());
        companyRepository.save(company);
    }
    public void addSalary(SalaryDTO salaryDTO) {
        String username = (String) session.getAttribute("username");
        Company company = companyRepository.findById(salaryDTO.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Company not found with ID: " + salaryDTO.getCompanyId()));

        ClientUser user = clientUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));

        Salary salary;
        if (salaryDTO.getSalaryId() != null) {
            salary = salaryRepository.findById(salaryDTO.getSalaryId())
                    .orElseThrow(() -> new RuntimeException("Salary not found with ID: " + salaryDTO.getSalaryId()));
        }else {
            salary = new Salary();
            salary.setUser(user);
            salary.setCreatedAt(LocalDateTime.now());
        }
        salary.setCompany(company);
        salary.setSalaryMonth(salaryDTO.getMonth());
        salary.setSalaryYear(salaryDTO.getYear());
        salary.setCreditDate(salaryDTO.getDateCredited());
        salary.setSalaryAmount(salaryDTO.getSalaryAmount());
        salary.setUpdatedAt(LocalDateTime.now());
        salaryRepository.save(salary);
    }

    public Map<Integer, List<SalaryDTO>> getAllSalaries()  {
        String username = (String) session.getAttribute("username");
        ClientUser user = clientUserRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found : " + username));

        List<Salary> salaries = salaryRepository.findAllSalariesByUser(user);

        return salaries.stream()
                .map(SalaryDTO::new)
                .collect(Collectors.groupingBy(
                        SalaryDTO::getYear,
                        TreeMap::new,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.stream()
                                        .sorted(Comparator.comparing(SalaryDTO::getDateCredited))
                                        .toList())));
    }

    public void generateSalaryPdf(OutputStream outputStream, Map<Integer, List<SalaryDTO>> yearMonthData) throws Exception {

        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        document.add(new Paragraph("SALARY REPORT")
                .setBold()
                .setFontSize(20)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(15));

        for (Map.Entry<Integer, List<SalaryDTO>> entry : yearMonthData.entrySet()) {
            Integer year = entry.getKey();
            List<SalaryDTO> salaries = entry.getValue();
            document.add(new Paragraph("YEAR - " + year)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setHorizontalAlignment(HorizontalAlignment.CENTER)
                    .setWidth(UnitValue.createPercentValue(90))
                    .setFontColor(new DeviceRgb(255,255,255))
                    .setBackgroundColor(new DeviceRgb(0, 100, 148))
                    .setFontSize(16)
                    .setMarginBottom(0));

            Table table = new Table(4);
            table.setWidth(UnitValue.createPercentValue(90));
            table.setHorizontalAlignment(HorizontalAlignment.CENTER);
            table.addHeaderCell(formatterUtils.createStyledCell("Company",0));
            table.addHeaderCell(formatterUtils.createStyledCell("Month",0));
            table.addHeaderCell(formatterUtils.createStyledCell("Date Credited",0));
            table.addHeaderCell(formatterUtils.createStyledCell("Salary Amount",0));

            double totalSalaryForYear = 0;
            for (SalaryDTO salary : salaries) {
                table.addCell(formatterUtils.createStyledCell(Character.toUpperCase(salary.getCompanyName().charAt(0)) + salary.getCompanyName().substring(1).toLowerCase() ,1));
                table.addCell(formatterUtils.createStyledCell(formatterUtils.getMonthName(salary.getMonth()),1));
                table.addCell(formatterUtils.createStyledCell(formatterUtils.formatDate(salary.getDateCredited()),1));
                table.addCell(formatterUtils.createStyledCell(formatterUtils.formatInIndianStyle(salary.getSalaryAmount()),2));
                totalSalaryForYear += salary.getSalaryAmount();
            }

            table.addCell(new Cell(1, 3)
                    .add(new Paragraph("Total For Year :"))
                    .setBackgroundColor(new DeviceRgb(241,248,233))
                    .setTextAlignment(TextAlignment.RIGHT).setPaddingRight(50)
                    .setBold());
            table.addCell(formatterUtils.createStyledCell(formatterUtils.formatInIndianStyle(totalSalaryForYear), 3));

            document.add(table);
            document.add(new Paragraph("\n"));
        }

        Table summaryTable = new Table(2);
        summaryTable.setWidth(UnitValue.createPercentValue(70));
        summaryTable.setHorizontalAlignment(HorizontalAlignment.CENTER);
        Cell titleCell = new Cell(1, 2);
        titleCell.add(new Paragraph("YEAR-WISE TOTALS").setTextAlignment(TextAlignment.CENTER).setBold());
        titleCell.setBackgroundColor(new DeviceRgb(0, 100, 148));
        titleCell.setFontColor(new DeviceRgb(255,255,255));
        titleCell.setPadding(5);
        summaryTable.addHeaderCell(titleCell);
        summaryTable.addHeaderCell(formatterUtils.createStyledCell("Year", 0));
        summaryTable.addHeaderCell(formatterUtils.createStyledCell("Total Salary", 0));

        double totalSalary = 0;
        for (Map.Entry<Integer, List<SalaryDTO>> entry : yearMonthData.entrySet()) {
            Integer year = entry.getKey();
            List<SalaryDTO> salaries = entry.getValue();

            double totalSalaryForYear = 0;
            for (SalaryDTO salary : salaries) {
                totalSalaryForYear += salary.getSalaryAmount();
            }
            totalSalary += totalSalaryForYear;
            summaryTable.addCell(formatterUtils.createStyledCell(String.valueOf(year), 1));
            summaryTable.addCell(formatterUtils.createStyledCell(formatterUtils.formatInIndianStyle(totalSalaryForYear), 2));
        }
        summaryTable.addCell(new Cell()
                .add(new Paragraph("Grand Total"))
                .setBackgroundColor(new DeviceRgb(241,248,233))
                .setTextAlignment(TextAlignment.CENTER)
                .setBold());
        summaryTable.addCell(formatterUtils.createStyledCell(formatterUtils.formatInIndianStyle(totalSalary), 3));

        document.add(summaryTable);
        document.close();
    }

    public void generateExperiencePdf(OutputStream outputStream, List<CompanyDTO> companies) {
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        document.add(new Paragraph("EXPERIENCE REPORT")
                .setBold()
                .setFontSize(20)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(15));

        Table table = new Table(5);
        table.setWidth(UnitValue.createPercentValue(100));
        table.setHorizontalAlignment(HorizontalAlignment.CENTER);

        table.addHeaderCell(formatterUtils.createStyledCell("Company", 0));
        table.addHeaderCell(formatterUtils.createStyledCell("Client", 0));
        table.addHeaderCell(formatterUtils.createStyledCell("Project", 0));
        table.addHeaderCell(formatterUtils.createStyledCell("From Date", 0));
        table.addHeaderCell(formatterUtils.createStyledCell("To Date", 0));

        for (CompanyDTO company : companies) {
            table.addCell(formatterUtils.createStyledCell(Character.toUpperCase(company.getCompanyName().charAt(0)) + company.getCompanyName().substring(1).toLowerCase(), 1));
            table.addCell(formatterUtils.createStyledCell(Character.toUpperCase(company.getClientName().charAt(0)) + company.getClientName().substring(1).toLowerCase(), 1));
            table.addCell(formatterUtils.createStyledCell(Character.toUpperCase(company.getProjectName().charAt(0)) + company.getProjectName().substring(1).toLowerCase(), 1));
            table.addCell(formatterUtils.createStyledCell(formatterUtils.formatDate(company.getFromDate()), 1));
            table.addCell(formatterUtils.createStyledCell(company.isCurrentlyEmployed() ? "Currently Employed" : formatterUtils.formatDate(company.getToDate()), 1));
        }
        document.add(table);
        document.add(new Paragraph("\n"));

        String explanation = formatterUtils.getTotalExp(companies);

        document.add(new Paragraph(explanation)
                .setFontSize(14)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10));
        document.close();
    }

    public String checkExpDates(CompanyDTO companyDTO, RedirectAttributes redirectAttributes) {
        List<CompanyDTO> existingCompanies = getCompaniesByUserName();
        if (companyDTO.getCompanyId() != null) {
            existingCompanies = existingCompanies.stream()
                    .filter(company -> !company.getCompanyId().equals(companyDTO.getCompanyId()))
                    .collect(Collectors.toList());
        }
        boolean hasOverlap = existingCompanies.stream().anyMatch(company -> {
            LocalDate existingFromDate = company.getFromDate();
            LocalDate existingToDate = company.getToDate() != null ? company.getToDate() : LocalDate.MAX;

            LocalDate newFromDate = companyDTO.getFromDate();
            LocalDate newToDate = companyDTO.isCurrentlyEmployed()? LocalDate.MAX:companyDTO.getToDate();

            return (newFromDate.isBefore(existingToDate) || newFromDate.isEqual(existingToDate)) &&
                    (newToDate.isAfter(existingFromDate) || newToDate.isEqual(existingFromDate));
        });
        boolean hasCurrentExperience = existingCompanies.stream()
                .anyMatch(CompanyDTO::isCurrentlyEmployed);

        if (companyDTO.isCurrentlyEmployed() && hasCurrentExperience) {
            redirectAttributes.addFlashAttribute("error", "You can only have one current experience marked.");
            return "redirect:/api/work/addExperience";
        }
        if (hasOverlap) {
            redirectAttributes.addFlashAttribute("error", "The experience period overlaps with an existing experience.");
            return "redirect:/api/work/addExperience";
        }

        return null;
    }

    @PostMapping("/calculate")
    public String calculate(
            @RequestParam("axis") double axis,
            @RequestParam("icici") double icici,
            @RequestParam("hdfc") double hdfc,
            @RequestParam("cc") double cc,
            @RequestParam("givnamnt") double givnamnt,
            Model model) {
        DecimalFormat indianFormat = new DecimalFormat("##,##,##,##0");
        axis = Math.ceil((axis*0.05)+(axis*0.05)*0.12+axis);
        hdfc = Math.ceil((hdfc*0.04)+(hdfc*0.04)*0.12+hdfc);
        StringBuilder resultMessage = new StringBuilder();
        resultMessage.append("<table><tr><td style=\"text-align: left;\">Total Loan: ").append("</td><td style=\"text-align: right;\">"+indianFormat.format(Math.ceil(axis+hdfc+icici))).append("\n");
        resultMessage.append("</td></tr><tr><td style=\"text-align: left;\">Credit Card: ").append("</td><td style=\"text-align: right;\">"+indianFormat.format(cc)).append("\n");
        resultMessage.append("</td></tr><tr><td style=\"text-align: left;\">Given Amount: ").append("</td><td style=\"text-align: right;\">"+indianFormat.format(givnamnt)).append("\n");

        double result = Math.ceil(axis+hdfc+icici+cc+givnamnt);
        resultMessage.append("</td></tr><tr><td style=\"text-align: left;\">Total Amount: ").append("</td><td style=\"text-align: right;\">"+indianFormat.format(result)).append("\n");
        double peracre = Math.ceil(result/1.85);
        resultMessage.append("</td></tr><tr><td style=\"text-align: left;\">Per Acre : ").append("</td><td style=\"text-align: right;\">"+indianFormat.format(peracre)+"</td></tr></table>");

        String formattedResult = resultMessage.toString().replaceAll("\n", "<br/>");
        model.addAttribute("result", formattedResult);

        return "index";
    }
}
