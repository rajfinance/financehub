function loadContent(apiUrl) {
    const pageContent = document.getElementById('page-content');
    fetch(apiUrl)
        .then(response => response.text())
        .then(html => {
            pageContent.innerHTML = html;
        })
        .catch(error => {
            console.error('Error loading content:', error);
            pageContent.innerHTML = "<p>There was an error loading the page.</p>";
        });
}
function submitForm(event) {
        event.preventDefault();
        const form = event.currentTarget;
        const formId = form.id;
        console.log(formId);
        if(!validForm(formId)){
            return;
        }
        const formData = new FormData(form);
        fetch(form.action, {
            method: 'POST',
            body: formData
        })
        .then(response => response.text())
        .then(html => {
            document.getElementById('page-content').innerHTML = html;
        })
        .catch(error => {
            console.error('Error submitting form:', error);
            document.getElementById('page-content').innerHTML = "<p>There was an error submitting the form. Please try again.</p>";
        });
}
function loadEditPageContent(url) {
    fetch(url)
        .then(response => {
            if (!response.ok) {
                throw new Error('Failed to fetch content');
            }
            return response.text();
        })
        .then(html => {
            document.getElementById('page-content').innerHTML = html;
        })
        .catch(error => {
            console.error('Error submitting form:', error);
            document.getElementById('page-content').innerHTML = "<p>There was an error submitting the form. Please try again.</p>";
        });
}
function deleteEntity(anchor, entityType, apiEndpoint) {
    var entityId = anchor.getAttribute('data-id');
    var reportType = anchor.getAttribute('data-report-type');
    if (confirm(`Are you sure you want to delete this ${entityType}?`)) {
        let apiUrl = `${apiEndpoint}?id=${entityId}`;
       if (entityType.toLowerCase().includes("plan")) {
            apiUrl += `&type=plan`;
       } else if (entityType.toLowerCase().includes("actual")) {
            apiUrl += `&type=actual`;
       }
        fetch(apiUrl, {
            method: 'DELETE'
        })
        .then(response => {
            return response.text().then(data => {
                if (response.status === 409) {
                    alert(data);
                } else if (response.ok) {
                    if (data === "success") {
                        alert(`${entityType.charAt(0).toUpperCase() + entityType.slice(1)} deleted successfully`);
                        fetchReportContent(reportType);
                    } else {
                        alert(`Error deleting ${entityType}`);
                    }
                } else {
                    alert(`Error: ${data}`);
                }
            });
        })
        .catch(error => {
            alert('Error: ' + error.message);
        });
    }
}
function editCategory(id, name, icon, sortOrder, enabled) {
    document.getElementById("categoryId").value = id;
    document.getElementById("categoryName").value = name;
    document.getElementById("iconPath").value = icon;
    document.getElementById("sortOrder").value = sortOrder;
    document.getElementById("enabled").checked = enabled;

    window.scrollTo({ top: 0, behavior: "smooth" });
}
function deleteCategoryEntity(element, entityName, apiUrl) {
    let categoryId = element.getAttribute("data-id");
    if (!confirm(`Are you sure you want to delete this ${entityName}?`)) {
        return;
    }
    fetch(apiUrl + "categoryDelete?id=" + categoryId, {
        method: "POST"
    })
    .then(response => response.text())
    .then(html => {
        alert("Category Deleted Successfully!");
        document.getElementById('page-content').innerHTML = html;
    })
    .catch(error => console.error("Error deleting category:", error));
}

function calculateExpenses() {
        let total = 0;
        document.querySelectorAll(".expenses").forEach(input => {
            total += parseFloat(input.value) || 0;
        });
        document.getElementById("totalExpense").value = total;
}

function loadReport(yearId,apiUrl,containerId) {
event.preventDefault();
const year = document.getElementById(yearId).value;
    if (year === "") {
        alert("Please select a year.");
        return;
    }
    let lastValue = apiUrl.split("/").pop();

    const url = `${apiUrl}?year=${encodeURIComponent(year)}`;
    const container = document.getElementById(containerId);
    fetch(url, {
        method: 'GET'
    })
    .then(response => {
        if (!response.ok) {
            throw new Error("Network response was not ok " + response.statusText);
        }
        return response.text();
    })
    .then(html => {
        if (!container) {
            alert("Target container not found.");
            return;
        }
        container.innerHTML = html;
    })
    .catch(error => {
        console.error("Error fetching report:", error);
        alert("Error fetching report. Please try again.");
    });
    attachReportListeners(container,lastValue);
}
function setActive(button) {
    button.closest('.form-container').querySelectorAll('button').forEach(btn => btn.classList.remove('active'));
    button.classList.add('active');
}