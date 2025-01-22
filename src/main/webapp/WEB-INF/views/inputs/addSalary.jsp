<%@ page session="true" %>
<html lang="en">
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<body>
<div class="form-container">
    <h1><c:choose>
            <c:when test="${action == 'edit'}">Edit Salary</c:when>
            <c:otherwise>Add Salary</c:otherwise>
        </c:choose></h1>

    <c:if test="${not empty successMessage}">
        <div style="color: green; text-align: center; font-weight: bold; margin-left: 280px;">
            ${successMessage}
        </div>
    </c:if>

    <form id="addSalaryForm" action="/api/work/addSalary" method="post" onsubmit="submitForm(event)">
        <c:if test="${action == 'edit'}">
            <input type="hidden" name="salaryId" value="${salary.salaryId}">
        </c:if>

        <div>
            <label for="companyName">Company Name<span class="mandatory">*</span>:</label>
            <select id="companyId" name="companyId" required>
                <option value="">Select Company</option>
                <c:forEach var="company" items="${companies}">
                    <option value="${company.companyId}" <c:if test="${company.companyId == salary.companyId}">selected</c:if>>${company.companyName}</option>
                </c:forEach>
                <c:if test="${empty companies}">
                    <option value="">No companies available</option>
                </c:if>
            </select>
        </div>

        <div>
            <label for="month">Month<span class="mandatory">*</span>:</label>
            <select id="month" name="month" required>
                <option value="">Select Month</option>
                <c:forEach var="month" items="${monthAbbreviations}" varStatus="status">
                    <option value="${status.index + 1}" ${status.index + 1 == salary.month ? "selected" : ""}>
                        ${month}
                    </option>
                </c:forEach>
            </select>
        </div>

        <div>
            <label for="year">Year<span class="mandatory">*</span>:</label>
            <input type="number" id="year" name="year" min="2000" max="2100" value="${salary.year}" required>
        </div>

        <div>
            <label for="dateCredited">Date Credited<span class="mandatory">*</span>:</label>
            <input type="date" id="dateCredited" name="dateCredited" value="${salary.dateCredited}" required max="9999-12-31">
        </div>

        <div>
            <label for="salaryAmount">Salary Amount<span class="mandatory">*</span>:</label>
            <input type="number" id="salaryAmount" name="salaryAmount"
                   value="${salary.salaryAmount != null ? salary.salaryAmount : ''}"
                   step="0.01" required>
        </div>

        <button type="submit"><c:choose>
            <c:when test="${action == 'edit'}">Update Salary</c:when>
            <c:otherwise>Add Salary</c:otherwise>
        </c:choose></button>
    </form>
</div>
</body>
</html>
