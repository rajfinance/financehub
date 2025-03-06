<%@ page import="java.util.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<body>
<c:if test="${not empty reports}">
<div id="reportContent">
    <h2>Manage Expenses For Year - ${year}</h2>
    <table style="width: 100%; border-collapse: collapse; font-family: Arial, sans-serif;">
        <thead>
            <tr>
                <th>Month</th>
                <th>Plan Amount</th>
                <th>Action</th>
                <th>Actual Amount</th>
                <th>Action</th>
            </tr>
        </thead>
        <tbody style="text-align: -webkit-center;text-transform: capitalize;">
            <c:forEach var="report" items="${reports}">
                <tr>
                    <td>${report.monthStr}</td>
                    <td>${report.planAmount}</td>
                    <td>
                    <a href="javascript:void(0);"
                        onclick="loadEditPageContent('/api/expenses/add?id=${report.id}&type=plan', '#page-content')"
                        style="text-decoration: none;margin-right:10px;">
                        <img src="${pageContext.request.contextPath}/images/edit-icon.png" alt="Edit" style="width: 20px; height: 20px; margin-right: 20px;margin-left: 15px;">
                    </a>
                    <a href="javascript:void(0);" class="delete-btn" data-report-type="manageReport_${year}_ReportContainer" data-id=${report.id} onclick="deleteEntity(this,'Plan Amount','/api/expenses/deleteAmount')">
                        <img src="${pageContext.request.contextPath}/images/delete-icon.png" alt="Delete" style="width: 20px; height: 20px;">
                    </a>
                    </td>
                    <td>${report.actualAmount}</td>
                    <td>
                    <a href="javascript:void(0);"
                        onclick="loadEditPageContent('/api/expenses/add?id=${report.id}&type=actual', '#page-content')"
                        style="text-decoration: none;margin-right:10px;">
                        <img src="${pageContext.request.contextPath}/images/edit-icon.png" alt="Edit" style="width: 20px; height: 20px; margin-right: 20px;margin-left: 15px;">
                    </a>
                    <a href="javascript:void(0);" class="delete-btn" data-report-type="manageReport_${year}_ReportContainer" data-id=${report.id} onclick="deleteEntity(this,'Actual Amount','/api/expenses/deleteAmount')">
                        <img src="${pageContext.request.contextPath}/images/delete-icon.png" alt="Delete" style="width: 20px; height: 20px;">
                    </a>
                    </td>
                </tr>
            </c:forEach>
        </tbody>
    </table>
</div>
</c:if>
<c:if test="${empty reports}">
        <h2 style="text-align: center; color: #333;padding-bottom: 10px;">No Expenses Available.</h2>
</c:if>
<br><br>
</body>