package com.financehub.services;

import com.financehub.dtos.*;
import com.financehub.entities.Owner;
import com.financehub.entities.RentPayment;
import com.financehub.repositories.ClientUserRepository;
import com.financehub.repositories.OwnerRepository;
import com.financehub.repositories.RentPaymentRepository;
import com.financehub.utils.FormatterUtils;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RentalService {
    @Autowired
    private HttpSession session;
    @Autowired
    private OwnerRepository ownerRepository;
    @Autowired
    private RentPaymentRepository rentPaymentRepository;
    @Autowired
    private ClientUserRepository clientUserRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private FormatterUtils formatterUtils;
    public OwnerDTO getOwnerDTOById(Long id) {
        return ownerRepository.findById(id)
                .map(OwnerDTO::new)
                .orElseThrow(() -> new EntityNotFoundException("Owner not found with id " + id));
    }
    public Optional<Owner> getOwnerById(Long id) {
        return ownerRepository.findById(id);
    }
    public RentPaymentDTO getPaymentById(Long paymentId) {
        return rentPaymentRepository.findById(paymentId)
                .map(RentPaymentDTO::new)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found with id " + paymentId));
    }
    public void deleteOwner(Long id) {
        Optional<Owner> ownerOpt = ownerRepository.findById(id);
        ownerOpt.ifPresent(owner -> ownerRepository.delete(owner));
    }
    public void deleteRentPayment(Long id) {
        Optional<RentPayment> paymentOpt = rentPaymentRepository.findById(id);
        paymentOpt.ifPresent(payment -> rentPaymentRepository.delete(payment));
    }
    public List<OwnerDTO> getOwnersByUserId() {
        List<Owner> owners =  ownerRepository.findByUserId(userService.getUserId());
        List<OwnerDTO> ownersDto = owners.stream()
                .map(OwnerDTO::new)
                .sorted(Comparator.comparing(OwnerDTO::getAdvanceDate))
                .toList();

        return ownersDto.stream()
                .peek(owner -> {
                    if (owner.getAdvanceDate() != null) {
                        owner.setFormattedAdvanceDate(formatterUtils.formatDate(owner.getAdvanceDate()));
                    }
                })
                .collect(Collectors.toList());
    }
    public void saveOwner(OwnerDTO ownerDTO) {
        Owner owner = (ownerDTO.getOwnerId() != null) ? ownerRepository.findById(ownerDTO.getOwnerId()).orElse(new Owner()) : new Owner();

        boolean isDuplicateDate;

        if (ownerDTO.getOwnerId() != null) {
            isDuplicateDate = ownerRepository.existsByAdvanceDateAndIdNot(ownerDTO.getAdvanceDate(),ownerDTO.getOwnerId()
            );
        } else {
            isDuplicateDate = ownerRepository.existsByAdvanceDate(ownerDTO.getAdvanceDate());
        }
        if (isDuplicateDate) {
            throw new IllegalArgumentException("Another owner already has this advance date.");
        }

        owner.setUserId(userService.getUserId());
        owner.setName(ownerDTO.getName());
        owner.setPhoneNumber(ownerDTO.getPhoneNumber());
        owner.setAddress(ownerDTO.getAddress());
        owner.setAdvanceMonths(ownerDTO.getAdvanceMonths());
        owner.setAdvanceAmount(ownerDTO.getAdvanceAmount());
        owner.setAdvanceDate(ownerDTO.getAdvanceDate());
        owner.setCreatedAt(owner.getCreatedAt() != null ? owner.getCreatedAt() : LocalDateTime.now());
        owner.setUpdatedAt(LocalDateTime.now());

        ownerRepository.save(owner);
    }
    public void validateAndSavePayment(RentPaymentDTO rentPaymentDTO) {
        RentPayment rentPayment;
        if (rentPaymentDTO.getPaymentId() != null) {
            rentPayment = rentPaymentRepository.findById(rentPaymentDTO.getPaymentId())
                    .orElseThrow(() -> new IllegalArgumentException("Payment not found."));
        } else {
            rentPayment = new RentPayment();
            rentPayment.setCreatedAt(LocalDateTime.now());
        }

        Optional<Owner> optionalOwner = getOwnerById(rentPaymentDTO.getOwnerId());
        if (!ownerRepository.existsById(rentPaymentDTO.getOwnerId())) {
            throw new IllegalArgumentException("Owner not found.");
        }
        Owner owner = optionalOwner.get();
        List<RentPayment> existingPayments = rentPaymentRepository.findByOwnerId(rentPaymentDTO.getOwnerId());
        for (RentPayment payment : existingPayments) {
            if (rentPaymentDTO.getPaymentId() != null && rentPaymentDTO.getPaymentId().equals(payment.getId())) {
                continue;
            }
            if (rentPaymentDTO.getRentPeriodStart().isBefore(payment.getRentPeriodEnd())
                    && rentPaymentDTO.getRentPeriodEnd().isAfter(payment.getRentPeriodStart())) {
                throw new IllegalArgumentException("The rent period overlaps with an existing payment.");
            }
        }

        if (rentPaymentDTO.getPaymentId() != null) {
            boolean paidOnExists = existingPayments.stream()
                    .anyMatch(payment -> !payment.getId().equals(rentPaymentDTO.getPaymentId())
                            && payment.getPaidOn().equals(rentPaymentDTO.getPaidOn()));
            if (paidOnExists) {
                throw new IllegalArgumentException("A payment has already been made on the given 'Paid On' date.");
            }
        } else {
            boolean paidOnExists = existingPayments.stream()
                    .anyMatch(payment -> payment.getPaidOn().equals(rentPaymentDTO.getPaidOn()));
            if (paidOnExists) {
                throw new IllegalArgumentException("A payment has already been made on the given 'Paid On' date.");
            }
        }

        rentPayment.setOwner(owner);
        rentPayment.setRentPeriodStart(rentPaymentDTO.getRentPeriodStart());
        rentPayment.setRentPeriodEnd(rentPaymentDTO.getRentPeriodEnd());
        rentPayment.setPaidOn(rentPaymentDTO.getPaidOn());
        rentPayment.setAmount(rentPaymentDTO.getAmount());
        rentPayment.setUpdatedAt(LocalDateTime.now());

        rentPaymentRepository.save(rentPayment);
    }


    public Map<Owner, RentSummaryDTO> getPaymentsGroupedByOwner() {
        List<RentPayment> rentPayments = rentPaymentRepository.findByUserId(userService.getUserId());
        List<RentPaymentDTO> rentPaymentDTOs = rentPayments.stream()
                .map(RentPaymentDTO::new)
                .collect(Collectors.toList());

        Map<Owner, RentSummaryDTO> paymentsByOwner = rentPaymentDTOs.stream()
                .collect(Collectors.groupingBy(RentPaymentDTO::getOwner,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                payments -> {
                                    List<RentPaymentDTO> sortedPayments = payments.stream()
                                            .sorted(Comparator.comparing(RentPaymentDTO::getRentPeriodStart))
                                            .collect(Collectors.toList());
                                    return new RentSummaryDTO(sortedPayments, calculateTotalAmount(sortedPayments));
                                }
                        )
                ));

        Map<Owner, RentSummaryDTO> formattedPaymentsByOwner = paymentsByOwner.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            List<RentPaymentDTO> formattedPayments = entry.getValue().getPayments().stream()
                                    .map(payment -> {
                                        String formattedStartDate = formatterUtils.formatDateToCustomPattern(payment.getRentPeriodStart());
                                        String formattedEndDate = formatterUtils.formatDateToCustomPattern(payment.getRentPeriodEnd());
                                        String formattedPaidOn = formatterUtils.formatDate(payment.getPaidOn());
                                        String formattedAmount = formatterUtils.formatInIndianStyle(payment.getAmount());

                                        payment.setFormattedRentPeriodStart(formattedStartDate);
                                        payment.setFormattedRentPeriodEnd(formattedEndDate);
                                        payment.setFormattedPaidOn(formattedPaidOn);
                                        payment.setFormattedAmount(formattedAmount);

                                        return payment;
                                    })
                                    .collect(Collectors.toList());

                            String totalAmount = entry.getValue().getTotalAmount();
                            return new RentSummaryDTO(formattedPayments, totalAmount);
                        }
                ));

        return formattedPaymentsByOwner;
    }
    private String calculateTotalAmount(List<RentPaymentDTO> payments) {
        return formatterUtils.formatInIndianStyle(payments.stream()
                .mapToDouble(RentPaymentDTO::getAmount)
                .sum());
    }

    public void generateOwnersPdf(OutputStream outputStream, List<OwnerDTO> owners) {
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        document.add(new Paragraph("OWNERS REPORT")
                .setBold()
                .setFontSize(20)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(15));

        Table table = new Table(6);
        table.setWidth(UnitValue.createPercentValue(100));
        table.setAutoLayout();
        table.setHorizontalAlignment(HorizontalAlignment.CENTER);

        table.addHeaderCell(formatterUtils.createStyledCell("Name", 0));
        table.addHeaderCell(formatterUtils.createStyledCell("Phone Number", 0));
        table.addHeaderCell(formatterUtils.createStyledCell("Address", 0));
        table.addHeaderCell(formatterUtils.createStyledCell("Months", 0));
        table.addHeaderCell(formatterUtils.createStyledCell("Amount", 0));
        table.addHeaderCell(formatterUtils.createStyledCell("Date", 0));

        for (OwnerDTO owner : owners) {
            table.addCell(formatterUtils.createStyledCell(Character.toUpperCase(owner.getName().charAt(0)) + owner.getName().substring(1).toLowerCase(), 1));
            table.addCell(formatterUtils.createStyledCell(owner.getPhoneNumber(),1));
            table.addCell(formatterUtils.createStyledCell(owner.getAddress(), 1));
            table.addCell(formatterUtils.createStyledCell(owner.getAdvanceMonths(), 1));
            table.addCell(formatterUtils.createStyledCell(formatterUtils.formatInIndianStyle(owner.getAdvanceAmount()), 2));
            table.addCell(formatterUtils.createStyledCell(formatterUtils.formatDate(owner.getAdvanceDate()), 1));
        }
        document.add(table);
        document.add(new Paragraph("\n"));

        String explanation = formatterUtils.getTotalAdvance(owners);

        document.add(new Paragraph(explanation)
                .setFontSize(14)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10));
        document.close();
    }

    public boolean hasRentPaymentsForOwner(Long ownerId) {
            if (ownerId == null) {
                throw new IllegalArgumentException("Owner ID cannot be null");
            }
            return rentPaymentRepository.existsByOwner_Id(ownerId);
    }

    public void generateRentPaymentPdf(OutputStream outputStream, Map<Owner, RentSummaryDTO> paymentsByOwner) {
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        Map<Owner, String> ownerTotalPayments = paymentsByOwner.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getTotalAmount()));

        double grandTotal = ownerTotalPayments.values().stream()
                .mapToDouble(amount -> {
                    try {
                        String numericAmount = amount.replaceAll("[^0-9.]", "");
                        return Double.parseDouble(numericAmount);
                    } catch (NumberFormatException e) {
                        return 0.0;
                    }
                })
                .sum();

        document.add(new Paragraph("RENT PAYMENTS REPORT")
                .setBold()
                .setFontSize(20)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10));
        for (Map.Entry<Owner, RentSummaryDTO> entry : paymentsByOwner.entrySet()) {
            int serialNo = 1;
            Owner owner = entry.getKey();
            RentSummaryDTO rentSummary = entry.getValue();

            document.add(new Paragraph()
                    .add(new Text("Owner : " + owner.getName())
                            .setBold()
                            .setFontSize(12))
                    .add(new Text("  Phone : " + owner.getPhoneNumber())
                            .setBold()
                            .setFontSize(12))
                    .setMarginBottom(0)
                    .setHeight(30f)
                    .setMarginTop(5)
                    .setWidth(UnitValue.createPercentValue(90))
                    .setFontColor(new DeviceRgb(255,255,255))
                    .setBackgroundColor(new DeviceRgb(0, 100, 148))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setHorizontalAlignment(HorizontalAlignment.CENTER));

            Table table = new Table(4);
            table.setWidth(UnitValue.createPercentValue(90));
            table.setAutoLayout();
            table.setHorizontalAlignment(HorizontalAlignment.CENTER);

            table.addHeaderCell(formatterUtils.createStyledCell("S.No", 0));
            table.addHeaderCell(formatterUtils.createStyledCell("Rent Period", 0));
            table.addHeaderCell(formatterUtils.createStyledCell("PaidOn", 0));
            table.addHeaderCell(formatterUtils.createStyledCell("Amount", 0));
            double ownerTotal = 0;
            for (RentPaymentDTO payment : rentSummary.getPayments()) {
                table.addCell(formatterUtils.createStyledCell(String.valueOf(serialNo++), 1));
                table.addCell(formatterUtils.createStyledCell(formatterUtils.formatDateToCustomPattern(payment.getRentPeriodStart())+" - "+formatterUtils.formatDateToCustomPattern(payment.getRentPeriodEnd()), 1));
                table.addCell(formatterUtils.createStyledCell(formatterUtils.formatDate(payment.getPaidOn()), 1));
                table.addCell(formatterUtils.createStyledCell(formatterUtils.formatInIndianStyle(payment.getAmount()), 2));
                ownerTotal += payment.getAmount();
            }
            table.addCell(new Cell(1, 3)
                    .add(new Paragraph("Total Payment :"))
                    .setBackgroundColor(new DeviceRgb(241,248,233))
                    .setTextAlignment(TextAlignment.RIGHT).setPaddingRight(50)
                    .setBold());
            table.addCell(formatterUtils.createStyledCell(formatterUtils.formatInIndianStyle(ownerTotal), 2));

            document.add(table);
            document.add(new Paragraph("\n"));
        }

        Table summaryTable = new Table(2);
        summaryTable.setWidth(UnitValue.createPercentValue(70));
        summaryTable.setHorizontalAlignment(HorizontalAlignment.CENTER);
        Cell titleCell = new Cell(1, 2);
        titleCell.add(new Paragraph("OWNER WISE TOTALS").setTextAlignment(TextAlignment.CENTER).setBold());
        titleCell.setBackgroundColor(new DeviceRgb(0, 100, 148));
        titleCell.setFontColor(new DeviceRgb(255,255,255));
        titleCell.setPadding(5);
        summaryTable.addHeaderCell(titleCell);
        summaryTable.addHeaderCell(formatterUtils.createStyledCell("Owner", 0));
        summaryTable.addHeaderCell(formatterUtils.createStyledCell("Total Payment", 0));

        for (Map.Entry<Owner, String> entry : ownerTotalPayments.entrySet()) {
            summaryTable.addCell(formatterUtils.createStyledCell(String.valueOf(entry.getKey().getName()), 1));
            summaryTable.addCell(formatterUtils.createStyledCell(entry.getValue(), 2));
        }
        summaryTable.addCell(new Cell()
                .add(new Paragraph("Grand Total"))
                .setBackgroundColor(new DeviceRgb(241,248,233))
                .setTextAlignment(TextAlignment.CENTER)
                .setBold());
        summaryTable.addCell(formatterUtils.createStyledCell(formatterUtils.formatInIndianStyle(grandTotal), 3));

        document.add(summaryTable);
        document.close();
    }



}
