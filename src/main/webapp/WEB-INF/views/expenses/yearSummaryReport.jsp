<%@ page session="true" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<html>
<head>
    <title>Year summary Expenses</title>
</head>
<body>
<div class="container">
<div id="yearwise" style="background-color: #f9f9f9; box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1); font-family: Arial, sans-serif;margin-top:5px;">
<h1 class="h1report">YEARLY EXPENSE SUMMARY - ${year}
    <div class="report-buttons">
        <button id="downloadPdf" class="pdf-btn" onclick="downloadPdf('yearSummary|${year}')" ><img src="${pageContext.request.contextPath}/images/pdf.png" alt="PDF" /></button>
        <button id="printReport" class="print-btn" onclick="printReport()"><img src="${pageContext.request.contextPath}/images/print.png" alt="Print" /></button>
    </div>
</h1>
<table class="summaryTable">
    <thead>
        <tr style="height: 40px;background-color: #006494;color: white;">
            <th>Category</th>
            <th>JAN</th> <th>FEB</th> <th>MAR</th> <th>APR</th> <th>MAY</th>
            <th>JUN</th> <th>JUL</th> <th>AUG</th> <th>SEP</th> <th>OCT</th>
            <th>NOV</th> <th>DEC</th> <th>TOTAL</th> <th>AVG</th>
        </tr>
    </thead>
    <tbody>
        <c:forEach var="entry" items="${categorySums}">
            <tr>
                <td style="text-align: left;font-weight: bold;padding-left: 10px;">${entry.key}</td>
                <c:forEach begin="1" end="12" var="month">
                    <c:set var="amount" value="0" />
                    <c:forEach var="expense" items="${expenseReport}">
                        <c:if test="${expense.category eq entry.key && expense.month eq month}">
                            <c:set var="amount" value="${expense.actualAmountStr}" />
                        </c:if>
                    </c:forEach>
                    <td>${amount}</td>
                </c:forEach>
                <td><strong>${entry.value}</strong></td>
                <td><strong>${categoryAverages[entry.key]}</strong></td>
            </tr>
            </c:forEach>
            <tr style="height: 30px;background-color: #006494;color: white;">
                <td style="text-align: left;font-weight: bold;padding-left: 10px;"><strong>Total</strong></td>
                <c:forEach begin="1" end="12" var="month">
                    <td><strong>${monthlySums[month]}</strong></td>
                </c:forEach>
                <td><strong>${grandTotal}</strong></td>
                <td><strong>${totalAverage}</strong></td>
            </tr>
    </tbody>
</table>
</div>
</div>
</body>
</html>
