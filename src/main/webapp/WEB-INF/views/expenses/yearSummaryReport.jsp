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
<div id="yearwise" style="background-color: #f9f9f9; box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1); font-family: Arial, sans-serif;margin-top:20px;">
<h2>Summary Of Expenses For Year - ${year}</h2>
<table class="summaryTable">
    <thead>
        <tr>
            <th>Category</th>
            <th>JAN</th> <th>FEB</th> <th>MAR</th> <th>APR</th> <th>MAY</th>
            <th>JUN</th> <th>JUL</th> <th>AUG</th> <th>SEP</th> <th>OCT</th>
            <th>NOV</th> <th>DEC</th> <th>TOTAL</th> <th>AVERAGE</th>
        </tr>
    </thead>
    <tbody>
        <c:forEach var="entry" items="${categorySums}">
            <tr>
                <td>${entry.key}</td>
                <c:forEach begin="1" end="12" var="month">
                    <c:set var="amount" value="0" />
                    <c:forEach var="expense" items="${expenseReport}">
                        <c:if test="${expense.category eq entry.key && expense.month eq month}">
                            <c:set var="amount" value="${expense.actualAmountStr}" />
                        </c:if>
                    </c:forEach>
                    <td>${amount}</td>
                </c:forEach>
                <td>${entry.value}</td>
                <td>${categoryAverages[entry.key]}</td>
            </tr>
            </c:forEach>
            <tr class="total-row">
                <td><strong>Total</strong></td>
                <c:forEach begin="1" end="12" var="month">
                    <td><strong>${monthlySums[month]}</strong></td>
                </c:forEach>
                <td colspan="2"></td>
            </tr>
    </tbody>
</table>
</div>
</div>
</body>
</html>
