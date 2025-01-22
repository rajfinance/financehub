package com.financehub.dtos;

import com.financehub.entities.Salary;
import lombok.Data;
import java.time.LocalDate;

@Data
public class SalaryDTO {
    private Long salaryId;
    private Long companyId;
    private String companyName;
    private int month;
    private String monthName;
    private int year;
    private LocalDate dateCredited;
    private Double salaryAmount;
    private String formattedDateCredited;
    private String formattedSalaryAmount;

    public SalaryDTO(Salary salary) {
        if(salary !=null) {
            this.salaryId= salary.getId();
            this.companyId = salary.getCompany().getId();
            this.companyName = salary.getCompany().getCompanyName();
            this.year = salary.getSalaryYear();
            this.month = salary.getSalaryMonth();
            this.dateCredited = salary.getCreditDate();
            this.salaryAmount = salary.getSalaryAmount();
        }else {
            this.salaryId=null;
            this.companyId = null;
            this.companyName = "";
            this.year = 0;
            this.month = 0;
            this.monthName="";
            this.formattedSalaryAmount="";
            this.formattedDateCredited="";
            this.dateCredited = null;
            this.salaryAmount = null;
        }
    }
}
