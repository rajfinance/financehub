function getCsrfHeaders() {
    const h = {};
    if (typeof window.__csrfHeaderName !== 'undefined' && window.__csrfToken) {
        h[window.__csrfHeaderName] = window.__csrfToken;
    }
    return h;
}

function appendCsrfToFormData(formData) {
    if (typeof window.__csrfParameterName !== 'undefined' && window.__csrfToken) {
        if (!formData.get(window.__csrfParameterName)) {
            formData.append(window.__csrfParameterName, window.__csrfToken);
        }
    }
}

function appendCsrfToUrlSearchParams(params) {
    if (typeof window.__csrfParameterName !== 'undefined' && window.__csrfToken) {
        if (!params.has(window.__csrfParameterName)) {
            params.append(window.__csrfParameterName, window.__csrfToken);
        }
    }
}

function formHasFileInput(form) {
    return form.querySelector('input[type="file"]') !== null;
}

function loadContent(apiUrl) {
    const pageContent = document.getElementById('page-content');
    const dashboardContent = document.getElementById('dashboardContent');
    const mainContent = document.getElementById('mainContent');
    if (!pageContent) {
        if (typeof apiUrl === 'string' && apiUrl.startsWith('/')) {
            try {
                sessionStorage.setItem('fhPendingLoad', apiUrl);
            } catch (e) {
                /* storage unavailable */
            }
            window.location.href = '/api/home';
            return;
        }
        console.error('loadContent: #page-content missing and URL not usable for redirect:', apiUrl);
        return;
    }
    if (dashboardContent) {
        dashboardContent.style.display = 'none';
    }
    if (mainContent) {
        mainContent.style.display = 'block';
    }
    fetch(apiUrl, {
        credentials: 'same-origin',
        headers: getCsrfHeaders()
    })
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
    if (!validForm(formId)) {
        return;
    }
    const headers = getCsrfHeaders();
    let body;
    if (formHasFileInput(form)) {
        const formData = new FormData(form);
        appendCsrfToFormData(formData);
        body = formData;
    } else {
        const params = new URLSearchParams(new FormData(form));
        appendCsrfToUrlSearchParams(params);
        body = params;
        headers['Content-Type'] = 'application/x-www-form-urlencoded;charset=UTF-8';
    }
    fetch(form.action, {
        method: 'POST',
        body: body,
        headers: headers,
        credentials: 'same-origin'
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
    fetch(url, {
        credentials: 'same-origin',
        headers: getCsrfHeaders()
    })
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
            method: 'DELETE',
            headers: getCsrfHeaders(),
            credentials: 'same-origin'
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
function callEditCategory(element) {
        const id = element.getAttribute('data-id') || '';
        const name = element.getAttribute('data-name') || '';
        const icon = element.getAttribute('data-icon') || '';
        const sortOrder = element.getAttribute('data-sort-order') || '';
        const enabled = element.getAttribute('data-enabled') === 'true';

        editCategory(id, name, icon, sortOrder, enabled);
}
let profilePhotoPreviewObjectUrl = null;

function previewProfilePhoto(fileInput) {
    const preview = document.getElementById('profilePhotoPreview');
    if (!preview) {
        return;
    }
    if (profilePhotoPreviewObjectUrl) {
        URL.revokeObjectURL(profilePhotoPreviewObjectUrl);
        profilePhotoPreviewObjectUrl = null;
    }
    if (!fileInput || !fileInput.files || !fileInput.files[0]) {
        const defaultSrc = preview.getAttribute('data-default-src');
        if (defaultSrc) {
            preview.src = defaultSrc;
        }
        return;
    }
    const file = fileInput.files[0];
    const allowed = ['image/jpeg', 'image/jpg', 'image/png'];
    const name = (file.name || '').toLowerCase();
    const typeOk = file.type && allowed.includes(file.type.toLowerCase());
    const extOk = name.endsWith('.jpg') || name.endsWith('.jpeg') || name.endsWith('.png');
    if (!typeOk && !extOk) {
        alert('Please choose a JPG, JPEG, or PNG image.');
        fileInput.value = '';
        return;
    }
    if (file.size > 512 * 1024) {
        alert('Image must be 512 KB or smaller.');
        fileInput.value = '';
        return;
    }
    profilePhotoPreviewObjectUrl = URL.createObjectURL(file);
    preview.src = profilePhotoPreviewObjectUrl;
}

function previewCategoryIcon(fileInput) {
    const preview = document.getElementById("categoryIconPreview");
    if (!preview || !fileInput || !fileInput.files || !fileInput.files[0]) {
        return;
    }
    const file = fileInput.files[0];
    if (file.size > 250 * 1024) {
        alert("Image must be 250 KB or smaller.");
        fileInput.value = "";
        return;
    }
    const url = URL.createObjectURL(file);
    preview.onload = function () {
        URL.revokeObjectURL(url);
        preview.onload = null;
    };
    preview.src = url;
}

function editCategory(id, name, icon, sortOrder, enabled) {
    document.getElementById("categoryId").value = id;
    document.getElementById("categoryName").value = name;
    const hiddenIcon = document.getElementById("iconPath");
    if (hiddenIcon) {
        hiddenIcon.value = icon || "";
    }
    document.getElementById("sortOrder").value = sortOrder;
    document.getElementById("enabled").checked = enabled;

    const fileInput = document.getElementById("iconImage");
    if (fileInput) {
        fileInput.value = "";
    }
    const preview = document.getElementById("categoryIconPreview");
    if (preview) {
        const src = icon && icon.trim() ? icon : "/images/category-placeholder.svg";
        preview.src = src;
    }

    window.scrollTo({ top: 0, behavior: "smooth" });
}
function deleteCategoryEntity(element, entityName, apiUrl) {
    let categoryId = element.getAttribute("data-id");
    if (!confirm(`Are you sure you want to delete this ${entityName}?`)) {
        return;
    }
    const params = new URLSearchParams();
    params.append("id", categoryId);
    if (typeof window.__csrfParameterName !== 'undefined' && window.__csrfToken) {
        params.append(window.__csrfParameterName, window.__csrfToken);
    }
    fetch(apiUrl + "categoryDelete", {
        method: "POST",
        headers: Object.assign({ "Content-Type": "application/x-www-form-urlencoded" }, getCsrfHeaders()),
        body: params.toString(),
        credentials: 'same-origin'
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
        method: 'GET',
        credentials: 'same-origin',
        headers: getCsrfHeaders()
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

function toggleLoanDetails(loanId) {
    const detailRow = document.getElementById(`loan-detail-${loanId}`);
    if (!detailRow) {
        return;
    }
    detailRow.style.display = detailRow.style.display === 'none' || detailRow.style.display === '' ? 'table-row' : 'none';
}

function prefillRecordEmiDate() {
    const loanSelect = document.getElementById('loanIdDisplay') || document.getElementById('loanId');
    const emiNumberInput = document.getElementById('emiNumber');
    const paidOnInput = document.getElementById('paidOn');
    if (!loanSelect || !emiNumberInput || !paidOnInput || paidOnInput.value) {
        return;
    }
    const selectedOption = loanSelect.options[loanSelect.selectedIndex];
    if (!selectedOption) {
        return;
    }
    const firstEmi = selectedOption.getAttribute('data-first-emi');
    const emiNumber = parseInt(emiNumberInput.value, 10);
    if (!firstEmi || Number.isNaN(emiNumber) || emiNumber < 1) {
        return;
    }
    const baseDate = new Date(`${firstEmi}T00:00:00`);
    if (Number.isNaN(baseDate.getTime())) {
        return;
    }
    baseDate.setMonth(baseDate.getMonth() + (emiNumber - 1));
    const y = baseDate.getFullYear();
    const m = String(baseDate.getMonth() + 1).padStart(2, '0');
    const d = String(baseDate.getDate()).padStart(2, '0');
    paidOnInput.value = `${y}-${m}-${d}`;
}

function toggleRecordEmiPreClosure(checkbox) {
    const checked = checkbox && checkbox.checked;
    const section = document.getElementById('preClosureSection');
    const closureType = document.getElementById('preClosureType');
    const preClosureDate = document.getElementById('preClosureDate');
    const preClosureAmount = document.getElementById('preClosureAmount');
    const referenceNumber = document.getElementById('preClosureReferenceNumber');
    if (section) {
        section.style.display = checked ? 'block' : 'none';
    }
    if (preClosureDate) preClosureDate.required = checked;
    if (preClosureAmount) preClosureAmount.required = checked;
    if (referenceNumber) referenceNumber.required = checked;
    if (checked) {
        const paidOnInput = document.getElementById('paidOn');
        if (preClosureDate && !preClosureDate.value && paidOnInput && paidOnInput.value) {
            preClosureDate.value = paidOnInput.value;
        }
    }
    toggleRecordEmiClosureType(closureType);
}

function toggleRecordEmiClosureType(selectEl) {
    const section = document.getElementById('partialClosureSection');
    const preClosureSelected = document.getElementById('preClosureSelected');
    const updatedEmi = document.getElementById('partialUpdatedEmiAmount') || document.getElementById('updatedEmiAmount');
    const updatedTenure = document.getElementById('partialUpdatedTenure') || document.getElementById('updatedTenure');
    const preClosureEnabled = preClosureSelected ? preClosureSelected.checked : true;
    const partial = preClosureEnabled && selectEl && selectEl.value === 'PARTIAL';
    if (section) {
        section.style.display = partial ? 'block' : 'none';
    }
    if (updatedEmi) updatedEmi.required = partial;
    if (updatedTenure) updatedTenure.required = partial;
}

function confirmAndSubmitRecordEmi(event) {
    const preClosureSelected = document.getElementById('preClosureSelected');
    if (preClosureSelected && preClosureSelected.checked) {
        const proceed = window.confirm("Remaining EMIs will be closed/adjusted after pre-closure. Continue?");
        if (!proceed) {
            event.preventDefault();
            return false;
        }
    }
    submitForm(event);
    return false;
}

function persistCategoryOrder(tbody) {
    const ids = [...tbody.querySelectorAll("tr[data-category-id]")]
        .map((r) => parseInt(r.getAttribute("data-category-id"), 10))
        .filter((n) => !Number.isNaN(n));
    if (!ids.length) {
        return;
    }
    const headers = Object.assign(
        { "Content-Type": "application/json" },
        typeof getCsrfHeaders === "function" ? getCsrfHeaders() : {}
    );
    fetch("/api/expenses/categoryReorder", {
        method: "POST",
        headers: headers,
        credentials: "same-origin",
        body: JSON.stringify({ orderedIds: ids }),
    })
        .then((r) => {
            if (!r.ok) {
                throw new Error("Reorder failed");
            }
            if (typeof loadContent === "function") {
                loadContent("/api/expenses/categories");
            }
        })
        .catch((err) => console.error(err));
}

(function initExpenseCategoryDrag() {
    let draggedRow = null;
    let orderBeforeDrag = "";

    document.addEventListener("dragstart", function (e) {
        const grip = e.target.closest("#categoryTable .category-drag-grip");
        if (!grip) {
            return;
        }
        const row = grip.closest("tr[data-category-id]");
        const categoryTbody = document.getElementById("categoryTable");
        if (!row || !categoryTbody || !categoryTbody.contains(row)) {
            return;
        }
        draggedRow = row;
        row.classList.add("fh-category-dragging");
        const tbody = row.closest("tbody");
        orderBeforeDrag = [...tbody.querySelectorAll("tr[data-category-id]")]
            .map((r) => r.getAttribute("data-category-id"))
            .join(",");
        if (e.dataTransfer) {
            e.dataTransfer.effectAllowed = "move";
            e.dataTransfer.setData("text/plain", row.getAttribute("data-category-id") || "");
        }
    });

    function onDragOverCategory(e) {
        if (!draggedRow) {
            return;
        }
        const tbody = draggedRow.closest("tbody");
        if (!tbody || !tbody.contains(e.target)) {
            return;
        }
        e.preventDefault();
        if (e.dataTransfer) {
            e.dataTransfer.dropEffect = "move";
        }
        const row = e.target.closest("tr[data-category-id]");
        if (!row || row === draggedRow) {
            return;
        }
        const rect = row.getBoundingClientRect();
        const after = e.clientY > rect.top + rect.height / 2;
        if (after) {
            tbody.insertBefore(draggedRow, row.nextSibling);
        } else {
            tbody.insertBefore(draggedRow, row);
        }
    }

    document.addEventListener("dragover", onDragOverCategory, true);

    document.addEventListener("drop", function (e) {
        if (draggedRow && e.target.closest("#categoryTable")) {
            e.preventDefault();
        }
    });

    document.addEventListener("dragend", function () {
        if (!draggedRow) {
            return;
        }
        draggedRow.classList.remove("fh-category-dragging");
        const tbody = draggedRow.closest("tbody");
        const snapshot = tbody;
        draggedRow = null;
        if (!snapshot || !orderBeforeDrag) {
            orderBeforeDrag = "";
            return;
        }
        const newOrder = [...snapshot.querySelectorAll("tr[data-category-id]")]
            .map((r) => r.getAttribute("data-category-id"))
            .join(",");
        if (newOrder !== orderBeforeDrag) {
            persistCategoryOrder(snapshot);
        }
        orderBeforeDrag = "";
    });
})();