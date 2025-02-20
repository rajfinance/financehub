<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<script src="/js/validations.js"></script>
<script src="/js/formUtils.js"></script>
<script src="/js/printWorkPdf.js"></script>
<script>
    window.onload = function() {
        if ('${not empty sessionScope.username}') {
            loadPage('home');
        }
    };
</script>
<div class="menu-bar">
    <ul class="menu-items">
        <c:if test="${empty sessionScope.username}">
            <li><a href="/">Home</a></li>
            <li><a href="/services">Services</a></li>
            <li><a href="/contact">Contact</a></li>
        </c:if>
        <c:if test="${not empty sessionScope.username}">
            <li><a href="#" onclick="loadPage('home')">Home</a></li>
            <li><a href="#" onclick="loadPage('professional')">Professional</a></li>
            <li><a href="#" onclick="loadPage('rentals')">Rentals</a></li>
            <li><a href="#" onclick="loadPage('expenses')">Expenses</a></li>
            <li><a href="#" onclick="loadPage('loans')">Loans</a></li>
            <li><a href="#" onclick="loadPage('investments')">Investments</a></li>
            <li><a href="/api/logout">Logout</a></li>
        </c:if>
    </ul>
</div>
<c:if test="${not empty sessionScope.username}">
<div class="main-content">
<div id="username" style="display: none;">
        <c:out value="${username}" />
</div>
<div class="sidebar" id="professional" style="display: none;">
    <ul>
        <li><a href="javascript:void(0);" onclick="loadContent('/api/work/addExperience?action=add')">Add Experience</a></li>
        <li><a href="javascript:void(0);" onclick="loadContent('/api/work/addSalary?action=add')">Add Salary</a></li>
        <li><a href="javascript:void(0);" onclick="loadContent('/api/work/workReport')">Report</a></li>
    </ul>
</div>
<div class="sidebar" id="expenses" style="display: none;">
    <ul>
        <li><a href="javascript:void(0);" onclick="loadContent('/api/expenses/categories')">Categories</a></li>
        <li><a href="javascript:void(0);" onclick="loadContent('/api/expenses/add')">Add Expenses</a></li>
        <li class="submenu">
                <a href="javascript:void(0);">Reports</a>
                <ul class="dropdown">
                    <li><a href="javascript:void(0);" onclick="loadContent('/api/expenses/manageExpenses')">Manage Expenses</a></li>
                    <li><a href="javascript:void(0);" onclick="loadContent('/api/expenses/reports?filter=month')">By Month</a></li>
                    <li><a href="javascript:void(0);" onclick="loadContent('/api/expenses/reports?filter=user')">By User</a></li>
                </ul>
        </li>
    </ul>
</div>
<div class="sidebar" id="rentals" style="display: none;">
    <ul>
        <li><a href="javascript:void(0);" onclick="loadContent('/api/rent/owners/add')">Add Owner</a></li>
        <li><a href="javascript:void(0);" onclick="loadContent('/api/rent/payments/add')">Add Rent </a></li>
        <li><a href="javascript:void(0);" onclick="loadContent('/api/rent/reports')">Reports</a></li>
    </ul>
</div>
<div class="sidebar" id="investments" style="display: none;">
    <ul>
        <li><a href="/api/investments/add">Add Investment</a></li>
        <li><a href="/api/investments/report">Investment Report</a></li>
    </ul>
</div>
<div class="sidebar" id="loans" style="display: none;">
    <ul>
        <li><a href="/api/loans/apply">Apply for Loan</a></li>
        <li><a href="/api/loans/track">Track Loan</a></li>
    </ul>
</div>
<div id="page-content">
</div>
</div>
</c:if>