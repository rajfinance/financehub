<%@ page import="java.util.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<body>
<c:if test="${not empty payments}">
<div id="reportContent">
    <h1 class="h1report">Rent Payments Report
    <div class="report-buttons">
        <button id="downloadPdf" class="pdf-btn" data-report="sal" ><img src="${pageContext.request.contextPath}/images/pdf.png" alt="PDF" /></button>
        <button id="printReport" class="print-btn" data-report="sal"><img src="${pageContext.request.contextPath}/images/print.png" alt="Print" /></button>
    </div>
    </h1>
    <c:forEach var="owner" items="${payments}">
    <table style="width: 100%; border-collapse: collapse; font-family: Arial, sans-serif;">
        <thead>
            <tr>
                <th colspan="5">Owner Name : ${owner.key.name} &nbsp;&nbsp;  Phone Number : ${owner.key.phoneNumber}</th>
            </tr>
            <tr>
                <th class="second">S.No</th>
                <th class="second">Rent Period</th>
                <th class="second">PaidOn</th>
                <th class="second">Amount</th>
                <th class="second">Action</th>
            </tr>
        </thead>
        <tbody style="text-align: -webkit-center;text-transform: capitalize;">
            <c:forEach var="payment" items="${owner.value.payments}" varStatus="status">
                <tr>
                    <td>${status.index + 1}</td>
                    <td>${payment.formattedRentPeriodStart} - ${payment.formattedRentPeriodEnd}</td>
                    <td>${payment.formattedPaidOn}</td>
                    <td style="text-align: right; padding-right: 5px;">${payment.formattedAmount}</td>
                    <td>
                    <a href="javascript:void(0);"
                        onclick="loadEditPageContent('/api/rent/payments/add?id=${payment.id}', '#page-content')"
                        style="text-decoration: none;">
                        <img src="${pageContext.request.contextPath}/images/edit-icon.png" alt="Edit" style="width: 20px; height: 20px; margin-right: 20px;margin-left: 15px;">
                    </a>
                    <a href="javascript:void(0);" class="delete-btn" data-report-type="rentPaymentReport" data-id=${payment.id} onclick="deleteEntity(this,'Payment','/api/rent/deleteRentPayment')">
                        <img src="${pageContext.request.contextPath}/images/delete-icon.png" alt="Delete" style="width: 20px; height: 20px;">
                    </a>
                    </td>
                </tr>
            </c:forEach>
            <tr style="font-weight: bold; background-color: #F1F8E9;height: 30px;">
                <td style="text-align: right;">Total Period : </td>
                <td style="text-align: center;">${owner.value.totalPeriod}</td>
                <td style="text-align: right;">Total Paid : </td>
                <td style="text-align: right; padding-right: 5px;">${owner.value.totalAmount}</td><td></td>
            </tr>
        </tbody>
    </table>
    <br>
    </c:forEach>
<div class="summary">
            <h2 class="year-header" style="width: 60%;margin: auto;">Owner Wise Totals</h2>
            <table style="width: 60%; border-collapse: collapse; font-family: Arial, sans-serif; margin: auto;">
                <thead style="background-color: #E3F2FD; color: black; text-align: center; font-size: 16px;">
                    <tr style="height: 30px;">
                        <th class="second">Owner</th>
                        <th class="second">Total Period
                        <th class="second">Total Rent Paid</th>
                    </tr>
                </thead>
                <tbody style="text-align: center;">
                    <c:forEach var="ownerEntry" items="${ownerTotalPayments}">
                        <tr style="height: 30px;">
                            <td>${ownerEntry.key.name}</td>
                            <td>${fn:split(ownerEntry.value, '&')[0]}</td>
                            <td style="text-align: right; padding-right: 15px;">${fn:split(ownerEntry.value, '&')[1]}</td>
                        </tr>
                    </c:forEach>
                    <tr style="font-weight: bold; background-color: #F1F8E9;height: 30px;">
                        <td>Grand Total:</td>
                        <td>${fn:split(grandTotal,'&')[0]}</td>
                        <td style="text-align: right; padding-right: 15px;">${fn:split(grandTotal,'&')[1]}</td>
                    </tr>
                </tbody>
            </table>
        </div>
</div>
 </c:if>
 <c:if test="${empty payments}">
         <h2 style="text-align: center; color: #333;padding-bottom: 10px;">No Rent Payments Available.</h2>
 </c:if>
</body>