<%@ page session="true" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<!DOCTYPE html>
<html>
<head>
    <title>Manage Expenses</title>
</head>
<body>
<div class="container">
<div class="form-container">
<h2>Manage Expenses</h2>
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
<button type="button" onclick="loadReport()">Show Report</button>
</form>
</div>
<div id="manageReportContainer"></div>
</div>
</div>
</body>
</html>
