<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<header>
<meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>RajaFinanceHub</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/finStyles.css">
    <div class="header-container">
        <div class="left-section"></div>
        <div class="middle-section">
            <div class="logo">
                <img src="${pageContext.request.contextPath}/images/financehublogo.png" alt="Logo">
                <span>RajaFinanceHub</span>
            </div>
        </div>
        <div class="right-section">
            <div class="userinfo">
                    <c:if test="${not empty sessionScope.username}">
                        <span>${sessionScope.username}</span>
                        <img src="${pageContext.request.contextPath}/images/signin.png" alt="User Image" class="user-image">
                    </c:if>
                    <c:if test="${empty sessionScope.username}">
                    <div class="signin">
                        <a href="/login" class="signin-link">
                            <span>SignIn</span>
                            <img src="${pageContext.request.contextPath}/images/signin.png" alt="Login" class="login-image">
                        </a>
                    </div>
                    </c:if>
            </div>
        </div>
    </div>
</header>
<jsp:include page="/WEB-INF/views/common/menu.jsp" />