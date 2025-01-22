<%@ page session="true" %>
<html lang="en">
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<body>
<div class="form-container">
    <h1><c:choose>
            <c:when test="${action == 'edit'}">Edit Experience</c:when>
            <c:otherwise>Add Experience</c:otherwise>
        </c:choose></h1>

    <c:if test="${not empty successMessage}">
        <div style="color: green; text-align: center; font-weight: bold; margin-left: 200px;">
            ${successMessage}
        </div>
    </c:if>

    <c:if test="${not empty error}">
        <div style="color: red; text-align: center; font-weight: bold; margin-left: 100px;">
            ${error} &nbsp;<a href="/api/work/workReport" style="color: blue;">View Experience</a>
        </div>
    </c:if>

    <form id="addExperience" action="/api/work/addExperience" method="post" onsubmit="submitForm(event)">
        <c:if test="${action == 'edit'}">
            <input type="hidden" name="companyId" value="${company.companyId}">
        </c:if>

        <div>
            <label for="companyName">Company Name<span class="mandatory">*</span>:</label>
            <input type="text" id="companyName" name="companyName" value="${company.companyName}" required>
        </div>

        <div>
            <label for="clientName">Client Name<span class="mandatory">*</span>:</label>
            <input type="text" id="clientName" name="clientName" value="${company.clientName}" required>
        </div>

        <div>
            <label for="projectName">Project Name<span class="mandatory">*</span>:</label>
            <input type="text" id="projectName" name="projectName" value="${company.projectName}" required>
        </div>

        <div>
            <label for="fromDate">Experience From (MM/DD/YYYY)<span class="mandatory">*</span>:</label>
            <input type="date" id="fromDate" name="fromDate" value="${company.fromDate}" required max="9999-12-31" class="styled-date">
        </div>

        <div>
            <label>Currently Employed <input type="checkbox" id="currentlyEmployed" name="currentlyEmployed" onclick="toggleToDate()" ${company.currentlyEmployed ? 'checked' : ''} ></label>
            <label></label>
        </div>

        <div>
            <label for="toDate">Experience To (MM/DD/YYYY):</label>
            <input type="date" id="toDate" name="toDate" value="${company.toDate != null ? company.toDate : ''}" ${company.currentlyEmployed ? 'disabled' : ''} max="9999-12-31">
        </div>

        <button type="submit"><c:choose>
            <c:when test="${action == 'edit'}">Update Experience</c:when>
            <c:otherwise>Add Experience</c:otherwise>
        </c:choose></button>
    </form>
</div>
</body>
</html>
