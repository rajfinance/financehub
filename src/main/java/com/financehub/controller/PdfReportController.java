package com.financehub.controller;

import com.financehub.dtos.CompanyDTO;
import com.financehub.dtos.OwnerDTO;
import com.financehub.dtos.RentSummaryDTO;
import com.financehub.dtos.SalaryDTO;
import com.financehub.entities.Owner;
import com.financehub.services.ExpensesService;
import com.financehub.services.RentalService;
import com.financehub.services.WorkService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api/pdf")
public class PdfReportController {

	private static final Logger log = LoggerFactory.getLogger(PdfReportController.class);

	private final WorkService workService;
	private final RentalService rentalService;
	private final ExpensesService expensesService;

	public PdfReportController(WorkService workService, RentalService rentalService, ExpensesService expensesService) {
		this.workService = workService;
		this.rentalService = rentalService;
		this.expensesService = expensesService;
	}

	@GetMapping("/salaryReportPdf")
	public void downloadSalaryReport(HttpServletResponse response) {
		try {
			Map<Integer, List<SalaryDTO>> salaryData = workService.getAllSalaries();
			response.setContentType("application/pdf");
			response.setHeader("Content-Disposition", "attachment; filename=SalaryReport.pdf");

			try (OutputStream outputStream = response.getOutputStream()) {
				workService.generateSalaryPdf(outputStream, salaryData);
			}
		} catch (Exception e) {
			log.error("Salary PDF failed", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/expReportPdf")
	public void downloadExperienceReport(HttpServletResponse response) {
		try {
			List<CompanyDTO> companies = workService.getCompaniesByUserName();
			response.setContentType("application/pdf");
			response.setHeader("Content-Disposition", "attachment; filename=expReport.pdf");

			try (OutputStream outputStream = response.getOutputStream()) {
				workService.generateExperiencePdf(outputStream, companies);
			}
		} catch (Exception e) {
			log.error("Experience PDF failed", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/ownersReportPdf")
	public void downloadOwnersReport(HttpServletResponse response) {
		try {
			List<OwnerDTO> owners = rentalService.getOwnersByUserId();
			response.setContentType("application/pdf");
			response.setHeader("Content-Disposition", "attachment;");

			try (OutputStream outputStream = response.getOutputStream()) {
				rentalService.generateOwnersPdf(outputStream, owners);
			}
		} catch (Exception e) {
			log.error("Owners PDF failed", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/rentPaymentReportPdf")
	public void downloadRentPaymentReport(HttpServletResponse response) {
		try {
			Map<Owner, RentSummaryDTO> paymentsByOwner = rentalService.getPaymentsGroupedByOwner();
			response.setContentType("application/pdf");
			response.setHeader("Content-Disposition", "attachment;");

			try (OutputStream outputStream = response.getOutputStream()) {
				rentalService.generateRentPaymentPdf(outputStream, paymentsByOwner);
			}
		} catch (Exception e) {
			log.error("Rent payment PDF failed", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/yearSummaryPdf")
	public void downloadYearSummaryReport(@RequestParam("year") int year, HttpServletResponse response) {
		try {
			Map<String, Object> data = expensesService.getYearWiseExpenseData(year);

			response.setContentType("application/pdf");
			response.setHeader("Content-Disposition", "attachment; filename=Yearly_Expense_Summary.pdf");

			try (OutputStream outputStream = response.getOutputStream()) {
				expensesService.generateYearlyExpensePdf(outputStream, data);
				outputStream.flush();
			} catch (IOException e) {
				log.error("Year summary PDF stream failed", e);
			}
		} catch (Exception e) {
			log.error("Year summary PDF failed", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
}
