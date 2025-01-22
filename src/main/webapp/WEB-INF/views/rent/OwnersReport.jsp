<%@ page import="java.util.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<body>
<c:if test="${not empty owners}">
<div class="report-buttons">
    <button id="downloadPdf" class="pdf-btn" data-report="sal" ><img src="${pageContext.request.contextPath}/images/pdf.png" alt="PDF" /></button>
    <button id="printReport" class="print-btn" data-report="sal"><img src="${pageContext.request.contextPath}/images/print.png" alt="Print" /></button>
</div>
<div id="reportContent" style="width:100%">
    <h1>Owners Report</h1>
    <table style="width: 100%; border-collapse: collapse; font-family: Arial, sans-serif;">
        <thead>
            <tr>
                <th rowspan="2">Name</th>
                <th rowspan="2">Phone Number</th>
                <th rowspan="2">Address</th>
                <th colspan="3">Advance</th>
                <th rowspan="2">Action</th>
            </tr>
            <tr>
                <th>Months</th>
                <th>Amount</th>
                <th>Date</th>
            </tr>
        </thead>
        <tbody style="text-align: -webkit-center;text-transform: capitalize;">
            <c:forEach var="owner" items="${owners}">
                <tr>
                    <td>${owner.name}</td>
                    <td>${owner.phoneNumber}</td>
                    <td style="max-width:250px;">${owner.address}</td>
                    <td>${owner.advanceMonths}</td>
                    <td>${owner.advanceAmount}</td>
                    <td>${owner.formattedAdvanceDate}</td>
                    <td>
                    <a href="javascript:void(0);"
                        onclick="loadEditPageContent('/api/rent/owners/add?id=${owner.ownerId}', '#page-content')"
                        style="text-decoration: none;margin-right:10px;">
                        <img src="${pageContext.request.contextPath}/images/edit-icon.png" alt="Edit" style="width: 20px; height: 20px; margin-right: 20px;margin-left: 15px;">
                    </a>
                    <a href="javascript:void(0);" class="delete-btn" data-report-type="ownersReport" data-id=${owner.ownerId} onclick="deleteEntity(this,'Owner','/api/rent/deleteOwner')">
                        <img src="${pageContext.request.contextPath}/images/delete-icon.png" alt="Delete" style="width: 20px; height: 20px;">
                    </a>
                    </td>
                </tr>
            </c:forEach>
        </tbody>
    </table>
    <h2 style="text-align: center; color: #333;"> Total Advance Amount : ${totalAdvance}</h2>
</div>
</c:if>
<c:if test="${empty owners}">
        <h2 style="text-align: center; color: #333;padding-bottom: 10px;">No Owners Available.</h2>
</c:if>
</body>