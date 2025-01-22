package com.financehub.dtos;

import com.financehub.entities.Company;
import lombok.Data;

import java.time.LocalDate;
@Data
public class CompanyDTO {
    private Long companyId;
    private String companyName;
    private String clientName;
    private String projectName;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String formattedFromDate;
    private String formattedToDate;
    private boolean currentlyEmployed;

    public CompanyDTO(Company company) {

        if (company != null) {
            this.companyId = company.getId();
            this.companyName = company.getCompanyName();
            this.clientName = company.getClient();
            this.projectName = company.getProject();
            this.fromDate = company.getExperienceFrom();
            this.toDate = company.getExperienceTo();
            this.currentlyEmployed = company.getIsCurrentCompany() != null && company.getIsCurrentCompany();
        } else {
            this.companyId=null;
            this.companyName = "";
            this.clientName = "";
            this.projectName = "";
            this.fromDate = null;
            this.toDate = null;
            this.formattedFromDate="";
            this.formattedToDate="";
            this.currentlyEmployed = false;
        }
    }
}
