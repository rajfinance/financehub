<%@ page session="true" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<head>
    <title>Manage Categories</title>
</head>
<body>
<div class="form-container">
<h2>Manage Expense Categories</h2>

<form id="categoryForm">
    <table style="width:100%;">
        <tr>
            <td><label for="categoryName">Name:</label></td>
            <td><input type="text" name="name" id="categoryName" placeholder="Enter category name" style="width: 200px;" required></td>
            <td><label for="iconPath">Icon Path:</label></td>
            <td><input type="text" name="icon" id="iconPath" placeholder="Enter icon path (e.g., images/icon.png)"></td>
        </tr>
        <tr>
            <td><label for="sortOrder">Sort Order:</label></td>
            <td><input type="number" name="sortOrder" id="sortOrder" placeholder="Enter sort order" required style="width: 200px;"></td>
            <td><label for="enabled">Enabled:</label></td>
            <td><input type="checkbox" name="enabled" id="enabled" checked style="text-align:left:"></td>
        </tr>
    </table>

    <div style="text-align: center; margin-top: 20px;">
        <button type="submit">Save</button>
    </div>
</form>


<br><br>

<div id="reportContent">
<c:if test="${not empty categories}">
<table width="100%">
    <thead>
        <tr>
            <th>Name</th>
            <th>Icon</th>
            <th>Sort Order</th>
            <th>Enabled</th>
            <th>Actions</th>
        </tr>
    </thead>
    <tbody id="categoryTable">
        <c:forEach var="category" items="${categories}">
            <tr id="row-${category.id}">
                <td>${category.name}</td>
                <td><img src="${pageContext.request.contextPath}/${category.icon}" width="30" alt="Icon"></td>
                <td>${category.sortOrder}</td>
                <td>${category.enabled ? 'Yes' : 'No'}</td>
                <td>
                <a href="javascript:void(0);" onclick="loadEditPageContent('/api/rent/add?id=${payment.id}', '#page-content')" style="text-decoration: none;">
                    <img src="${pageContext.request.contextPath}/images/edit-icon.png" alt="Edit" style="width: 20px; height: 20px; margin-right: 20px;margin-left: 15px;">
                </a>
                <a href="javascript:void(0);" class="delete-btn" data-report-type="rentPaymentReport" data-id=${payment.id} onclick="deleteEntity(this,'Payment','/api/rent/\')">
                    <img src="${pageContext.request.contextPath}/images/delete-icon.png" alt="Delete" style="width: 20px; height: 20px;">
                </a>
                </td>
            </tr>
        </c:forEach>
    </tbody>
</table>
</c:if>
<c:if test="${empty categories}">
    <h2 style="text-align: center; color: #333;padding-bottom: 10px;">No Expenses Categories Available.</h2>
</c:if>
</div>

<script>
document.getElementById("categoryForm").addEventListener("submit", function(event) {
    event.preventDefault();

    let formData = new FormData(this);
    fetch("categories/add", {
        method: "POST",
        body: formData
    })
    .then(response => response.json())
    .then(category => {
        let newRow = `
            <tr id="row-${category.id}">
                <td><img src="${category.icon}" width="30" alt="Icon"></td>
                <td>${category.name}</td>
                <td>${category.sortOrder}</td>
                <td>${category.enabled ? 'Yes' : 'No'}</td>
                <td>
                    <button onclick="deleteCategory(${category.id})">Delete</button>
                </td>
            </tr>`;
        document.getElementById("categoryTable").insertAdjacentHTML("beforeend", newRow);
    });
});

function deleteCategory(id) {
    fetch("categories/delete?id=" + id, { method: "POST" })
    .then(() => document.getElementById("row-" + id).remove());
}
</script>
</div>
