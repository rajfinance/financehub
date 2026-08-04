package com.financehub.dtos;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class ExperienceCompanyGroupDTO {
    private String groupKey;
    private String companyName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String formattedStartDate;
    private String formattedEndDate;
    private String experienceLabel;
    private boolean currentlyEmployed;
    private List<CompanyDTO> entries = new ArrayList<>();
}
