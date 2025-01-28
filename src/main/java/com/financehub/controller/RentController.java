package com.financehub.controller;

import com.financehub.dtos.CompanyDTO;
import com.financehub.dtos.OwnerDTO;
import com.financehub.dtos.RentPaymentDTO;
import com.financehub.dtos.RentSummaryDTO;
import com.financehub.entities.Owner;
import com.financehub.entities.RentPayment;
import com.financehub.services.RentalService;
import com.financehub.services.UserService;
import com.financehub.utils.FormatterUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Period;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/api/rent")
public class RentController {
    @Autowired
    private RentalService rentalService;
    @Autowired
    private FormatterUtils formatterUtils;

    @GetMapping("/owners/add")
    public String showAddOwnerPage(@RequestParam(value = "id", required = false) Long id, Model model) {
        if (id != null) {
            OwnerDTO ownerDTO = rentalService.getOwnerDTOById(id);
            if (ownerDTO != null) {
                model.addAttribute("owner", ownerDTO);
            } else {
                model.addAttribute("errors", "Owner not found");
            }
        }else {
            model.addAttribute("owner", new OwnerDTO(new Owner()));
        }
        return "rent/addOwner";
    }
    @GetMapping("/payments/add")
    public String showAddPaymentPage(@RequestParam(value = "id", required = false) Long paymentId, Model model) {
        if (paymentId != null) {
            RentPaymentDTO paymentDTO = rentalService.getPaymentById(paymentId);
            model.addAttribute("payment", paymentDTO);
        } else {
            model.addAttribute("payment", null);
        }
        List<OwnerDTO> owners = rentalService.getOwnersByUserId();
        model.addAttribute("owners", owners);

        return "rent/addPayment";
    }
    @GetMapping("/reports")
    public String showReportsPage() {
        return "rent/rentReports";
    }

    @PostMapping("/addOwners")
    public String addOwner(@Valid @ModelAttribute("owner") OwnerDTO ownerDTO,
                           BindingResult bindingResult, RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errors", bindingResult.getAllErrors());
            return "redirect:/api/rent/owners/add";
        }

        try {
            rentalService.saveOwner(ownerDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Owner " + ownerDTO.getName() + " Details Saved Successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errors", List.of(e.getMessage()));
        }
        return "redirect:/api/rent/owners/add";
    }
    @PostMapping("/addPayments")
    public String addPayment(@Valid @ModelAttribute("rentPaymentDTO") RentPaymentDTO rentPaymentDTO,
                             BindingResult bindingResult, RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "There are errors in the form.");
            return "redirect:payments/add";
        }

        try {
            rentalService.validateAndSavePayment(rentPaymentDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Payment saved successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", List.of(e.getMessage()));
            return "redirect:payments/add";
        }

        return "redirect:payments/add";
    }

    @GetMapping("/ownersReport")
    public String getOwnersReport(Model model) {
        List<OwnerDTO> owners = rentalService.getOwnersByUserId();
        double totalAdvanceAmount = owners.stream()
                .mapToDouble(OwnerDTO::getAdvanceAmount)
                .sum();
        model.addAttribute("owners", owners);
        model.addAttribute("totalAdvance", formatterUtils.formatInIndianStyle(totalAdvanceAmount));
        return "rent/OwnersReport";
    }

    @GetMapping("/rentPaymentReport")
    public String getRentPaymentReport(Model model) {
        Map<Owner, RentSummaryDTO> paymentsByOwner = rentalService.getPaymentsGroupedByOwner();

        Comparator<Owner> ownerComparator = Comparator.comparing(Owner::getAdvanceDate);

        Map<Owner, String> ownerTotalPayments = paymentsByOwner.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().getTotalPeriod()+"&"+entry.getValue().getTotalAmount(),
                        (e1, e2) -> e1,
                        () -> new TreeMap<>(ownerComparator)
                ));

        double grandTotal = ownerTotalPayments.values().stream()
                .mapToDouble(amount -> {
                    try {
                        String numericAmount = amount.split("&")[1].replaceAll("[^0-9.]", "");
                        return Double.parseDouble(numericAmount);
                    } catch (NumberFormatException e) {
                        return 0.0;
                    }
                })
                .sum();

        Period grandTotalPeriod = ownerTotalPayments.values().stream()
                .map(value -> {
                    return formatterUtils.parsePeriod(value.split("&")[0]);
                })
                .reduce(Period.ZERO, Period::plus);

        String grandTotalPeriodstring = formatterUtils.formatPeriod(grandTotalPeriod);

        model.addAttribute("payments", paymentsByOwner);
        model.addAttribute("ownerTotalPayments", ownerTotalPayments);
        model.addAttribute("grandTotal",grandTotalPeriodstring+"&"+formatterUtils.formatInIndianStyle(grandTotal));
        return "rent/PaymentsReport";
    }
    @DeleteMapping("/deleteOwner")
    public ResponseEntity<String> deleteOwner(@RequestParam("id") Long id) {
        OwnerDTO owner = rentalService.getOwnerDTOById(id);
        if (owner == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("error");
        }
        boolean hasPayments = rentalService.hasRentPaymentsForOwner(id);
        if (hasPayments) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Please delete Rent Payment entries for this Owner first.");
        }
        rentalService.deleteOwner(id);
        return ResponseEntity.ok("success");
    }
    @DeleteMapping("/deleteRentPayment")
    public ResponseEntity<String> deleteRentPayment(@RequestParam("id") Long id) {
        RentPaymentDTO payment = rentalService.getPaymentById(id);
        if (payment == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("error");
        }
        rentalService.deleteRentPayment(id);
        return ResponseEntity.ok("success");
    }

}
