<%@ page session="true" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<!DOCTYPE html>
<html>
<head>
    <title>YearWise Plan Expenses</title>
</head>
<body>
<div class="container">
<div class="form-container">
<h2>Year Wise Expenses</h2>
<form method="get">
<div class="input-group">
    <label for="year">Select Year<span class="mandatory">*</span>:</label></label>
    <select id="year" name="year" required>
        <option value="">--Select Year--</option>
            <c:forEach var="yr" items="${years}">
                <option value="${yr}">${yr}</option>
            </c:forEach>
    </select>
</div><br>
<div class="button-group">
    <button type="button" onclick="setActive(this); loadReport('year','/api/expenses/manageReport','ReportContainer')">Manage</button>
    <button type="button" onclick="setActive(this); loadReport('year','/api/expenses/yearWiseActualPlanReport','ReportContainer')">Plan vs Actual</button>
    <button type="button" onclick="setActive(this); loadReport('year','/api/expenses/yearSummaryReport','ReportContainer')">Summary</button>
</div>
</form>
</div>
<div id="ReportContainer"></div>
</div>
</body>
</html>
