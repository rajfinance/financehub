<%@ page session="true" %>
<html lang="en">
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<body>
<c:if test="${not empty companies}">

    <div id="reportContent">
        <h1 class="h1report">Total Experience
            <div class="report-buttons">
                <button id="downloadPdf" class="pdf-btn" data-report="exp">
                    <img src="${pageContext.request.contextPath}/images/pdf.png" alt="PDF" />
                </button>
                <button id="printReport" class="print-btn" data-report="exp">
                    <img src="${pageContext.request.contextPath}/images/print.png" alt="Print" />
                </button>
            </div>
        </h1>
        <table style="width: 100%; margin: 10px auto; border-collapse: collapse; font-family: Arial, sans-serif;">
            <thead style="background-color: #006494; color: white; text-align: left; font-size: 16px;">
                <tr>
                    <th style="padding: 10px 15px;">Company Name</th>
                    <th style="padding: 10px 15px;">Client Name</th>
                    <th style="padding: 10px 15px;">Project Name</th>
                    <th style="padding: 10px 15px;">From Date</th>
                    <th style="padding: 10px 15px;">To Date</th>
                    <th style="padding: 10px 15px;" rowspan="2">Action</th>
                </tr>
            </thead>
            <tbody style="font-size: 14px; color: #555;text-transform: capitalize;">
                <c:if test="${not empty companies}">
                    <c:forEach var="company" items="${companies}">
                        <tr style="border-bottom: 1px solid #ddd;">
                            <td style="padding: 8px 15px;">${company.companyName}</td>
                            <td style="padding: 8px 15px;">${company.clientName}</td>
                            <td style="padding: 8px 15px;">${company.projectName}</td>
                            <td style="padding: 8px 15px;">${company.formattedFromDate}</td>
                            <td style="padding: 8px 15px;">
                                ${company.formattedToDate != null ? company.formattedToDate : "Currently Employed"}
                            </td>
                            <td>
                                <a href="javascript:void(0);"
                                   onclick="loadEditPageContent('/api/work/addExperience?action=edit&id=${company.companyId}')"
                                   style="text-decoration: none;">
                                    <img src="${pageContext.request.contextPath}/images/edit-icon.png" alt="Edit" style="width: 20px; height: 20px; margin-right: 20px;margin-left: 15px;">
                                </a>
                                <a href="javascript:void(0);" class="delete-btn" data-report-type="expReport" data-id="${company.companyId}" onclick="deleteEntity(this,'company','/api/work/deleteExperience')">
                                    <img src="${pageContext.request.contextPath}/images/delete-icon.png" alt="Delete" style="width: 20px; height: 20px;">
                                </a>
                            </td>
                        </tr>
                    </c:forEach>
                </c:if>
                <c:if test="${empty companies}">
                    <tr>
                        <td colspan="6" style="text-align: center; padding: 15px; font-style: italic; color: #777;">
                            No companies found.
                        </td>
                    </tr>
                </c:if>
            </tbody>
        </table>
        <h2 style="text-align: center; color: #333;">${totalExp}</h2>
    </div>
</c:if>
<c:if test="${empty companies}">
        <h2 style="text-align: center; color: #333;padding-bottom: 10px;">No Companies Data Available.</h2>
</c:if>
</body>