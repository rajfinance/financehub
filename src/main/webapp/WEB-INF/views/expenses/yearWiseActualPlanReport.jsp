<%@ page session="true" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<!DOCTYPE html>
<html>
<head>
    <title>Year Wise Expenses</title>
</head>
<body>
<div class="container">
<div id="yearwise" style="background-color: #f9f9f9; box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1); font-family: Arial, sans-serif;margin-top:5px;">
<h2>Plan & Actual Expenses For Year - ${year}</h2>
<c:set var="months" value="JAN,FEB,MAR,APR,MAY,JUN,JUL,AUG,SEP,OCT,NOV,DEC" />
<c:set var="monthArray" value="${fn:split(months, ',')}" />
<table class="myTable">
<thead>
    <tr style="height: 35px;background-color: #006494;color: white;">
        <th rowspan="2" style="font-size: 17px;">Category</th>
        <c:forEach var="monthName" items="${monthArray}">
            <th colspan="2" style="font-size: 17px;">${monthName}</th>
        </c:forEach>
    </tr>
    <tr style="height: 25px;background-color: #006494;color: white;">
        <c:forEach var="monthName" items="${monthArray}">
            <th>Plan</th>
            <th>Actual</th>
        </c:forEach>
    </tr>
</thead>
<tbody>
<c:set var="currentCategory" value="" />
<c:forEach var="data" items="${reportData}">
    <c:if test="${data.category != currentCategory}">
    <tr>
        <td style="text-align:left;font-weight:bold;padding-left: 10px;">${data.category}</td>
        <c:forEach var="monthIndex" begin="1" end="12">
            <c:set var="planAmount" value="0" />
            <c:set var="actualAmount" value="0" />
            <c:forEach var="innerData" items="${reportData}">
                <c:if test="${innerData.category == data.category && innerData.month == monthIndex}">
                    <c:set var="planAmount" value="${innerData.planAmount}" />
                    <c:set var="actualAmount" value="${innerData.actualAmount}" />
                </c:if>
            </c:forEach>
        <td><fmt:formatNumber value="${planAmount}" type="number" groupingUsed="true" /></td>
        <td><fmt:formatNumber value="${actualAmount}" type="number" groupingUsed="true" /></td>
        </c:forEach>
    </tr>
    <c:set var="currentCategory" value="${data.category}" />
    </c:if>
</c:forEach>

<tr style="height: 30px;background-color: #006494;color: white;">
    <td style="text-align:left;font-weight:bold;padding-left: 10px;font-size: 16px;">Total</td>
    <c:forEach var="monthIndex" begin="1" end="12">
        <td style="text-align:right;font-weight:bold;"><fmt:formatNumber value="${monthlyPlanTotalMap[monthIndex]}" type="number" groupingUsed="true" /></td>
        <td style="text-align:right;font-weight:bold;"><fmt:formatNumber value="${monthlyActualTotalMap[monthIndex]}" type="number" groupingUsed="true" /></td>
    </c:forEach>
</tr>
</tr>
</tbody>
</table>
</div>
</div>
</body>
</html>