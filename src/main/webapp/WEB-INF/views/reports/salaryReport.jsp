<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<body>
<c:if test="${not empty salaries}">
<div id="reportContent">
    <h1 class="h1report">Salary Report
    <div class="report-buttons">
        <button id="downloadPdf" class="pdf-btn" data-report="sal" ><img src="${pageContext.request.contextPath}/images/pdf.png" alt="PDF" /></button>
        <button id="printReport" class="print-btn" data-report="sal"><img src="${pageContext.request.contextPath}/images/print.png" alt="Print" /></button>
    </div>
    </h1>

        <c:forEach var="entry" items="${salaries}">
            <c:set var="year" value="${entry.key}" />
            <c:set var="salariesList" value="${entry.value}" />
            <c:set var="yearTotal" value="${yearWiseTotals[year]}" />

            <div class="year-header">YEAR - ${year}</div>

            <table style="width: 100%; border-collapse: collapse; font-family: Arial, sans-serif;">
                <thead style="background-color: #E3F2FD; color: black; text-align: center;height: 25px; font-size: 16px;">
                    <tr>
                        <th class="second">Company</th>
                        <th class="second">Month</th>
                        <th class="second">Date Credited</th>
                        <th class="second">Salary Amount</th>
                        <th class="second" style="padding: 10px 15px;" rowspan="2">Action</th>
                    </tr>
                </thead>
                <tbody style="text-align: -webkit-center;text-transform: capitalize;">
                    <c:forEach var="salary" items="${salariesList}">
                        <tr>
                            <td>${salary.companyName}</td>
                            <td>${salary.monthName}</td>
                            <td>${salary.formattedDateCredited}</td>
                            <td style="text-align: right; padding-right: 5px;">${salary.formattedSalaryAmount}</td>
                            <td>
                                <a href="javascript:void(0);"
                                   onclick="loadEditPageContent('/api/work/addSalary?action=edit&id=${salary.salaryId}', '#page-content')"
                                   style="text-decoration: none;">
                                    <img src="${pageContext.request.contextPath}/images/edit-icon.png" alt="Edit" style="width: 20px; height: 20px; margin-right: 20px;margin-left: 15px;">
                                </a>
                                <a href="javascript:void(0);" class="delete-btn" data-report-type="salaryReport" data-id="${salary.salaryId}" onclick="deleteEntity(this,'salary','/api/work/deleteSalary')">
                                    <img src="${pageContext.request.contextPath}/images/delete-icon.png" alt="Delete" style="width: 20px; height: 20px;">
                                </a>
                            </td>
                        </tr>
                    </c:forEach>
                    <tr style="font-weight: bold; background-color: #F1F8E9;height: 30px;">
                        <td colspan="3" style="text-align: right;">Total for Year:</td>
                        <td style="text-align: right; padding-right: 5px;">${yearTotal}</td>
                        <td></td>
                    </tr>
                </tbody>
            </table>
            <br>
        </c:forEach>

        <br>
        <div class="summary">
            <h2 class="year-header" style="width: 50%;margin: auto;">Year-Wise Totals</h2>
            <table style="width: 50%; border-collapse: collapse; font-family: Arial, sans-serif; margin: auto;">
                <thead style="background-color: #E3F2FD; color: black; text-align: center; font-size: 16px;">
                    <tr style="height: 30px;">
                        <th class="second">Year</th>
                        <th class="second">Total Salary</th>
                    </tr>
                </thead>
                <tbody style="text-align: center;">
                    <c:forEach var="yearEntry" items="${yearWiseTotals}">
                        <tr style="height: 30px;">
                            <td>${yearEntry.key}</td>
                            <td style="text-align: right; padding-right: 15px;">${yearEntry.value}</td>
                        </tr>
                    </c:forEach>
                    <tr style="font-weight: bold; background-color: #F1F8E9;height: 30px;">
                        <td>Grand Total:</td>
                        <td style="text-align: right; padding-right: 15px;">${totalSum}</td>
                    </tr>
                </tbody>
            </table>
        </div>
</div>
    </c:if>
    <c:if test="${empty salaries}">
        <h2 style="text-align: center; color: #333;padding-bottom: 10px;">No salary Data Available.</h2>
    </c:if>
</body>
